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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class NvdApiSynchronizer {

    private static final String API_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private final NvdDatabaseManager dbManager;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter NVD_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

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
        // Backpressure: Limit concurrent pages in memory to 4 (prevent GCLocker/OOM)
        Semaphore memoryGate = new Semaphore(4);

        try {
            dbManager.initializeSchema();

            String lastSync = dbManager.getLastSyncTime();
            String queryParams = "";
            int startIdx = 0;
            boolean isInitialSync = false;

            if (lastSync != null) {
                String now = ZonedDateTime.now(ZoneOffset.UTC).format(NVD_DATE_FORMATTER);
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
            // If the key is just the placeholder "YOUR_API_KEY", use the unauthenticated rate (6500ms)
            long rateLimitDelay = (apiKey != null && !apiKey.equalsIgnoreCase("YOUR_API_KEY")) ? 650 : 6500;

            for (int i = startIdx; i < totalResults; i += resultsPerPage) {
                final int currentStart = i;
                
                // Block if too many pages are already in memory waiting to be written
                memoryGate.acquire();
                
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
                                    System.out.println(
                                            "[NVD-NUCLEAR] [DB-WRITE] Saved up to " + nextProgress + "/" + totalResults
                                                    + " (" + (nextProgress * 100 / totalResults) + "%)");
                                } catch (Exception e) {
                                    System.err.println(
                                            "[NVD-NUCLEAR] [DB-ERROR] At " + currentStart + ": " + e.getMessage());
                                } finally {
                                    // Release the gate once DB write is done
                                    memoryGate.release();
                                }
                            });
                        } else {
                            memoryGate.release();
                        }
                    } catch (Exception e) {
                        System.err.println("[NVD-NUCLEAR] [NET-ERROR] At " + currentStart + ": " + e.getMessage());
                        memoryGate.release();
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
                String newSyncMarker = ZonedDateTime.now(ZoneOffset.UTC).format(NVD_DATE_FORMATTER);
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
