package com.dependencyhealth.visualization;

import com.dependencyhealth.risk.DependencyRiskProfile;
import com.dependencyhealth.vulnerability.Vulnerability;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisualizationComponentsTest {

    @Test
    public void testGraphConstructionAndExport() {
        // 1. Mock dependencies
        Artifact rootArt = new DefaultArtifact("com.test", "my-app", "1.0", "compile", "jar", "", new DefaultArtifactHandler());
        Artifact childArt1 = new DefaultArtifact("org.safe", "safe-lib", "2.0", "compile", "jar", "", new DefaultArtifactHandler());
        Artifact childArt2 = new DefaultArtifact("org.vuln", "vuln-lib", "1.5", "compile", "jar", "", new DefaultArtifactHandler());
        
        DependencyNode rootNode = mock(DependencyNode.class);
        DependencyNode childNode1 = mock(DependencyNode.class);
        DependencyNode childNode2 = mock(DependencyNode.class);
        
        when(rootNode.getArtifact()).thenReturn(rootArt);
        when(childNode1.getArtifact()).thenReturn(childArt1);
        when(childNode2.getArtifact()).thenReturn(childArt2);
        
        when(rootNode.getChildren()).thenReturn(Arrays.asList(childNode1, childNode2));
        when(childNode1.getChildren()).thenReturn(Collections.emptyList());
        when(childNode2.getChildren()).thenReturn(Collections.emptyList());

        // 2. Mock Risks
        Map<String, DependencyRiskProfile> riskProfiles = new HashMap<>();
        String vulnPurl = "pkg:maven/org.vuln/vuln-lib@1.5";
        DependencyRiskProfile vulnProfile = new DependencyRiskProfile(vulnPurl);
        vulnProfile.addVulnerability(new Vulnerability("CVE-TEST-1", "NVD", "CRITICAL", 10.0, "Test", "url"));
        riskProfiles.put(vulnPurl, vulnProfile);

        // 3. Build Graph
        GraphModelBuilder builder = new GraphModelBuilder();
        DependencyGraphModel graph = builder.buildGraph(rootNode, riskProfiles);

        // Assert Builder
        assertEquals(3, graph.getNodes().size());
        assertEquals(2, graph.getEdges().size());
        
        GraphNode vulnChild = graph.getNode("org.vuln:vuln-lib:1.5");
        assertEquals("CRITICAL", vulnChild.getRiskLevel());
        assertTrue(vulnChild.isDirectDependency());

        // 4. Test Blast Radius
        BlastRadiusAnalyzer analyzer = new BlastRadiusAnalyzer();
        analyzer.analyzeAndTagBlastRadius(graph);
        
        long blastRadiusEdges = graph.getEdges().stream().filter(e -> "blast_radius".equals(e.getRelationshipType())).count();
        assertEquals(1, blastRadiusEdges, "Should flag the edge connecting root to the vulnerable library as blast radius");

        // 5. Test JSON Export
        GraphJsonExporter exporter = new GraphJsonExporter();
        String json = exporter.exportToJson(graph);
        assertTrue(json.contains("\"nodes\""));
        assertTrue(json.contains("\"links\""));
        assertTrue(json.contains("blast_radius"));
    }
}
