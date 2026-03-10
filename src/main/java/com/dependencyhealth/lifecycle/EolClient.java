package com.dependencyhealth.lifecycle;

import com.dependencyhealth.util.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.artifact.Artifact;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EolClient implements LifecycleClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final org.apache.maven.plugin.logging.Log log;

    private List<String> supportedProducts;

    public EolClient(org.apache.maven.plugin.logging.Log log) {
        this.log = log;
    }

    private void initProducts() {
        if (supportedProducts == null) {
            supportedProducts = new java.util.ArrayList<>();
            try {
                String response = HttpClientUtil.get("https://endoflife.date/api/all.json");
                if (response != null && !response.isEmpty()) {
                    JsonNode arr = mapper.readTree(response);
                    for (JsonNode n : arr) {
                        supportedProducts.add(n.asText());
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch products from endoflife.date API: " + e.getMessage());
            }
        }
    }

    private String findProductMatch(Artifact artifact) {
        initProducts();
        String artifactId = artifact.getArtifactId().toLowerCase();
        String groupId = artifact.getGroupId().toLowerCase();

        // 1. Explicit high-value mappings
        if (groupId.startsWith("org.apache.maven")) return "apache-maven";
        if (groupId.equals("org.apache.tomcat") || groupId.startsWith("org.apache.tomcat.")) return "tomcat";
        if (groupId.contains("springframework")) return "spring-framework";
        if (groupId.startsWith("org.eclipse.jetty")) return "eclipse-jetty";
        if (artifactId.equals("log4j-api") || artifactId.equals("log4j-core")) return "log4j";

        // 2. Exact matches
        if (supportedProducts.contains(artifactId)) return artifactId;
        
        String groupLast = groupId.substring(groupId.lastIndexOf('.') + 1);
        if (supportedProducts.contains(groupLast)) return groupLast;

        // 3. ArtifactId as suffix of product (e.g. maven-core matches apache-maven)
        for (String product : supportedProducts) {
            if (product.contains("-") && (artifactId.equals(product.substring(product.lastIndexOf('-') + 1)))) {
                return product;
            }
        }

        // 4. ArtifactId prefixes (e.g. log4j-core -> log4j)
        for (String product : supportedProducts) {
            if (artifactId.startsWith(product + "-")) {
                return product;
            }
        }
        
        return null;
    }

    @Override
    public Map<String, LifecycleData> checkLifecycle(Set<Artifact> dependencies) {
        Map<String, LifecycleData> results = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        int total = dependencies.size();
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        for (Artifact artifact : dependencies) {
            futures.add(executor.submit(() -> {
                String mappedProduct = findProductMatch(artifact);
                try {
                    if (mappedProduct != null) {
                        LifecycleData data = checkProductCycle(artifact, mappedProduct);
                        if (data != null) {
                            String purl = String.format("pkg:maven/%s/%s@%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                            results.put(purl, data);
                        }
                    }
                } catch (Exception e) {
                    // Fail silently for individual artifacts
                } finally {
                    int c = count.incrementAndGet();
                    if (log != null && (c % 5 == 0 || c == total)) {
                        log.info(" - Progress: " + c + "/" + total + " lifecycle products checked...");
                    }
                }
            }));
        }

        for (java.util.concurrent.Future<?> future : futures) {
            try {
                future.get(60, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
        executor.shutdown();

        return results;
    }

    private LifecycleData checkProductCycle(Artifact artifact, String product) throws IOException {
        String url = "https://endoflife.date/api/" + product + ".json";
        String response = HttpClientUtil.get(url);

        if (response != null && !response.isEmpty()) {
            JsonNode cycles = mapper.readTree(response);
            
            // Artifact version, e.g., 2.7.5. We usually match by major.minor prefix.
            String versionStr = artifact.getVersion();
            
            String absoluteLatestVersion = getAbsoluteLatestVersion(cycles);
            
            for (JsonNode cycleNode : cycles) {
                String cycle = cycleNode.path("cycle").asText();
                if (versionStr.startsWith(cycle)) {
                    JsonNode eolNode = cycleNode.path("eol");
                    boolean isEol = false;
                    String eolDateStr = null;

                    if (eolNode.isBoolean()) {
                        isEol = eolNode.asBoolean();
                    } else if (eolNode.isTextual()) {
                        eolDateStr = eolNode.asText();
                        try {
                            LocalDate eolDate = LocalDate.parse(eolDateStr);
                            if (eolDate.isBefore(LocalDate.now())) {
                                isEol = true;
                            }
                        } catch (DateTimeParseException ignored) {
                        }
                    }

                    return new LifecycleData(product, versionStr, isEol, eolDateStr, absoluteLatestVersion);
                }
            }
        }
        return null;
    }

    private String getAbsoluteLatestVersion(JsonNode cycles) {
        // The endoflife.date API typically sorts cycles, but we can't always guarantee. 
        // We look for the cycle that has the highest 'latest' value. A simple heuristic is 
        // often the first element or parsing the versions, but here we just grab the first cycle's 'latest'
        // since endoflife.date API typically returns them in reverse chronological order (newest first).
        if (cycles.isArray() && cycles.size() > 0) {
             JsonNode firstCycle = cycles.get(0);
             return firstCycle.path("latest").asText(null);
        }
        return null;
    }
}
