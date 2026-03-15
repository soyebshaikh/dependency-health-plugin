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
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.artifact.Artifact;

import com.dependencyhealth.dependency.DependencyCollector;
import com.dependencyhealth.lifecycle.EolClient;
import com.dependencyhealth.lifecycle.LifecycleClient;
import com.dependencyhealth.lifecycle.LifecycleData;
import com.dependencyhealth.lifecycle.MavenSearchClient;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enterprise-Grade Maven Dependency Intelligence Plugin - Aggregate Goal
 *
 * Runs once across all modules in a multi-module project to generate a
 * singular, aggregated dependency health report.
 */
@Mojo(name = "aggregate", defaultPhase = LifecyclePhase.VERIFY, aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class DependencyHealthAggregateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

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

    @Parameter(property = "enableGraphVisualization", defaultValue = "true")
    private boolean enableGraphVisualization;

    @Parameter(property = "skipPolicy", defaultValue = "true")
    private boolean skipPolicy;

    @Parameter(property = "nvdApiKey")
    private String nvdApiKey;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipScan) {
            getLog().info("Dependency Health Aggregate Scan skipped.");
            return;
        }

        getLog().info("------------------------------------------------------------------------");
        getLog().info("Starting Aggregated Dependency Health Scan...");
        getLog().info("------------------------------------------------------------------------");

        if (reactorProjects == null || reactorProjects.isEmpty()) {
            getLog().info("No reactor projects found.");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. Collect Dependencies across all modules
            getLog().info("Step 1: Collecting dependencies from all reactor projects...");
            Set<Artifact> allDependencies = new HashSet<>();
            List<DependencyNode> rootNodes = new ArrayList<>();

            MavenProject rootProject = reactorProjects.get(0);

            for (MavenProject proj : reactorProjects) {
                DependencyCollector collector = new DependencyCollector(proj, session, dependencyGraphBuilder);
                allDependencies.addAll(collector.getAllDependencies());
                try {
                    rootNodes.add(collector.buildDependencyGraph());
                } catch (Exception e) {
                    getLog().warn("Could not build dependency graph for project: " + proj.getName());
                }
            }

            getLog().info("Found " + allDependencies.size() + " unique dependencies across " + reactorProjects.size()
                    + " modules.");

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // 2. Generate SBOM
            getLog().info("Step 2: Generating Aggregated SBOM...");
            SbomGenerator sbomGenerator = new SbomGenerator();
            File sbomFile = new File(outputDirectory, "dependency-sbom.json");
            sbomGenerator.generateSbom(
                    rootProject.getGroupId(),
                    rootProject.getArtifactId() + "-aggregate",
                    rootProject.getVersion(),
                    allDependencies,
                    sbomFile);
            getLog().info("Aggregated SBOM saved to: " + sbomFile.getAbsolutePath());

            // 3. Vulnerability Intelligence
            getLog().info("Step 3: Querying vulnerability intelligence APIs...");
            getLog().info(" - API: Sonatype OSS Index (https://ossindex.sonatype.org/api/v3/component-report)");
            getLog().info(" - API: NVD (https://services.nvd.nist.gov/rest/json/cves/2.0)");
            List<VulnerabilityClient> vulnClients = new ArrayList<>();
            vulnClients.add(new OssIndexClient());
            vulnClients.add(new NvdClient(nvdApiKey));

            java.util.HashMap<String, List<Vulnerability>> aggregatedVulns = new java.util.HashMap<>();
            for (VulnerabilityClient client : vulnClients) {
                getLog().info(" - Querying " + client.getProviderName() + "...");
                Map<String, List<Vulnerability>> clientResults = client.checkVulnerabilities(allDependencies);
                clientResults.forEach((purl, vulns) -> {
                    aggregatedVulns.computeIfAbsent(purl, k -> new ArrayList<>()).addAll(vulns);
                });
            }

            // 4. End-of-Life Detection
            getLog().info("Step 4: Querying lifecycle intelligence APIs...");
            getLog().info(" - API: endoflife.date (https://endoflife.date/api)");
            LifecycleClient eolClient = new EolClient(getLog());
            Map<String, LifecycleData> lifecycleDataMap = eolClient.checkLifecycle(allDependencies);
            
            // 4.5 Latest Version Check
            getLog().info("Step 4.5: Resolving latest versions from Maven Central...");
            MavenSearchClient searchClient = new MavenSearchClient(getLog());
            Map<String, String> latestVersionsMap = searchClient.getLatestVersions(allDependencies);

            // 5. Risk Aggregation
            getLog().info("Step 5: Aggregating risk intelligence...");
            RiskAnalysisService riskService = new RiskAnalysisService();
            Map<String, DependencyRiskProfile> riskProfiles = riskService.analyzeRisk(allDependencies, aggregatedVulns,
                    lifecycleDataMap, latestVersionsMap);

            // 6. Report Generation
            getLog().info("Step 6: Generating aggregated reports...");
            ConsoleReporter consoleReporter = new ConsoleReporter(getLog());
            consoleReporter.generateReport(riskProfiles);

            HtmlReportGenerator htmlReporter = new HtmlReportGenerator();
            File htmlReportFile = new File(outputDirectory, "dependency-health-report.html");
            htmlReporter.generateReport(riskProfiles, htmlReportFile);
            getLog().info("Aggregated HTML Report saved to: " + htmlReportFile.getAbsolutePath());

            JsonReportGenerator jsonReporter = new JsonReportGenerator();
            File jsonReportFile = new File(outputDirectory, "dependency-health-report.json");
            jsonReporter.generateReport(riskProfiles, jsonReportFile);
            getLog().info("Aggregated JSON Report saved to: " + jsonReportFile.getAbsolutePath());

            // 6.5 Optional Visualization
            if (enableGraphVisualization) {
                getLog().info("Step 6.5: Generating Interactive Dependency Graph Visualization...");
                try {
                    GraphModelBuilder graphBuilder = new GraphModelBuilder();
                    DependencyGraphModel graphModel = graphBuilder.buildAggregateGraph(rootNodes, riskProfiles);

                    BlastRadiusAnalyzer blastRadiusAnalyzer = new BlastRadiusAnalyzer();
                    blastRadiusAnalyzer.analyzeAndTagBlastRadius(graphModel);

                    GraphJsonExporter jsonExporter = new GraphJsonExporter();
                    String graphJson = jsonExporter.exportToJson(graphModel);

                    HtmlGraphRenderer htmlRenderer = new HtmlGraphRenderer();
                    File graphHtmlFile = new File(outputDirectory, "dependency-graph.html");
                    htmlRenderer.renderHtml(graphJson, graphHtmlFile);

                    getLog().info(
                            "Aggregated Dependency Graph Visualization saved to: " + graphHtmlFile.getAbsolutePath());
                } catch (Exception e) {
                    getLog().warn("Failed to generate aggregated dependency graph visualization: " + e.getMessage());
                }
            } else {
                getLog().debug("Graph Visualization is disabled. Skipping.");
            }

            // 7. Policy Enforcement
            if (!skipPolicy) {
                getLog().info("Step 7: Enforcing policies...");
                PolicyEngine policyEngine = new PolicyEngine(failOnCritical, failOnHighCount, failOnEol);
                policyEngine.evaluate(riskProfiles);
            } else {
                getLog().info("Step 7: Policy enforcement skipped (skipPolicy=true).");
            }

            long duration = System.currentTimeMillis() - startTime;
            getLog().info("------------------------------------------------------------------------");
            getLog().info("Dependency Health Aggregate Scan completed in " + duration + " ms.");
            getLog().info("------------------------------------------------------------------------");

        } catch (Exception e) {
            getLog().error("Failed to execute Aggregated Dependency Health Scan", e);
            throw new MojoExecutionException("Error during aggregated dependency scanning: " + e.getMessage(), e);
        }
    }
}
