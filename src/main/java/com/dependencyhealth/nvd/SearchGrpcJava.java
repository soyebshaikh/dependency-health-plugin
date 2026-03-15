package com.dependencyhealth.nvd;
import java.sql.*;
import java.io.File;

public class SearchGrpcJava {
    public static void main(String[] args) throws Exception {
        String dbPath = new File(System.getProperty("user.home"), ".m2/dependency-health/nvd-cache/nvd.db").getAbsolutePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Searching for products containing 'grpc'...");
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT product FROM cpes WHERE product LIKE '%grpc%' LIMIT 20");
            while (rs.next()) {
                System.out.println("Product: " + rs.getString("product"));
            }
            
            System.out.println("\nSearching for CVEs for product 'grpc'...");
            ResultSet rs2 = stmt.executeQuery("SELECT cve_id FROM cpes WHERE product='grpc' LIMIT 10");
            while (rs2.next()) {
                System.out.println("CVE for grpc: " + rs2.getString("cve_id"));
            }
        }
    }
}
