package com.dependencyhealth.lifecycle;

import com.dependencyhealth.util.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.artifact.Artifact;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Queries the Maven Central Search API (search.maven.org) to find the absolute latest version
 * published for a specific groupId:artifactId coordinate.
 */
public class MavenSearchClient {

    private static final String SEARCH_URL = "https://search.maven.org/solrsearch/select?q=g:\"%s\"+AND+a:\"%s\"&core=gav&rows=1&wt=json";
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> getLatestVersions(Set<Artifact> dependencies) {
        Map<String, String> latestVersions = new HashMap<>();

        for (Artifact artifact : dependencies) {
            String purl = String.format("pkg:maven/%s/%s@%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            
            try {
                String url = String.format(SEARCH_URL, artifact.getGroupId(), artifact.getArtifactId());
                String response = HttpClientUtil.get(url);
                
                if (response != null && !response.isEmpty()) {
                    JsonNode root = mapper.readTree(response);
                    JsonNode docs = root.path("response").path("docs");
                    
                    if (docs.isArray() && docs.size() > 0) {
                        String latestVersion = docs.get(0).path("v").asText();
                        if (latestVersion != null && !latestVersion.isEmpty()) {
                            latestVersions.put(purl, latestVersion);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch latest version from Maven Central for " + artifact.getArtifactId() + ": " + e.getMessage());
            }
        }
        
        return latestVersions;
    }
}
