package com.dependencyhealth.risk;

import com.dependencyhealth.lifecycle.LifecycleData;
import com.dependencyhealth.vulnerability.Vulnerability;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RiskAnalysisServiceTest {

    @Test
    public void testRiskAnalysis() {
        RiskAnalysisService service = new RiskAnalysisService();

        Map<String, List<Vulnerability>> vulns = new HashMap<>();
        Vulnerability v1 = new Vulnerability("CVE-2021-44228", "NVD", "CRITICAL", 10.0, "Log4Shell", "url");
        vulns.put("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1", Collections.singletonList(v1));

        Map<String, LifecycleData> eolData = new HashMap<>();
        LifecycleData l1 = new LifecycleData("spring-boot", "1.5.0", true, "2019-08-01", "3.1.2");
        eolData.put("pkg:maven/org.springframework.boot/spring-boot@1.5.0", l1);

        Map<String, DependencyRiskProfile> profiles = service.analyzeRisk(null, vulns, eolData);

        assertEquals(2, profiles.size());

        DependencyRiskProfile log4jProfile = profiles.get("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1");
        assertTrue(log4jProfile.hasCritical());
        assertEquals(100, log4jProfile.getRiskScore());

        DependencyRiskProfile springProfile = profiles.get("pkg:maven/org.springframework.boot/spring-boot@1.5.0");
        assertTrue(springProfile.getLifecycleData().isEol());
        assertEquals(75, springProfile.getRiskScore());
    }
}
