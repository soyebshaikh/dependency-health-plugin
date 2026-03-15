package com.dependencyhealth.nvd;
import java.sql.*;
import java.io.File;

public class InspectCve {
    public static void main(String[] args) throws Exception {
        String dbPath = new File(System.getProperty("user.home"), ".m2/dependency-health/nvd-cache/nvd.db").getAbsolutePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Checking CVE-2024-11407...");
            ResultSet rs = stmt.executeQuery("SELECT * FROM cves WHERE id='CVE-2024-11407'");
            if (rs.next()) {
                System.out.println("Description: " + rs.getString("description"));
            }
            
            System.out.println("\nChecking CPEs for CVE-2024-11407...");
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM cpes WHERE cve_id='CVE-2024-11407'");
            while (rs2.next()) {
                System.out.println("CPE: vendor=" + rs2.getString("vendor") + ", product=" + rs2.getString("product") + 
                    ", vStartIncl=" + rs2.getString("version_start_including") + ", vEndIncl=" + rs2.getString("version_end_including") +
                    ", exact=" + rs2.getString("exact_version"));
            }
        }
    }
}
