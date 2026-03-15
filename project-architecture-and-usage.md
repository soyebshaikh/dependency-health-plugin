# Dependency Health Maven Plugin - Architecture & Usage Guide

## 1. Project Architecture
The project follows a clean, modular architecture separating concerns into specific packages. It hooks into the Maven `verify` lifecycle phase to perform its analysis.

*   **`mojo`**: Contains `DependencyHealthScanMojo`, the main entry point (the Maven Plugin goal). Orchestrates the entire scan process.
*   **`dependency`**: Handles interaction with Maven APIs (`MavenProject`, `DependencyGraphBuilder`) to resolve direct and transitive dependencies.
*   **`sbom`**: Generates a standard CycloneDX JSON Software Bill of Materials.
*   **`vulnerability`**: Interfaces (`VulnerabilityClient`) and concrete implementations (`OssIndexClient`, `NvdClient`). The `NvdClient` now performs lightning-fast lookups against a local SQLite database.
*   **`nvd`**: The "Nuclear" Parallel Sync Engine. Handles high-speed synchronization of NIST data into a local SQLite cache with strict rate limiting and termination safety.
*   **`lifecycle`**: Interfaces (`LifecycleClient`) and concrete implementations (`EolClient`) for checking End-of-Life status.
*   **`risk`**: The `RiskAnalysisService` merges vulnerability and lifecycle data into a `DependencyRiskProfile`, calculating a unified risk score.
*   **`policy`**: `PolicyEngine` enforces rules (like failing the build on critical vulnerabilities or EOL libraries).
*   **`report`**: Generators for HTML (`HtmlReportGenerator`), JSON (`JsonReportGenerator`), and Console parsing.
    *   **Interactive Dashboard**: The HTML report features real-time interaction through filtering cards (Total Scans, Critical, High, EOL). 
    *   **Vulnerability Linking**: Directly links CVEs and OSS Index IDs to official vulnerability databases (NVD, Sonatype, etc.).
*   **`visualization`**: Handles the D3.js interactive graph, including its built-in conflict detection logic and search panel.

## 2. APIs Used

### Sonatype OSS Index
*   **Endpoint**: `https://ossindex.sonatype.org/api/v3/component-report`
*   **Usage**: Checks packages (via PURL) against known vulnerabilities (CVEs).
*   **Note**: The public, anonymous API is rate-limited. In production, it's recommended to configure authentication credentials (username and API token) to handle larger enterprise projects.

### NVD API (Offline)
*   **Local Storage**: `~/.m2/dependency-health/nvd-cache/nvd.db`
*   **Usage**: The plugin primarily uses this **Offline Database** for scanning. It ensures scans are fast (under 60s) and reliable. 
*   **Syncing**: Use `mvn sync-nvd` to hydrate or update this database. The sync process uses a parallel fetch engine to saturate the NIST API limit (50 req/30s) while maintaining 100% crash-safety.

### endoflife.date API
*   **Endpoint**: `https://endoflife.date/api/{product}.json`
*   **Usage**: Checks if a specific technology/framework version (e.g., Spring Boot, Java) is currently End-of-Life (EOL) or actively supported.

### Maven Central API
*   **Endpoint**: `https://search.maven.org/solrsearch/select`
*   **Usage**: Maps identifiers to discover the Absolute Latest secure versions.
*   **Resilience**: Queries are parallelized (5 threads) with a 3-attempt retry mechanism and automatic connection leak prevention.

## 3. How to Use & Deploy

### Prerequisites
*   Maven 3.6+
*   Java 17 (See Java 21 section below)

### Building the Plugin locally
To install the plugin into your local `~/.m2` Maven repository so other projects can use it:
```bash
mvn clean install
```

### Adding to a target project
Add the plugin to the `pom.xml` of the project you want to scan:

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
                    <!-- Binds to the verify phase by default -->
                </execution>
            </executions>
            <configuration>
                <failOnCritical>true</failOnCritical>
                <failOnHighCount>5</failOnHighCount>
                <failOnEol>true</failOnEol>
                <skipPolicy>false</skipPolicy> <!-- Set to false to fail build on violations -->
                <!-- Optional: Use NVD API Key to avoid strict rate limits (5/min to 50/min) -->
                <!-- remove the below line if you don't have the API key -->
                <nvdApiKey>your-nvd-api-key</nvdApiKey> 
            </configuration>
        </plugin>
    </plugins>
</build>
```

Run the scan:
```bash
mvn verify
```
*Reports will be generated in configured project's `target/` directory.*

### Multi-Module (Parent/Child) Projects
If you are running a multi-module enterprise project, run the `aggregate` goal at the root directory:

```bash
mvn dependency-health:aggregate
```

This runs exactly **once** across the reactor, collecting dependencies from all modules. It includes:
- **Automatic NVD Sync**: Checks database health for the entire reactor.
- **Unified Risk Profile**: Aggregates vulnerabilities across all submodules.
- **Unified Reports**: Generates a single HTML, JSON, and Interactive Graph at the parent's `target/` directory.

## 4. How to Push to GitHub

1.  Initialize a Git repository (if not done already):
    ```bash
    git init
    ```
2.  Create a `.gitignore` file. Ensure `target/`, `.idea/`, `*.iml`, and `.vscode/` are excluded.
3.  Add the files:
    ```bash
    git add .
    ```
4.  Commit your changes:
    ```bash
    git commit -m "Initial commit of Dependency Health Maven Plugin"
    ```
5.  Create a new repository on GitHub.
6.  Link your local repo to GitHub:
    ```bash
    git branch -M main
    git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
    git push -u origin main
    ```

## 5. Compatibility with Java 21

**Yes, this plugin will work on Java 21.**

The plugin is currently compiled with `maven.compiler.source=17` and `maven.compiler.target=17` in its `pom.xml`. 
Java maintains strong backward compatibility. Code compiled for Java 17 runs perfectly fine on a Java 21 JVM.

If you specifically want the plugin to be *built* targeting Java 21 features (e.g., virtual threads, record patterns, switch expressions), you simply need to update the properties in the plugin's `pom.xml`:

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```
*Note: If you update it to Java 21, the developers using this plugin will also need to be running Java 21 or higher to execute their Maven builds.*

## 6. License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Copyright (c) 2026 Soyeb Shaikh.
