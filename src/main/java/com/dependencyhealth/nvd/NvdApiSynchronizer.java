package com.dependencyhealth.nvd;

import com.dependencyhealth.util.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NvdApiSynchronizer {

    private static final String API_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private final NvdDatabaseManager dbManager;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public NvdApiSynchronizer(NvdDatabaseManager dbManager, String apiKey) {
        this.dbManager = dbManager;
        this.apiKey = apiKey;
    }

    public void sync() {
        System.out.println("[NVD-NUCLEAR] 🚀 Initializing Parallel High-Concurrency Sync Engine...");
        
        // Multi-threaded fetchers (Network I/O parallelization)
        ExecutorService fetcherPool = Executors.newFixedThreadPool(8);
        // Single-threaded writer (ACID safety + sequential markers)
        ExecutorService dbWriter = Executors.newSingleThreadExecutor();
        
        try {
            dbManager.initializeSchema();
            
            String lastSync = dbManager.getLastSyncTime();
            String queryParams = "";
            int startIdx = 0;
            boolean isInitialSync = false;
            
            if (lastSync != null) {
                String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                queryParams = String.format("?lastModStartDate=%s&lastModEndDate=%s", lastSync, now);
                System.out.println("[NVD-NUCLEAR] Incremental Sync from " + lastSync);
            } else {
                startIdx = dbManager.getLastStartIndex();
                if (startIdx > 0) {
                    System.out.println("[NVD-NUCLEAR] ⏩ Resuming from index " + startIdx);
                } else {
                    isInitialSync = true;
                    System.out.println("[NVD-NUCLEAR] ✨ Full Mirror Mode (250k+ CVEs)");
                    dbManager.dropIndexes();
                }
            }
            
            final int resultsPerPage = 2000;
            final String finalQueryParams = queryParams;
            final boolean fastSync = isInitialSync;

            // 1. Probe for total count
            String probeUrl = API_URL + queryParams + (queryParams.isEmpty() ? "?" : "&") + "resultsPerPage=1";
            JsonNode probe = mapper.readTree(HttpClientUtil.get(probeUrl, apiKey));
            int totalResults = probe.path("totalResults").asInt(0);
            System.out.println("[NVD-NUCLEAR] Total records to fetch: " + totalResults);

            if (totalResults == 0) {
                System.out.println("[NVD-NUCLEAR] No new records found. Sync complete.");
                return;
            }

            // 2. Parallel Dispatcher
            // Rate limit: 50 requests / 30 seconds with key (~600ms per request)
            long rateLimitDelay = (apiKey != null) ? 650 : 6500; 

            for (int i = startIdx; i < totalResults; i += resultsPerPage) {
                final int currentStart = i;
                fetcherPool.submit(() -> {
                    try {
                        String pageUrl = API_URL + finalQueryParams + (finalQueryParams.isEmpty() ? "?" : "&") 
                                       + "resultsPerPage=" + resultsPerPage + "&startIndex=" + currentStart;
                        
                        System.out.println("[NVD-NUCLEAR] [NET-FETCH] Starting page " + currentStart);
                        String json = HttpClientUtil.get(pageUrl, apiKey);
                        
                        if (json != null) {
                            JsonNode root = mapper.readTree(json);
                            JsonNode batch = root.path("vulnerabilities");
                            
                            // Hand off to sequential writer
                            dbWriter.submit(() -> {
                                try {
                                    List<JsonNode> list = new ArrayList<>();
                                    batch.forEach(list::add);
                                    dbManager.insertVulnerabilities(list, fastSync);
                                    
                                    // Update progress marker
                                    int nextProgress = currentStart + batch.size();
                                    dbManager.saveLastStartIndex(nextProgress);
                                    System.out.println("[NVD-NUCLEAR] [DB-WRITE] Saved up to " + nextProgress + "/" + totalResults 
                                                       + " (" + (nextProgress * 100 / totalResults) + "%)");
                                } catch (Exception e) {
                                    System.err.println("[NVD-NUCLEAR] [DB-ERROR] At " + currentStart + ": " + e.getMessage());
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("[NVD-NUCLEAR] [NET-ERROR] At " + currentStart + ": " + e.getMessage());
                    }
                });
                
                // Nuclear throttling
                Thread.sleep(rateLimitDelay);
            }

            fetcherPool.shutdown();
            fetcherPool.awaitTermination(3, TimeUnit.HOURS);
            
            dbWriter.shutdown();
            dbWriter.awaitTermination(1, TimeUnit.HOURS);

            System.out.println("[NVD-NUCLEAR] Rebuilding optimized indexes...");
            dbManager.createIndexes();

            if (startIdx + resultsPerPage >= totalResults || totalResults > 0) {
                String newSyncMarker = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                dbManager.saveLastSyncTime(newSyncMarker);
                dbManager.saveLastStartIndex(0);
                System.out.println("[NVD-NUCLEAR] ✅ MISSION ACCOMPLISHED: Mirroring Complete!");
            }

        } catch (Exception e) {
            System.err.println("[NVD-NUCLEAR] ❌ Fatal engine failure: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fetcherPool.shutdownNow();
            dbWriter.shutdownNow();
        }
    }
}
