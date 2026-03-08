package com.dependencyhealth.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphJsonExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Export the graph to a hierarchical format suitable for D3 trees/force graphs:
     * {
     *   "name": "id",
     *   "risk": "SAFE",
     *   ...
     *   "children": [ ... ]
     * }
     * Note: Pure trees don't allow multiple parents, but dependency graphs are DAGs.
     * To support D3 force graphs efficiently handling DAGs, we export nodes and links arrays:
     * {
     *   "nodes": [ { "id": "...", "name": "...", "risk": "..." } ],
     *   "links": [ { "source": "id1", "target": "id2", "type": "direct/transitive/blast_radius" } ]
     * }
     */
    public String exportToJson(DependencyGraphModel graph) {
        try {
            ObjectNode root = mapper.createObjectNode();

            ArrayNode nodesArray = mapper.createArrayNode();
            for (GraphNode n : graph.getNodes()) {
                ObjectNode nodeObj = mapper.createObjectNode();
                nodeObj.put("id", n.getId());
                nodeObj.put("name", n.getName());
                nodeObj.put("version", n.getVersion());
                nodeObj.put("risk", n.getRiskLevel());
                nodeObj.put("isDirect", n.isDirectDependency());
                
                ObjectNode metaNode = mapper.createObjectNode();
                for (Map.Entry<String, Object> entry : n.getMetadata().entrySet()) {
                    metaNode.putPOJO(entry.getKey(), entry.getValue());
                }
                nodeObj.set("metadata", metaNode);
                
                nodesArray.add(nodeObj);
            }
            root.set("nodes", nodesArray);

            ArrayNode linksArray = mapper.createArrayNode();
            for (GraphEdge e : graph.getEdges()) {
                ObjectNode linkObj = mapper.createObjectNode();
                linkObj.put("source", e.getSource());
                linkObj.put("target", e.getTarget());
                linkObj.put("type", e.getRelationshipType());
                linksArray.add(linkObj);
            }
            root.set("links", linksArray);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export graph to JSON", e);
        }
    }
}
