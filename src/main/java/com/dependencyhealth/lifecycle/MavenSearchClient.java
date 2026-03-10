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
    private final org.apache.maven.plugin.logging.Log log;

    public MavenSearchClient(org.apache.maven.plugin.logging.Log log) {
        this.log = log;
    }

    public Map<String, String> getLatestVersions(Set<Artifact> dependencies) {
        Map<String, String> latestVersions = new java.util.concurrent.ConcurrentHashMap<>();
        // Reduced to 5 threads to be gentler on Maven Central
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        
        int total = dependencies.size();
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        for (Artifact artifact : dependencies) {
            futures.add(executor.submit(() -> {
                String purl = String.format("pkg:maven/%s/%s@%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                try {
                    String url = String.format(SEARCH_URL, artifact.getGroupId(), artifact.getArtifactId());
                    String response = HttpClientUtil.get(url);
                    
                    if (response != null && !response.isEmpty()) {
                        JsonNode root = mapper.readTree(response);
                        JsonNode docs = root.path("response").path("docs");
                        
                        if (docs.isArray() && docs.size() > 0) {
                            String v = docs.get(0).path("v").asText();
                            if (v != null && !v.isEmpty()) {
                                latestVersions.put(purl, v);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fail silently for individual artifacts
                } finally {
                    int c = count.incrementAndGet();
                    if (log != null && (c % 5 == 0 || c == total)) {
                        log.info(" - Progress: " + c + "/" + total + " versions resolved...");
                    }
                }
            }));
        }

        for (java.util.concurrent.Future<?> future : futures) {
            try {
                future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
        executor.shutdown();
        
        return latestVersions;
    }
}
