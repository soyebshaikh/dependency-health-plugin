package com.dependencyhealth.risk;

import com.dependencyhealth.lifecycle.LifecycleData;
import com.dependencyhealth.vulnerability.Vulnerability;

import java.util.ArrayList;
import java.util.List;

/**
 * Merged risk profile for a dependency.
 */
public class DependencyRiskProfile {
    private String purl;
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
    private LifecycleData lifecycleData;
    private String absoluteLatestVersion;
    private int riskScore = 0;

    public DependencyRiskProfile(String purl) {
        this.purl = purl;
    }

    public void addVulnerability(Vulnerability v) {
        if (v == null || v.getId() == null)
            return;

        String newId = v.getId().trim().toUpperCase();

        // Deduplicate by ID (CVE code)
        boolean exists = vulnerabilities.stream()
                .anyMatch(existing -> existing.getId().trim().toUpperCase().equals(newId));

        if (!exists) {
            vulnerabilities.add(v);
            calculateRiskScore();
        }
    }

    public void setLifecycleData(LifecycleData data) {
        this.lifecycleData = data;
        calculateRiskScore();
    }

    public void setAbsoluteLatestVersion(String v) {
        this.absoluteLatestVersion = v;
    }

    private void calculateRiskScore() {
        riskScore = 0;
        for (Vulnerability v : vulnerabilities) {
            switch (v.getSeverity().toUpperCase()) {
                case "CRITICAL":
                    riskScore += 100;
                    break;
                case "HIGH":
                    riskScore += 50;
                    break;
                case "MEDIUM":
                    riskScore += 20;
                    break;
                case "LOW":
                    riskScore += 5;
                    break;
                default:
                    riskScore += 1;
            }
        }
        if (lifecycleData != null && lifecycleData.isEol()) {
            riskScore += 75; // EOL is considered highly risky
        }
    }

    public String getPurl() {
        return purl;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public LifecycleData getLifecycleData() {
        return lifecycleData;
    }

    public String getAbsoluteLatestVersion() {
        return absoluteLatestVersion;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public boolean hasCritical() {
        return vulnerabilities.stream().anyMatch(v -> "CRITICAL".equalsIgnoreCase(v.getSeverity()));
    }

    public long getHighCount() {
        return vulnerabilities.stream().filter(v -> "HIGH".equalsIgnoreCase(v.getSeverity())).count();
    }

    public String getRiskLevelName() {
        if (riskScore >= 100)
            return "! CRITICAL";
        if (riskScore >= 50)
            return "* HIGH";
        if (riskScore >= 20)
            return "MEDIUM";
        return ". LOW";
    }

    public String getNotes() {
        if (lifecycleData != null && lifecycleData.isEol()) {
            return "Upgrade to supported major version";
        }
        if (hasCritical() || getHighCount() > 0) {
            return "Contains severe vulnerabilities, patch immediately";
        }
        if (riskScore > 0) {
            return "Consider updating or reviewing vulnerabilities";
        }

        // Priority 1: Maven Central direct check (the most accurate)
        if (absoluteLatestVersion != null && !absoluteLatestVersion.isEmpty()) {
            if (!absoluteLatestVersion.equals(purl.substring(purl.lastIndexOf('@') + 1))) {
                return "Safe but outdated";
            }
        }
        // Priority 2: Fallback to EOL Client heuristics
        else if (lifecycleData != null && lifecycleData.getLatestVersion() != null
                && !lifecycleData.getVersion().equals(lifecycleData.getLatestVersion())) {
            return "Safe but outdated";
        }

        return "Up to date";
    }
}
