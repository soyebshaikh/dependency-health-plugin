package com.dependencyhealth.visualization;

import com.dependencyhealth.risk.DependencyRiskProfile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.Map;

public class GraphModelBuilder {

    public DependencyGraphModel buildGraph(
            DependencyNode rootNode,
            Map<String, DependencyRiskProfile> riskProfiles) {

        DependencyGraphModel graph = new DependencyGraphModel();
        
        // Root project usually doesn't have an exact matching "purl" in riskProfiles the same way deps do,
        // but we treat it as SAFE.
        Artifact projectArtifact = rootNode.getArtifact();
        String rootId = createId(projectArtifact);
        graph.setRootNodeId(rootId);
        
        GraphNode rootGraphNode = new GraphNode(rootId, projectArtifact.getArtifactId(), projectArtifact.getVersion());
        rootGraphNode.setDirectDependency(true);
        rootGraphNode.setRiskLevel("SAFE");
        graph.addNode(rootGraphNode);

        traverseAndBuild(rootNode, rootId, graph, riskProfiles, true);

        return graph;
    }

    public DependencyGraphModel buildAggregateGraph(
            java.util.List<DependencyNode> rootNodes,
            Map<String, DependencyRiskProfile> riskProfiles) {
        
        DependencyGraphModel graph = new DependencyGraphModel();
        
        String aggregateId = "aggregate:reactor:1.0";
        graph.setRootNodeId(aggregateId);
        
        GraphNode aggregateNode = new GraphNode(aggregateId, "Aggregated Reactor", "1.0");
        aggregateNode.setDirectDependency(true);
        aggregateNode.setRiskLevel("SAFE");
        graph.addNode(aggregateNode);
        
        for (DependencyNode rootNode : rootNodes) {
            Artifact projectArtifact = rootNode.getArtifact();
            String rootId = createId(projectArtifact);
            
            GraphNode rootGraphNode = graph.getNode(rootId);
            if (rootGraphNode == null) {
                rootGraphNode = new GraphNode(rootId, projectArtifact.getArtifactId(), projectArtifact.getVersion());
                rootGraphNode.setDirectDependency(true);
                rootGraphNode.setRiskLevel("SAFE");
                graph.addNode(rootGraphNode);
            }
            
            graph.addEdge(new GraphEdge(aggregateId, rootId, "direct"));
            traverseAndBuild(rootNode, rootId, graph, riskProfiles, true);
        }
        
        return graph;
    }

    private void traverseAndBuild(DependencyNode parentNode, 
                                  String parentId, 
                                  DependencyGraphModel graph, 
                                  Map<String, DependencyRiskProfile> riskProfiles,
                                  boolean parentIsRoot) {
        
        for (DependencyNode child : parentNode.getChildren()) {
            Artifact childArtifact = child.getArtifact();
            String childId = createId(childArtifact);
            String childPurl = createPurl(childArtifact);

            // Create or get node
            GraphNode childGraphNode = graph.getNode(childId);
            if (childGraphNode == null) {
                childGraphNode = new GraphNode(childId, childArtifact.getArtifactId(), childArtifact.getVersion());
                childGraphNode.setDirectDependency(parentIsRoot); // If parent is root, this is a direct dependency
                
                // Assign Risk
                DependencyRiskProfile profile = riskProfiles.get(childPurl);
                if (profile != null) {
                    childGraphNode.setRiskLevel(normalizeRiskLevel(profile.getRiskLevelName()));
                    childGraphNode.addMetadata("riskScore", profile.getRiskScore());
                    childGraphNode.addMetadata("notes", profile.getNotes());
                    childGraphNode.addMetadata("vulnerabilitiesCount", profile.getVulnerabilities().size());
                    boolean isEol = profile.getLifecycleData() != null && profile.getLifecycleData().isEol();
                    childGraphNode.addMetadata("isEol", isEol);
                }
                
                graph.addNode(childGraphNode);
            }

            // Create Edge
            String relationship = parentIsRoot ? "direct" : "transitive";
            GraphEdge edge = new GraphEdge(parentId, childId, relationship);
            graph.addEdge(edge);

            // Recurse
            traverseAndBuild(child, childId, graph, riskProfiles, false);
        }
    }

    private String createId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private String createPurl(Artifact artifact) {
        return String.format("pkg:maven/%s/%s@%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    private String normalizeRiskLevel(String iconAndText) {
        // e.g. "ðŸ”´ Critical" -> "CRITICAL"
        if (iconAndText == null) return "SAFE";
        String txt = iconAndText.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (txt.contains("CRITICAL")) return "CRITICAL";
        if (txt.contains("HIGH")) return "HIGH";
        if (txt.contains("MEDIUM")) return "MEDIUM";
        if (txt.contains("LOW")) return "LOW";
        return "SAFE";
    }
}
