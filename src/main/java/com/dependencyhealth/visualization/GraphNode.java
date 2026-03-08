package com.dependencyhealth.visualization;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GraphNode {
    private String id; // e.g. groupId:artifactId:version
    private String name;
    private String version;
    private String riskLevel; // SAFE, LOW, MEDIUM, HIGH, CRITICAL
    private boolean isDirectDependency;
    private Map<String, Object> metadata; // e.g. CVE count, EOL flag

    public GraphNode(String id, String name, String version) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.metadata = new HashMap<>();
        this.riskLevel = "SAFE"; // default
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public boolean isDirectDependency() { return isDirectDependency; }
    public void setDirectDependency(boolean directDependency) { isDirectDependency = directDependency; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(id, graphNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
