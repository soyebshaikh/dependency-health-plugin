package com.dependencyhealth.visualization;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyGraphModel {
    private String rootNodeId;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Set<GraphEdge> edges = new HashSet<>();

    public String getRootNodeId() {
        return rootNodeId;
    }

    public void setRootNodeId(String rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public Collection<GraphNode> getNodes() {
        return nodes.values();
    }

    public Set<GraphEdge> getEdges() {
        return edges;
    }
}
