package com.dependencyhealth.visualization;

import java.util.Objects;

public class GraphEdge {
    private String source; // node ID
    private String target; // node ID
    private String relationshipType; // e.g., "transitive", "direct", "blast_radius"

    public GraphEdge(String source, String target, String relationshipType) {
        this.source = source;
        this.target = target;
        this.relationshipType = relationshipType;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(source, graphEdge.source) &&
                Objects.equals(target, graphEdge.target) &&
                Objects.equals(relationshipType, graphEdge.relationshipType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, relationshipType);
    }
}
