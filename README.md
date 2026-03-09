# Enterprise-Grade Maven Dependency Intelligence Plugin

This plugin analyzes your Maven project dependencies (direct and transitive) during the build lifecycle to detect vulnerabilities, End-of-Life (EOL) components, and overall risk.

## Features
- **Vulnerability Scanning**: Queries Sonatype OSS Index.
- **Lifecycle Detection**: Integrates with endoflife.date API to flag EOL technologies.
- **Latest Version Resolution**: Queries Maven Central Search API to map dependencies to their true latest published version.
- **SBOM Generation**: Automatically produces a CycloneDX JSON Software Bill of Materials.
- **Risk & Policy Engine**: Assigns risk scores and fails the build flexibly based on constraints like critical severity flaws or EOL versions.
- **Comprehensive Reporting**: Console summary, HTML detailed report, and JSON CI/CD integration report.

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

Run the verify phase:

```bash
mvn verify
```

### Multi-Module Projects

If you have a multi-module (parent/child) Maven project and you want a single unified report for all modules rather than individual reports, run the `aggregate` goal from the root directory:

```bash
mvn dependency-health:aggregate
```

### Interactive Decision Graph

Both normal Scans and Aggregate Scans will automatically generate an interactive HTML dependency graph inside the target folder (`dependency-graph.html`) without you needing to pass any extra arguments!

## Output Artifacts

- `target/dependency-sbom.json`: CycloneDX SBOM.
- `target/dependency-health-report.html`: Detailed HTML report.
- `target/dependency-health-report.json`: JSON output for external systems.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Copyright (c) 2026 Soyeb Shaikh.
