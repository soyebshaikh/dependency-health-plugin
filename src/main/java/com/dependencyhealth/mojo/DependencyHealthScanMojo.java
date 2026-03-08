package com.dependencyhealth.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.artifact.Artifact;

import com.dependencyhealth.dependency.DependencyCollector;
import com.dependencyhealth.lifecycle.EolClient;
import com.dependencyhealth.lifecycle.LifecycleClient;
import com.dependencyhealth.lifecycle.LifecycleData;
import com.dependencyhealth.policy.PolicyEngine;
import com.dependencyhealth.report.ConsoleReporter;
import com.dependencyhealth.report.HtmlReportGenerator;
import com.dependencyhealth.report.JsonReportGenerator;
import com.dependencyhealth.risk.DependencyRiskProfile;
import com.dependencyhealth.risk.RiskAnalysisService;
import com.dependencyhealth.sbom.SbomGenerator;
import com.dependencyhealth.vulnerability.NvdClient;
import com.dependencyhealth.vulnerability.OssIndexClient;
import com.dependencyhealth.vulnerability.Vulnerability;
import com.dependencyhealth.vulnerability.VulnerabilityClient;
import com.dependencyhealth.visualization.BlastRadiusAnalyzer;
import com.dependencyhealth.visualization.DependencyGraphModel;
import com.dependencyhealth.visualization.GraphJsonExporter;
import com.dependencyhealth.visualization.GraphModelBuilder;
import com.dependencyhealth.visualization.HtmlGraphRenderer;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enterprise-Grade Maven Dependency Intelligence Plugin
 */
@Mojo(name = "scan-dependencies", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class DependencyHealthScanMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(property = "failOnCritical", defaultValue = "true")
    private boolean failOnCritical;

    @Parameter(property = "failOnHighCount", defaultValue = "5")
    private int failOnHighCount;

    @Parameter(property = "failOnEol", defaultValue = "true")
    private boolean failOnEol;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(property = "skipScan", defaultValue = "false")
    private boolean skipScan;

    @Parameter(property = "enableGraphVisualization", defaultValue = "false")
    private boolean enableGraphVisualization;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipScan) {
            getLog().info("Dependency Health Scan skipped.");
            return;
        }

        getLog().info("------------------------------------------------------------------------");
        getLog().info("Starting Dependency Health Scan...");
        getLog().info("------------------------------------------------------------------------");
        getLog().info("Project: " + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Collect Dependencies
            getLog().info("Step 1: Collecting dependencies...");
            DependencyCollector collector = new DependencyCollector(project, session, dependencyGraphBuilder);
            Set<Artifact> dependencies = collector.getAllDependencies();
            DependencyNode rootNode = collector.buildDependencyGraph();
            getLog().info("Found " + dependencies.size() + " dependencies.");

            // 2. Generate SBOM
            getLog().info("Step 2: Generating SBOM...");
            SbomGenerator sbomGenerator = new SbomGenerator();
            File sbomFile = new File(outputDirectory, "dependency-sbom.json");
            sbomGenerator.generateSbom(
                    project.getGroupId(),
                    project.getArtifactId(),
                    project.getVersion(),
                    dependencies,
                    sbomFile
            );
            getLog().info("SBOM saved to: " + sbomFile.getAbsolutePath());

            // 3. Vulnerability Intelligence
            getLog().info("Step 3: Querying vulnerability intelligence APIs...");
            List<VulnerabilityClient> vulnClients = new ArrayList<>();
            vulnClients.add(new OssIndexClient());
            vulnClients.add(new NvdClient());

            java.util.HashMap<String, List<Vulnerability>> aggregatedVulns = new java.util.HashMap<>();
            for (VulnerabilityClient client : vulnClients) {
                getLog().info(" - Querying " + client.getProviderName() + "...");
                Map<String, List<Vulnerability>> clientResults = client.checkVulnerabilities(dependencies);
                // Merge results
                clientResults.forEach((purl, vulns) -> {
                    aggregatedVulns.computeIfAbsent(purl, k -> new ArrayList<>()).addAll(vulns);
                });
            }

            // 4. End-of-Life Detection
            getLog().info("Step 4: Querying lifecycle intelligence APIs...");
            LifecycleClient eolClient = new EolClient();
            Map<String, LifecycleData> lifecycleDataMap = eolClient.checkLifecycle(dependencies);

            // 5. Risk Aggregation
            getLog().info("Step 5: Aggregating risk intelligence...");
            RiskAnalysisService riskService = new RiskAnalysisService();
            Map<String, DependencyRiskProfile> riskProfiles = riskService.analyzeRisk(dependencies, aggregatedVulns, lifecycleDataMap);

            // 6. Report Generation
            getLog().info("Step 6: Generating reports...");
            ConsoleReporter consoleReporter = new ConsoleReporter(getLog());
            consoleReporter.generateReport(riskProfiles);

            HtmlReportGenerator htmlReporter = new HtmlReportGenerator();
            File htmlReportFile = new File(outputDirectory, "dependency-health-report.html");
            htmlReporter.generateReport(riskProfiles, htmlReportFile);
            getLog().info("HTML Report saved to: " + htmlReportFile.getAbsolutePath());

            JsonReportGenerator jsonReporter = new JsonReportGenerator();
            File jsonReportFile = new File(outputDirectory, "dependency-health-report.json");
            jsonReporter.generateReport(riskProfiles, jsonReportFile);
            getLog().info("JSON Report saved to: " + jsonReportFile.getAbsolutePath());

            // 6.5 Optional Visualization
            if (enableGraphVisualization) {
                getLog().info("Step 6.5: Generating Interactive Dependency Graph Visualization...");
                try {
                    GraphModelBuilder graphBuilder = new GraphModelBuilder();
                    DependencyGraphModel graphModel = graphBuilder.buildGraph(rootNode, riskProfiles);

                    BlastRadiusAnalyzer blastRadiusAnalyzer = new BlastRadiusAnalyzer();
                    blastRadiusAnalyzer.analyzeAndTagBlastRadius(graphModel);

                    GraphJsonExporter jsonExporter = new GraphJsonExporter();
                    String graphJson = jsonExporter.exportToJson(graphModel);

                    HtmlGraphRenderer htmlRenderer = new HtmlGraphRenderer();
                    File graphHtmlFile = new File(outputDirectory, "dependency-graph.html");
                    htmlRenderer.renderHtml(graphJson, graphHtmlFile);
                    
                    getLog().info("Dependency Graph Visualization saved to: " + graphHtmlFile.getAbsolutePath());
                } catch (Exception e) {
                    getLog().warn("Failed to generate dependency graph visualization: " + e.getMessage());
                }
            } else {
                getLog().debug("Graph Visualization is disabled. Skipping.");
            }

            // 7. Policy Enforcement
            getLog().info("Step 7: Enforcing policies...");
            PolicyEngine policyEngine = new PolicyEngine(failOnCritical, failOnHighCount, failOnEol);
            policyEngine.evaluate(riskProfiles);

            long duration = System.currentTimeMillis() - startTime;
            getLog().info("------------------------------------------------------------------------");
            getLog().info("Dependency Health Scan completed in " + duration + " ms.");
            getLog().info("------------------------------------------------------------------------");

        } catch (Exception e) {
            getLog().error("Failed to execute Dependency Health Scan", e);
            throw new MojoExecutionException("Error during dependency scanning: " + e.getMessage(), e);
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public boolean isFailOnCritical() {
        return failOnCritical;
    }

    public int getFailOnHighCount() {
        return failOnHighCount;
    }

    public boolean isFailOnEol() {
        return failOnEol;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }
}
