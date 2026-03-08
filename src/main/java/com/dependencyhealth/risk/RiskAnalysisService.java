package com.dependencyhealth.risk;

import com.dependencyhealth.lifecycle.LifecycleData;
import com.dependencyhealth.vulnerability.Vulnerability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to aggregate risks from various intelligence sources.
 */
public class RiskAnalysisService {

    public Map<String, DependencyRiskProfile> analyzeRisk(
            java.util.Set<org.apache.maven.artifact.Artifact> dependencies,
            Map<String, List<Vulnerability>> vulnerabilityMap,
            Map<String, LifecycleData> lifecycleMap) {

        Map<String, DependencyRiskProfile> riskProfiles = new HashMap<>();

        // Initialize for ALL dependencies to ensure they show up in the report
        if (dependencies != null) {
            for (org.apache.maven.artifact.Artifact artifact : dependencies) {
                String purl = String.format("pkg:maven/%s/%s@%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                riskProfiles.put(purl, new DependencyRiskProfile(purl));
            }
        }

        // Process Vulnerabilities
        if (vulnerabilityMap != null) {
            for (Map.Entry<String, List<Vulnerability>> entry : vulnerabilityMap.entrySet()) {
                String purl = entry.getKey();
                DependencyRiskProfile profile = riskProfiles.computeIfAbsent(purl, DependencyRiskProfile::new);

                for (Vulnerability v : entry.getValue()) {
                    profile.addVulnerability(v);
                }
            }
        }

        // Process Lifecycle data
        if (lifecycleMap != null) {
            for (Map.Entry<String, LifecycleData> entry : lifecycleMap.entrySet()) {
                String purl = entry.getKey();
                DependencyRiskProfile profile = riskProfiles.computeIfAbsent(purl, DependencyRiskProfile::new);
                profile.setLifecycleData(entry.getValue());
            }
        }

        return riskProfiles;
    }
}
