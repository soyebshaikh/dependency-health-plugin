package com.dependencyhealth.report;

import com.dependencyhealth.risk.DependencyRiskProfile;
import org.apache.maven.plugin.logging.Log;

import java.util.Map;

/**
 * Generates a console summary report.
 */
public class ConsoleReporter {

    private final Log log;

    public ConsoleReporter(Log log) {
        this.log = log;
    }

    public void generateReport(Map<String, DependencyRiskProfile> riskProfiles) {
        log.info("");
        log.info("=== DEPENDENCY HEALTH SCAN SUMMARY ===");
        log.info(String.format("%-40s | %-15s | %-15s | %-25s | %-15s | %s", "Dependency", "Current Version", "Latest Version", "Lifecycle Status", "Risk Level", "Notes"));
        log.info("---------------------------------------------------------------------------------------------------------------------------------------------------------");

        int totalCritical = 0;
        int totalHigh = 0;
        int totalEol = 0;

        for (DependencyRiskProfile profile : riskProfiles.values()) {
            boolean isEol = profile.getLifecycleData() != null && profile.getLifecycleData().isEol();
            String eolDate = profile.getLifecycleData() != null && profile.getLifecycleData().getEolDate() != null ? 
                    profile.getLifecycleData().getEolDate() : "";
            
            String lifecycleStatus;
            if (isEol) {
                lifecycleStatus = "âŒ EOL" + (!eolDate.isEmpty() ? " (" + eolDate + ")" : "");
                totalEol++;
            } else if (profile.getLifecycleData() != null) {
                lifecycleStatus = "âœ… Supported";
            } else {
                lifecycleStatus = "â“ Unknown";
            }
            
            if (profile.hasCritical()) totalCritical++;
            totalHigh += profile.getHighCount();

            String dependencyName = profile.getPurl();
            String currentVersion = "Unknown";
            if (profile.getPurl().contains("@")) {
                dependencyName = profile.getPurl().substring(0, profile.getPurl().indexOf('@')).replace("pkg:maven/", "");
                currentVersion = profile.getPurl().substring(profile.getPurl().indexOf('@') + 1);
            }

            String latestVersion = profile.getLifecycleData() != null && profile.getLifecycleData().getLatestVersion() != null ? 
                    profile.getLifecycleData().getLatestVersion() : "Unknown";

            log.info(String.format("%-40s | %-15s | %-15s | %-25s | %-15s | %s",
                    truncate(dependencyName, 40),
                    truncate(currentVersion, 15),
                    truncate(latestVersion, 15),
                    truncate(lifecycleStatus, 25),
                    profile.getRiskLevelName(),
                    profile.getNotes()));
        }

        log.info("---------------------------------------------------------------------------------------------------------------------------------------------------------");
        log.info("Total Dependencies Scanned: " + riskProfiles.size());
        log.info("Dependencies with CRITICAL: " + totalCritical);
        log.info("Total HIGH vulnerabilities: " + totalHigh);
        log.info("Dependencies End-of-Life: " + totalEol);
        log.info("======================================");
        log.info("");
    }

    private String truncate(String str, int length) {
        if (str.length() <= length) {
            return str;
        }
        return "..." + str.substring(str.length() - length + 3);
    }
}
