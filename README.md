# Enterprise-Grade Maven Dependency Intelligence Plugin

This plugin analyzes your Maven project dependencies (direct and transitive) during the build lifecycle to detect vulnerabilities, End-of-Life (EOL) components, and overall risk.

- **Offline Vulnerability Scanning**: Local NVD database for lightning-fast, highly-reliable scans without NVD API rate limits.
- **Interactive Dashboard**: Modern HTML report with real-time filtering cards (Critical, High, EOL).
- **Vulnerability Intelligence**: Clickable CVE/OSS Index IDs that link directly to external vulnerability databases.
- **Decision Graph**: Interactive D3.js visualization with a built-in "Find Duplicate Versions" search tool and violet conflict highlighting.
- **"Nuclear" Parallel Engine**: High-concurrency scanning of vulnerabilities, versions, and lifecycles (Step 4/4.5 is now up to 10x faster).

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

Both normal Scans and Aggregate Scans will automatically generate an interactive HTML dependency graph inside the target folder (`dependency-graph.html`). 
- **Duplicate Search**: Includes a "Find Duplicate Versions" tool to instantly identify and highlight version conflicts in violet.
- **Zoom & Focus**: Click on conflicts in the tools panel to automatically zoom and center on the problematic dependency.

## Output Artifacts

- `target/dependency-sbom.json`: CycloneDX SBOM.
- `target/dependency-health-report.html`: Detailed HTML report.
- `target/dependency-health-report.json`: JSON output for external systems.

## Troubleshooting

### Database Issues (Corrupted or Partial Sync)
If the scan hangs, fails due to database errors, or the NVD data was interrupted during download, use this command to clear the local cache and restart:

**Windows (PowerShell):**
```powershell
Remove-Item -Path "$HOME\.m2\dependency-health\nvd-cache" -Recurse -Force
```

**Linux / macOS / Git Bash:**
```bash
rm -rf ~/.m2/dependency-health/nvd-cache
```

Once cleared, run the scan again to trigger a fresh synchronization.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Copyright (c) 2026 Soyeb Shaikh.
