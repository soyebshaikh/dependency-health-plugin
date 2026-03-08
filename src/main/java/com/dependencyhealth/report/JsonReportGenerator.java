package com.dependencyhealth.report;

import com.dependencyhealth.risk.DependencyRiskProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Generates a JSON report for CI/CD ingestion.
 */
public class JsonReportGenerator {

    private final ObjectMapper mapper;

    public JsonReportGenerator() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void generateReport(Map<String, DependencyRiskProfile> riskProfiles, File outputFile) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        mapper.writeValue(outputFile, riskProfiles.values());
    }
}
