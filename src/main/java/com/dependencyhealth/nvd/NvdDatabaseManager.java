package com.dependencyhealth.nvd;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NvdDatabaseManager {

    private final String dbPath;
    private final String jdbcUrl;

    public NvdDatabaseManager() {
        this(new File(System.getProperty("user.home"), ".m2/dependency-health/nvd-cache"));
    }

    public NvdDatabaseManager(File nvdDir) {
        if (!nvdDir.exists()) {
            nvdDir.mkdirs();
        }
        this.dbPath = new File(nvdDir, "nvd.db").getAbsolutePath();
        this.jdbcUrl = "jdbc:sqlite:" + this.dbPath;
    }

    public void initializeSchema() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Meta table for tracking syncs
            stmt.execute("CREATE TABLE IF NOT EXISTS meta (" +
                         "key TEXT PRIMARY KEY, " +
                         "value TEXT)");

            // Main CVE table
            stmt.execute("CREATE TABLE IF NOT EXISTS cves (" +
                         "id TEXT PRIMARY KEY, " +
                         "description TEXT, " +
                         "cvss_score REAL, " +
                         "severity TEXT)");

            // CPE configuration logic table (links vulnerabilities to dependency versions)
            stmt.execute("CREATE TABLE IF NOT EXISTS cpes (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "cve_id TEXT, " +
                         "vendor TEXT, " +
                         "product TEXT, " +
                         "version_start_including TEXT, " +
                         "version_start_excluding TEXT, " +
                         "version_end_including TEXT, " +
                         "version_end_excluding TEXT, " +
                         "exact_version TEXT, " +
                         "FOREIGN KEY(cve_id) REFERENCES cves(id))");

            // Accelerate deletions during sync!
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cpe_cve ON cpes (cve_id)");
        }
    }

    private Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA synchronous = OFF");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA cache_size = 32000"); // Use ~32MB of cache
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA mmap_size = 536870912"); // Use 512MB for memory mapping
        }
        return conn;
    }

    public void createIndexes() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cpe_product ON cpes (vendor, product)");
        }
    }

    public void dropIndexes() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP INDEX IF EXISTS idx_cpe_product");
        }
    }

    public void saveLastSyncTime(String isoTimestamp) throws Exception {
        saveMeta("lastModStartDate", isoTimestamp);
    }

    public void saveLastStartIndex(int index) throws Exception {
        saveMeta("lastStartIndex", String.valueOf(index));
    }

    private void saveMeta(String key, String value) throws Exception {
        String sql = "INSERT INTO meta (key, value) VALUES (?, ?) " +
                     "ON CONFLICT(key) DO UPDATE SET value=excluded.value";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }

    public String getLastSyncTime() throws Exception {
        return getMeta("lastModStartDate");
    }

    public int getLastStartIndex() throws Exception {
        String val = getMeta("lastStartIndex");
        return val != null ? Integer.parseInt(val) : 0;
    }

    public boolean exists() {
        return new File(dbPath).exists();
    }

    public long getLastModified() {
        return new File(dbPath).lastModified();
    }

    private String getMeta(String key) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT value FROM meta WHERE key=?")) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (Exception e) {
            // Table might not exist yet
        }
        return null;
    }

    public void insertVulnerabilities(List<JsonNode> vulnerabilities, boolean isInitialSync) throws Exception {
        String insertCve = "INSERT OR REPLACE INTO cves (id, description, cvss_score, severity) VALUES (?, ?, ?, ?)";
        String deleteCpes = "DELETE FROM cpes WHERE cve_id = ?";
        String insertCpe = "INSERT INTO cpes (cve_id, vendor, product, version_start_including, " +
                           "version_start_excluding, version_end_including, version_end_excluding, exact_version) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Massive speedup for batch inserts
            
            try (PreparedStatement cveStmt = conn.prepareStatement(insertCve);
                 PreparedStatement delStmt = conn.prepareStatement(deleteCpes);
                 PreparedStatement cpeStmt = conn.prepareStatement(insertCpe)) {

                for (JsonNode vulnRoot : vulnerabilities) {
                    JsonNode cveNode = vulnRoot.path("cve");
                    String id = cveNode.path("id").asText();
                    
                    // Delete old CPEs for this CVE to avoid duplicates (SKIP ON FIRST RUN)
                    if (!isInitialSync) {
                        delStmt.setString(1, id);
                        delStmt.addBatch();
                    }

                    String desc = getEnglishDescription(cveNode);
                    double score = getCvssScore(cveNode);
                    String severity = categorizeCvss(score);
                    
                    cveStmt.setString(1, id);
                    cveStmt.setString(2, desc);
                    cveStmt.setDouble(3, score);
                    cveStmt.setString(4, severity);
                    cveStmt.addBatch();
                    
                    // Parse configurations (CPE matches)
                    JsonNode configs = cveNode.path("configurations");
                    if (configs.isArray()) {
                        for (JsonNode config : configs) {
                            JsonNode nodes = config.path("nodes");
                            if (nodes.isArray()) {
                                for (JsonNode node : nodes) {
                                    JsonNode matches = node.path("cpeMatch");
                                    if (matches.isArray()) {
                                        for (JsonNode match : matches) {
                                            if (match.path("vulnerable").asBoolean(true)) {
                                                String cpeStr = match.path("criteria").asText("");
                                                // cpe:2.3:a:vendor:product:...
                                                String[] parts = cpeStr.split(":");
                                                if (parts.length >= 5) {
                                                    String vendor = parts[3];
                                                    String product = parts[4];
                                                    
                                                    String vStartIncl = match.path("versionStartIncluding").asText(null);
                                                    String vStartExcl = match.path("versionStartExcluding").asText(null);
                                                    String vEndIncl = match.path("versionEndIncluding").asText(null);
                                                    String vEndExcl = match.path("versionEndExcluding").asText(null);
                                                    
                                                    // If no ranges are defined, the part[5] usually contains the exact version
                                                    String exactVersion = null;
                                                    if (vStartIncl == null && vStartExcl == null && vEndIncl == null && vEndExcl == null && parts.length >= 6) {
                                                        exactVersion = parts[5];
                                                        if ("*".equals(exactVersion) || "-".equals(exactVersion)) {
                                                            exactVersion = null;
                                                        }
                                                    }
                                                    
                                                    cpeStmt.setString(1, id);
                                                    cpeStmt.setString(2, vendor);
                                                    cpeStmt.setString(3, product);
                                                    cpeStmt.setString(4, vStartIncl);
                                                    cpeStmt.setString(5, vStartExcl);
                                                    cpeStmt.setString(6, vEndIncl);
                                                    cpeStmt.setString(7, vEndExcl);
                                                    cpeStmt.setString(8, exactVersion);
                                                    cpeStmt.addBatch();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (!isInitialSync) {
                    delStmt.executeBatch();
                }
                cveStmt.executeBatch();
                cpeStmt.executeBatch();
            }
            conn.commit();
        }
    }

    public List<CpeMatch> findVulnerabilities(String vendor, String product) throws Exception {
        List<CpeMatch> matches = new ArrayList<>();
        String sql = "SELECT c.id, c.description, c.cvss_score, c.severity, " +
                     "p.version_start_including, p.version_start_excluding, " +
                     "p.version_end_including, p.version_end_excluding, p.exact_version " +
                     "FROM cpes p JOIN cves c ON p.cve_id = c.id " +
                     "WHERE p.vendor = ? AND p.product = ?";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vendor);
            pstmt.setString(2, product);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    matches.add(new CpeMatch(
                        rs.getString("id"),
                        rs.getString("description"),
                        rs.getDouble("cvss_score"),
                        rs.getString("severity"),
                        rs.getString("version_start_including"),
                        rs.getString("version_start_excluding"),
                        rs.getString("version_end_including"),
                        rs.getString("version_end_excluding"),
                        rs.getString("exact_version")
                    ));
                }
            }
        }
        return matches;
    }

    public static class CpeMatch {
        public final String cveId;
        public final String description;
        public final double cvssScore;
        public final String severity;
        public final String vStartIncl;
        public final String vStartExcl;
        public final String vEndIncl;
        public final String vEndExcl;
        public final String exactVersion;

        public CpeMatch(String cveId, String description, double cvssScore, String severity,
                        String vStartIncl, String vStartExcl, String vEndIncl, String vEndExcl,
                        String exactVersion) {
            this.cveId = cveId;
            this.description = description;
            this.cvssScore = cvssScore;
            this.severity = severity;
            this.vStartIncl = vStartIncl;
            this.vStartExcl = vStartExcl;
            this.vEndIncl = vEndIncl;
            this.vEndExcl = vEndExcl;
            this.exactVersion = exactVersion;
        }
    }

    private String getEnglishDescription(JsonNode cveNode) {
        JsonNode descNodes = cveNode.path("descriptions");
        if (descNodes.isArray()) {
            for (JsonNode d : descNodes) {
                if ("en".equals(d.path("lang").asText())) {
                    return d.path("value").asText();
                }
            }
        }
        return "No description available";
    }

    private double getCvssScore(JsonNode cveNode) {
        JsonNode metrics = cveNode.path("metrics");
        JsonNode cvssMetricV31 = metrics.path("cvssMetricV31");
        if (cvssMetricV31.isArray() && cvssMetricV31.size() > 0) {
            return cvssMetricV31.get(0).path("cvssData").path("baseScore").asDouble(0.0);
        }
        JsonNode cvssMetricV30 = metrics.path("cvssMetricV30");
        if (cvssMetricV30.isArray() && cvssMetricV30.size() > 0) {
            return cvssMetricV30.get(0).path("cvssData").path("baseScore").asDouble(0.0);
        }
        JsonNode cvssMetricV2 = metrics.path("cvssMetricV2");
        if (cvssMetricV2.isArray() && cvssMetricV2.size() > 0) {
            return cvssMetricV2.get(0).path("cvssData").path("baseScore").asDouble(0.0);
        }
        return 0.0;
    }

    private String categorizeCvss(double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        return "LOW";
    }
}
