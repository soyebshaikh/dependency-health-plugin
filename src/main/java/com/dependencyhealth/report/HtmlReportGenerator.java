package com.dependencyhealth.report;

import com.dependencyhealth.risk.DependencyRiskProfile;
import com.dependencyhealth.vulnerability.Vulnerability;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.List;

/**
 * Generates a premium, modern HTML intelligence report.
 */
public class HtmlReportGenerator {

    public void generateReport(Map<String, DependencyRiskProfile> riskProfiles, File outputFile) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // Calculate Summary Stats
        int totalDeps = riskProfiles.size();
        int criticalCount = 0;
        int highCount = 0;
        int eolCount = 0;
        int totalVulns = 0;

        for (DependencyRiskProfile profile : riskProfiles.values()) {
            if (profile.getRiskScore() >= 100)
                criticalCount++;
            else if (profile.getRiskScore() >= 50)
                highCount++;

            if (profile.getLifecycleData() != null && profile.getLifecycleData().isEol()) {
                eolCount++;
            }
            totalVulns += profile.getVulnerabilities().size();
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Dependency Health Intelligence</title>\n");
        html.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
        html.append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n");
        html.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
        html.append("<style>\n");
        html.append(":root {\n");
        html.append("  --bg: #0f172a;\n");
        html.append("  --card-bg: rgba(30, 41, 59, 0.7);\n");
        html.append("  --primary: #38bdf8;\n");
        html.append("  --critical: #ef4444;\n");
        html.append("  --high: #f97316;\n");
        html.append("  --medium: #eab308;\n");
        html.append("  --low: #22c55e;\n");
        html.append("  --text: #f8fafc;\n");
        html.append("  --text-muted: #94a3b8;\n");
        html.append("  --border: rgba(255, 255, 255, 0.1);\n");
        html.append("}\n");
        html.append("body {\n");
        html.append("  background-color: var(--bg);\n");
        html.append("  color: var(--text);\n");
        html.append("  font-family: 'Inter', sans-serif;\n");
        html.append("  margin: 0;\n");
        html.append("  padding: 40px;\n");
        html.append("  line-height: 1.5;\n");
        html.append("}\n");
        html.append(".container {\n");
        html.append("  max-width: 1200px;\n");
        html.append("  margin: 0 auto;\n");
        html.append("}\n");
        html.append("h1 {\n");
        html.append("  font-size: 2.5rem;\n");
        html.append("  font-weight: 700;\n");
        html.append("  margin-bottom: 8px;\n");
        html.append("  background: linear-gradient(to right, #38bdf8, #818cf8);\n");
        html.append("  -webkit-background-clip: text;\n");
        html.append("  -webkit-text-fill-color: transparent;\n");
        html.append("}\n");
        html.append(".subtitle {\n");
        html.append("  color: var(--text-muted);\n");
        html.append("  margin-bottom: 32px;\n");
        html.append("}\n");
        
        // Dashboard Stats
        html.append(".stats-grid {\n");
        html.append("  display: grid;\n");
        html.append("  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n");
        html.append("  gap: 20px;\n");
        html.append("  margin-bottom: 40px;\n");
        html.append("}\n");
        html.append(".stat-card {\n");
        html.append("  background: var(--card-bg);\n");
        html.append("  backdrop-filter: blur(10px);\n");
        html.append("  border: 1px solid var(--border);\n");
        html.append("  padding: 24px;\n");
        html.append("  border-radius: 16px;\n");
        html.append("  transition: all 0.2s ease;\n");
        html.append("  cursor: pointer;\n");
        html.append("}\n");
        html.append(".stat-card:hover {\n");
        html.append("  transform: translateY(-4px);\n");
        html.append("  background: rgba(47, 61, 85, 0.8);\n");
        html.append("  border-color: var(--primary);\n");
        html.append("}\n");
        html.append(".stat-card.active {\n");
        html.append("  border-color: var(--primary);\n");
        html.append("  box-shadow: 0 0 20px rgba(56, 189, 248, 0.2);\n");
        html.append("}\n");
        html.append(".stat-value {\n");
        html.append("  font-size: 2.25rem;\n");
        html.append("  font-weight: 700;\n");
        html.append("  display: block;\n");
        html.append("}\n");
        html.append(".stat-label {\n");
        html.append("  color: var(--text-muted);\n");
        html.append("  font-size: 0.875rem;\n");
        html.append("  text-transform: uppercase;\n");
        html.append("  letter-spacing: 0.05em;\n");
        html.append("}\n");
        
        // Table Styles
        html.append(".table-container {\n");
        html.append("  background: var(--card-bg);\n");
        html.append("  border: 1px solid var(--border);\n");
        html.append("  border-radius: 16px;\n");
        html.append("  overflow: hidden;\n");
        html.append("}\n");
        html.append("table {\n");
        html.append("  width: 100%;\n");
        html.append("  border-collapse: collapse;\n");
        html.append("  table-layout: fixed;\n");
        html.append("}\n");
        html.append("th {\n");
        html.append("  background: rgba(255, 255, 255, 0.03);\n");
        html.append("  text-align: left;\n");
        html.append("  padding: 16px;\n");
        html.append("  font-size: 0.75rem;\n");
        html.append("  text-transform: uppercase;\n");
        html.append("  letter-spacing: 0.05em;\n");
        html.append("  color: var(--text-muted);\n");
        html.append("  border-bottom: 1px solid var(--border);\n");
        html.append("}\n");
        html.append("th:nth-child(1) { width: 25%; }\n");
        html.append("th:nth-child(2) { width: 15%; }\n");
        html.append("th:nth-child(3) { width: 15%; }\n");
        html.append("th:nth-child(4) { width: 45%; }\n");
        html.append("td {\n");
        html.append("  padding: 16px;\n");
        html.append("  border-bottom: 1px solid var(--border);\n");
        html.append("  vertical-align: top;\n");
        html.append("  word-break: break-word;\n");
        html.append("}\n");
        html.append("tr:last-child td { border-bottom: none; }\n");
        html.append("tr.hidden { display: none; }\n");
        
        // Badges
        html.append(".badge {\n");
        html.append("  padding: 4px 10px;\n");
        html.append("  border-radius: 9999px;\n");
        html.append("  font-size: 0.75rem;\n");
        html.append("  font-weight: 600;\n");
        html.append("  white-space: nowrap;\n");
        html.append("  display: inline-block;\n");
        html.append("}\n");
        html.append(".badge-critical { background: rgba(239, 68, 68, 0.2); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }\n");
        html.append(".badge-high { background: rgba(249, 115, 22, 0.2); color: #fdba74; border: 1px solid rgba(249, 115, 22, 0.3); }\n");
        html.append(".badge-medium { background: rgba(234, 179, 8, 0.2); color: #fde047; border: 1px solid rgba(234, 179, 8, 0.3); }\n");
        html.append(".badge-low { background: rgba(34, 197, 94, 0.2); color: #86efac; border: 1px solid rgba(34, 197, 94, 0.3); }\n");
        
        html.append(".dep-name { font-weight: 600; color: #fff; }\n");
        html.append(".version-pill { background: rgba(255,255,255,0.05); padding: 2px 8px; border-radius: 4px; font-family: monospace; font-size: 0.85rem; }\n");
        html.append(".vuln-list { list-style: none; padding: 0; margin: 0; }\n");
        html.append(".vuln-item {\n");
        html.append("  margin-bottom: 8px;\n");
        html.append("  padding: 8px;\n");
        html.append("  background: rgba(255,255,255,0.02);\n");
        html.append("  border-radius: 8px;\n");
        html.append("  font-size: 0.85rem;\n");
        html.append("}\n");
        html.append(".vuln-id { font-weight: 700; color: var(--primary); text-decoration: none; }\n");
        html.append(".vuln-id:hover { text-decoration: underline; }\n");
        
        // JS Filtering
        html.append("</style>\n");
        html.append("<script>\n");
        html.append("function filterTable(type, element) {\n");
        html.append("  const rows = document.querySelectorAll('tbody tr');\n");
        html.append("  const cards = document.querySelectorAll('.stat-card');\n");
        html.append("  cards.forEach(c => c.classList.remove('active'));\n");
        html.append("  element.classList.add('active');\n");
        html.append("  \n");
        html.append("  rows.forEach(row => {\n");
        html.append("    if (type === 'all') {\n");
        html.append("      row.classList.remove('hidden');\n");
        html.append("    } else if (type === 'eol') {\n");
        html.append("      row.getAttribute('data-eol') === 'true' ? row.classList.remove('hidden') : row.classList.add('hidden');\n");
        html.append("    } else {\n");
        html.append("      row.getAttribute('data-risk').toLowerCase().includes(type.toLowerCase()) ? row.classList.remove('hidden') : row.classList.add('hidden');\n");
        html.append("    }\n");
        html.append("  });\n");
        html.append("}\n");
        html.append("</script>\n");
        html.append("</head><body>\n");
        
        html.append("<div class=\"container\">\n");
        html.append("<h1>Intelligence Report</h1>\n");
        html.append("<p class=\"subtitle\">Security analysis for your software supply chain</p>\n\n");

        // Stats Dashboard
        html.append("<div class=\"stats-grid\">\n");
        html.append("  <div class=\"stat-card active\" onclick=\"filterTable('all', this)\">\n");
        html.append("    <span class=\"stat-value\">").append(totalDeps).append("</span>\n");
        html.append("    <span class=\"stat-label\">Total Scanned</span>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"stat-card\" onclick=\"filterTable('CRITICAL', this)\">\n");
        html.append("    <span class=\"stat-value\" style=\"color: var(--critical)\">").append(criticalCount).append("</span>\n");
        html.append("    <span class=\"stat-label\">Critical Issues</span>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"stat-card\" onclick=\"filterTable('HIGH', this)\">\n");
        html.append("    <span class=\"stat-value\" style=\"color: var(--high)\">").append(highCount).append("</span>\n");
        html.append("    <span class=\"stat-label\">High Risk</span>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"stat-card\" onclick=\"filterTable('eol', this)\">\n");
        html.append("    <span class=\"stat-value\" style=\"color: var(--medium)\">").append(eolCount).append("</span>\n");
        html.append("    <span class=\"stat-label\">End-of-Life</span>\n");
        html.append("  </div>\n");
        html.append("</div>\n\n");

        // Table
        html.append("<div class=\"table-container\">\n");
        html.append("<table>\n");
        html.append("<thead>\n<tr>\n");
        html.append("  <th>Dependency</th>\n");
        html.append("  <th>Lifecycle</th>\n");
        html.append("  <th>Risk Level</th>\n");
        html.append("  <th>Vulnerabilities & Intelligence</th>\n");
        html.append("</tr>\n</thead>\n<tbody>\n");

        for (DependencyRiskProfile profile : riskProfiles.values()) {
            boolean isEol = profile.getLifecycleData() != null && profile.getLifecycleData().isEol();
            String riskLevel = profile.getRiskLevelName();
            
            html.append("<tr data-risk=\"").append(riskLevel).append("\" data-eol=\"").append(isEol).append("\">\n");

            // Dependency Col
            String dependencyName = profile.getPurl();
            String currentVersion = "Unknown";
            if (profile.getPurl().contains("@")) {
                dependencyName = profile.getPurl().substring(0, profile.getPurl().indexOf('@')).replace("pkg:maven/", "");
                currentVersion = profile.getPurl().substring(profile.getPurl().indexOf('@') + 1);
            }
            
            String latestVersion = "Unknown";
            if (profile.getAbsoluteLatestVersion() != null && !profile.getAbsoluteLatestVersion().isEmpty()) {
                latestVersion = profile.getAbsoluteLatestVersion();
            } else if (profile.getLifecycleData() != null && profile.getLifecycleData().getLatestVersion() != null) {
                latestVersion = profile.getLifecycleData().getLatestVersion();
            }

            html.append("  <td>\n");
            html.append("    <div class=\"dep-name\">").append(dependencyName).append("</div>\n");
            html.append("    <div style=\"margin-top: 8px\">\n");
            html.append("      <span class=\"version-pill\" title=\"Current\">").append(currentVersion).append("</span>\n");
            html.append("      <span style=\"color: var(--text-muted); margin: 0 4px\">â†’</span>\n");
            html.append("      <span class=\"version-pill\" title=\"Latest\" style=\"background: rgba(56, 189, 248, 0.1); color: var(--primary)\">").append(latestVersion).append("</span>\n");
            html.append("    </div>\n");
            html.append("  </td>\n");

            // Lifecycle Col
            String eolDate = profile.getLifecycleData() != null && profile.getLifecycleData().getEolDate() != null ? profile.getLifecycleData().getEolDate() : "";
            
            html.append("  <td>\n");
            if (isEol) {
                html.append("    <span class=\"badge badge-critical\">EOL</span>\n");
                if (!eolDate.isEmpty()) html.append("<div style=\"font-size: 0.75rem; color: var(--critical); margin-top: 4px\">Retired: ").append(eolDate).append("</div>\n");
            } else if (profile.getLifecycleData() != null) {
                html.append("    <span class=\"badge badge-low\">Supported</span>\n");
            } else {
                html.append("    <span class=\"badge\" style=\"background: rgba(255,255,255,0.05); color: var(--text-muted)\">Unknown</span>\n");
            }
            html.append("  </td>\n");

            // Risk Col
            String riskBadgeClass = "badge-low";
            if (profile.getRiskScore() >= 100)
                riskBadgeClass = "badge-critical";
            else if (profile.getRiskScore() >= 50)
                riskBadgeClass = "badge-high";
            else if (profile.getRiskScore() >= 20)
                riskBadgeClass = "badge-medium";
            
            html.append("  <td>\n");
            html.append("    <span class=\"badge ").append(riskBadgeClass).append("\">").append(riskLevel).append("</span>\n");
            html.append("  </td>\n");

            // Vulns/Intelligence Col
            html.append("  <td>\n");
            if (profile.getVulnerabilities().isEmpty()) {
                if (profile.getNotes() != null && !profile.getNotes().isEmpty() && !profile.getNotes().equals("Up to date")) {
                    html.append("<div style=\"font-size: 0.85rem; color: var(--text-muted)\">").append(profile.getNotes()).append("</div>");
                } else {
                    html.append("<span style=\"color: var(--low); font-size: 0.85rem\">âœ” Safe</span>");
                }
            } else {
                html.append("    <ul class=\"vuln-list\">\n");
                for (Vulnerability v : profile.getVulnerabilities()) {
                    String vSev = v.getSeverity().toLowerCase();
                    String dotColor = "var(--low)";
                    if (vSev.contains("crit")) dotColor = "var(--critical)";
                    else if (vSev.contains("high")) dotColor = "var(--high)";
                    else if (vSev.contains("med")) dotColor = "var(--medium)";

                    html.append("      <li class=\"vuln-item\" style=\"border-left: 3px solid ").append(dotColor).append("\">\n");
                    
                    if (v.getReferenceUrl() != null && !v.getReferenceUrl().isEmpty()) {
                        html.append("        <a href=\"").append(v.getReferenceUrl()).append("\" target=\"_blank\" class=\"vuln-id\">").append(v.getId()).append("</a> ");
                    } else {
                        html.append("        <span class=\"vuln-id\">").append(v.getId()).append("</span> ");
                    }
                    
                    html.append("        <span style=\"color: var(--text-muted); font-size: 0.75rem\">(").append(v.getSeverity()).append(")</span>\n");
                    html.append("        <div style=\"margin-top: 4px; font-size: 0.8rem; color: #cbd5e1\">").append(v.getDescription()).append("</div>\n");
                    html.append("      </li>\n");
                }
                html.append("    </ul>\n");
            }
            html.append("  </td>\n");

            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n</div>\n</div>\n</body>\n</html>");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
}
