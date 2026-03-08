package com.dependencyhealth.policy;

import com.dependencyhealth.risk.DependencyRiskProfile;
import org.apache.maven.plugin.MojoFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Engine to evaluate policies against aggregated risks.
 */
public class PolicyEngine {

    private final boolean failOnCritical;
    private final int failOnHighCount;
    private final boolean failOnEol;

    public PolicyEngine(boolean failOnCritical, int failOnHighCount, boolean failOnEol) {
        this.failOnCritical = failOnCritical;
        this.failOnHighCount = failOnHighCount;
        this.failOnEol = failOnEol;
    }

    /**
     * Executes policy rules. Throws exception if policies are violated.
     *
     * @param riskProfiles The aggregated risk profiles.
     * @throws MojoFailureException if the build should fail based on policy.
     */
    public void evaluate(Map<String, DependencyRiskProfile> riskProfiles) throws MojoFailureException {
        List<String> violations = new ArrayList<>();

        for (DependencyRiskProfile profile : riskProfiles.values()) {
            if (failOnCritical && profile.hasCritical()) {
                violations.add(profile.getPurl() + " has CRITICAL vulnerabilities.");
            }
            if (failOnHighCount > 0 && profile.getHighCount() >= failOnHighCount) {
                violations.add(profile.getPurl() + " has " + profile.getHighCount() + " HIGH vulnerabilities (Limit: " + failOnHighCount + ").");
            }
            if (failOnEol && profile.getLifecycleData() != null && profile.getLifecycleData().isEol()) {
                violations.add(profile.getPurl() + " is End-of-Life.");
            }
        }

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nDependency Health Policy Violations:\n");
            for (String v : violations) {
                sb.append(" - ").append(v).append("\n");
            }
            throw new MojoFailureException(sb.toString());
        }
    }
}
