# Enterprise-Grade Maven Dependency Intelligence Plugin

This plugin analyzes your Maven project dependencies (direct and transitive) during the build lifecycle to detect vulnerabilities, End-of-Life (EOL) components, and overall risk.

- **Offline Vulnerability Scanning**: Local NVD database for lightning-fast, highly-reliable scans without NVD API rate limits.
- **"Nuclear" Parallel Engine**: High-concurrency scanning of vulnerabilities, versions, and lifecycles (Step 4/4.5 is now up to 10x faster).
- **Lifecycle Detection**: Integrates with endoflife.date API with smart version-based fallbacks (`[V] Current`, `[?] Outdated`).
- **Latest Version Resolution**: Resilient parallel lookups against Maven Central.
- **SBOM Generation**: Automatically produces a CycloneDX JSON Software Bill of Materials.
- **Risk & Policy Engine**: Assigns risk scores and fails the build flexibly. Use `-DskipPolicy=true` for diagnostic scans.
- **Comprehensive Reporting**: Console summary (ASCII-safe), HTML detailed report, and JSON CI/CD integration report.

## Usage

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.dependencyhealth</groupId>
            <artifactId>dependency-health-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>scan-dependencies</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <failOnCritical>true</failOnCritical>
                <failOnHighCount>5</failOnHighCount>
                <failOnEol>true</failOnEol>
                <!-- Optional: Use NVD API Key to avoid strict rate limits (5/min to 50/min) -->
                <nvdApiKey>your-nvd-api-key</nvdApiKey>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Run the scan:
```bash
mvn dependency-health:scan-dependencies
```
*The plugin will automatically sync vulnerability data if your local database is missing or more than 24 hours old. By default, the scan will NOT fail the build on vulnerabilities (policy enforcement is skipped for a smoother developer experience).*

### Strict Policy Enforcement
To make the build fail if vulnerabilities are found, set `skipPolicy` to `false`:
```bash
mvn dependency-health:scan-dependencies -DskipPolicy=false
```

### Multi-Module Projects

For multi-module (parent/child) projects, use the `aggregate` goal to get a single unified report for the entire reactor:

```bash
mvn dependency-health:aggregate
```
*This also supports automatic synchronization and the default non-blocking policy.*

### Interactive Decision Graph

Both normal Scans and Aggregate Scans will automatically generate an interactive HTML dependency graph inside the target folder (`dependency-graph.html`) without you needing to pass any extra arguments!

## Output Artifacts

- `target/dependency-sbom.json`: CycloneDX SBOM.
- `target/dependency-health-report.html`: Detailed HTML report.
- `target/dependency-health-report.json`: JSON output for external systems.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Copyright (c) 2026 Soyeb Shaikh.
