package com.dependencyhealth.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlastRadiusAnalyzer {

    /**
     * Tags paths from root to vulnerable nodes as 'blast_radius'.
     *
     * @param graph The DependencyGraphModel to analyze and modify in-place
     */
    public void analyzeAndTagBlastRadius(DependencyGraphModel graph) {
        // Find all high/critical risk nodes
        Set<String> vulnerableNodeIds = new HashSet<>();
        for (GraphNode node : graph.getNodes()) {
            if ("HIGH".equals(node.getRiskLevel()) || "CRITICAL".equals(node.getRiskLevel())) {
                vulnerableNodeIds.add(node.getId());
            }
        }

        // If no vulnerable nodes, blast radius is empty
        if (vulnerableNodeIds.isEmpty()) {
            return;
        }

        // Build adjacency lists for reverse traversal
        Map<String, List<GraphEdge>> incomingEdges = new HashMap<>();
        for (GraphNode node : graph.getNodes()) {
            incomingEdges.put(node.getId(), new ArrayList<>());
        }

        for (GraphEdge edge : graph.getEdges()) {
            List<GraphEdge> incoming = incomingEdges.get(edge.getTarget());
            if (incoming != null) {
                incoming.add(edge);
            }
        }

        // Tag blast radius paths using BFS backwards from vulnerable nodes
        Set<GraphEdge> blastRadiusEdges = new HashSet<>();
        Set<String> visitedNodes = new HashSet<>(vulnerableNodeIds);
        
        List<String> queue = new ArrayList<>(vulnerableNodeIds);

        while (!queue.isEmpty()) {
            String currentId = queue.remove(0);
            List<GraphEdge> incoming = incomingEdges.getOrDefault(currentId, new ArrayList<>());

            for (GraphEdge edge : incoming) {
                blastRadiusEdges.add(edge);
                edge.setRelationshipType("blast_radius");
                
                String parentId = edge.getSource();
                if (!visitedNodes.contains(parentId)) {
                    visitedNodes.add(parentId);
                    queue.add(parentId);
                }
            }
        }
    }
}
