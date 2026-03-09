package com.dependencyhealth.report;

import com.dependencyhealth.risk.DependencyRiskProfile;
import com.dependencyhealth.vulnerability.Vulnerability;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Generates an HTML report.
 */
public class HtmlReportGenerator {

    public void generateReport(Map<String, DependencyRiskProfile> riskProfiles, File outputFile) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Dependency Health Report</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".critical { background-color: #ffcccc; color: #990000; font-weight: bold; }");
        html.append(".high { background-color: #ffddaa; color: #cc5500; }");
        html.append(".medium { background-color: #ffffcc; }");
        html.append("</style></head><body>");

        html.append("<h1>Dependency Health Report</h1>");

        html.append("<table>");
        html.append(
                "<tr><th>Dependency</th><th>Current Version</th><th>Latest / Recommended Version</th><th>Lifecycle Status</th><th>Risk Level</th><th>Notes</th><th>Vulnerabilities</th></tr>");

        for (DependencyRiskProfile profile : riskProfiles.values()) {
            html.append("<tr>");

            // Extract core dependency name and version for clearer display
            String dependencyName = profile.getPurl();
            String currentVersion = "Unknown";
            if (profile.getPurl().contains("@")) {
                dependencyName = profile.getPurl().substring(0, profile.getPurl().indexOf('@')).replace("pkg:maven/",
                        "");
                currentVersion = profile.getPurl().substring(profile.getPurl().indexOf('@') + 1);
            }

            html.append("<td>").append(dependencyName).append("</td>");
            html.append("<td>").append(currentVersion).append("</td>");

            String latestVersion = "Unknown";
            if (profile.getAbsoluteLatestVersion() != null && !profile.getAbsoluteLatestVersion().isEmpty()) {
                latestVersion = profile.getAbsoluteLatestVersion();
            } else if (profile.getLifecycleData() != null && profile.getLifecycleData().getLatestVersion() != null) {
                latestVersion = profile.getLifecycleData().getLatestVersion();
            }
            html.append("<td>").append(latestVersion).append("</td>");

            boolean isEol = profile.getLifecycleData() != null && profile.getLifecycleData().isEol();
            String eolDate = profile.getLifecycleData() != null && profile.getLifecycleData().getEolDate() != null
                    ? profile.getLifecycleData().getEolDate()
                    : "";

            String lifecycleStatus;
            if (isEol) {
                lifecycleStatus = "<span class='critical'> âœ– End-of-Life"
                        + (!eolDate.isEmpty() ? " (" + eolDate + ")" : "") + "</span>";
            } else if (profile.getLifecycleData() != null) {
                lifecycleStatus = "âœ”  Supported";
            } else {
                lifecycleStatus = "Unknown";
            }
            html.append("<td>").append(lifecycleStatus).append("</td>");

            html.append("<td>").append(profile.getRiskLevelName()).append("</td>");

            html.append("<td>").append(profile.getNotes()).append("</td>");

            html.append("<td>");
            if (profile.getVulnerabilities().isEmpty()) {
                html.append("None");
            } else {
                html.append("<ul>");
                for (Vulnerability v : profile.getVulnerabilities()) {
                    String sevClass = v.getSeverity().toLowerCase();
                    html.append("<li class='").append(sevClass).append("'>");
                    html.append("<b>").append(v.getId()).append("</b> (").append(v.getSeverity()).append(") - ");
                    html.append(v.getDescription());
                    html.append("</li>");
                }
                html.append("</ul>");
            }
            html.append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</body></html>");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
}
