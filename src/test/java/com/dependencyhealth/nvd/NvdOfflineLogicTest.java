package com.dependencyhealth.nvd;

import com.dependencyhealth.vulnerability.NvdClient;
import com.dependencyhealth.vulnerability.Vulnerability;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class NvdOfflineLogicTest {

    private NvdDatabaseManager dbManager;
    private ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() throws Exception {
        // Use a test-specific DB path
        dbManager = new NvdDatabaseManager(tempDir.toFile());
        dbManager.initializeSchema();
    }

    @Test
    public void testVulnerabilityMatching() throws Exception {
        // 1. Mock a vulnerability JSON structure
        ObjectNode vuln = mapper.createObjectNode();
        ObjectNode cve = vuln.putObject("cve");
        cve.put("id", "CVE-2024-TEST");
        
        cve.putArray("descriptions")
           .addObject().put("lang", "en").put("value", "Test vulnerability for Apache CXF");
        
        ObjectNode metrics = cve.putObject("metrics");
        metrics.putArray("cvssMetricV31")
               .addObject().putObject("cvssData").put("baseScore", 8.5);
               
        ObjectNode config = cve.putArray("configurations").addObject();
        ObjectNode node = config.putArray("nodes").addObject();
        node.putArray("cpeMatch")
            .addObject()
            .put("vulnerable", true)
            .put("criteria", "cpe:2.3:a:apache:cxf:*:*:*:*:*:*:*:*")
            .put("versionStartIncluding", "4.0.0")
            .put("versionEndIncluding", "4.1.1");

        // 2. Insert into DB
        dbManager.insertVulnerabilities(Collections.singletonList((com.fasterxml.jackson.databind.JsonNode) vuln), false);

        // 3. Query DB
        List<NvdDatabaseManager.CpeMatch> matches = dbManager.findVulnerabilities("apache", "cxf");
        assertFalse(matches.isEmpty(), "Should find the inserted CVE");
        assertEquals("CVE-2024-TEST", matches.get(0).cveId);

        // 4. Test NvdClient Matching Logic
        NvdClient client = new NvdClient(dbManager);
        
        Artifact vulnerableArtifact = new DefaultArtifact("org.apache.cxf", "cxf-core", "4.1.1", "compile", "jar", "", new DefaultArtifactHandler("jar"));
        Artifact safeArtifact = new DefaultArtifact("org.apache.cxf", "cxf-core", "4.1.2", "compile", "jar", "", new DefaultArtifactHandler("jar"));
        Artifact oldSafeArtifact = new DefaultArtifact("org.apache.cxf", "cxf-core", "3.9.9", "compile", "jar", "", new DefaultArtifactHandler("jar"));

        Map<String, List<com.dependencyhealth.vulnerability.Vulnerability>> results = client.checkVulnerabilities(Set.of(vulnerableArtifact, safeArtifact, oldSafeArtifact));

        // Purls
        String vulnPurl = "pkg:maven/org.apache.cxf/cxf-core@4.1.1";
        String safePurl = "pkg:maven/org.apache.cxf/cxf-core@4.1.2";
        
        assertTrue(results.containsKey(vulnPurl), "4.1.1 should be vulnerable");
        assertFalse(results.containsKey(safePurl), "4.1.2 should be safe (outside range)");
        
        List<com.dependencyhealth.vulnerability.Vulnerability> vulns = results.get(vulnPurl);
        assertEquals(1, vulns.size(), "Should find exactly 1 vulnerability (idempotent insert)");
        assertEquals("CVE-2024-TEST", vulns.get(0).getId());
        assertEquals("HIGH", vulns.get(0).getSeverity());
    }
}
