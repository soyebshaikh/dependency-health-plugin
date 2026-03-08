# Dependency Health Maven Plugin - Architecture & Usage Guide

## 1. Project Architecture
The project follows a clean, modular architecture separating concerns into specific packages. It hooks into the Maven `verify` lifecycle phase to perform its analysis.

*   **`mojo`**: Contains `DependencyHealthScanMojo`, the main entry point (the Maven Plugin goal). Orchestrates the entire scan process.
*   **`dependency`**: Handles interaction with Maven APIs (`MavenProject`, `DependencyGraphBuilder`) to resolve direct and transitive dependencies.
*   **`sbom`**: Generates a standard CycloneDX JSON Software Bill of Materials.
*   **`vulnerability`**: Interfaces (`VulnerabilityClient`) and concrete implementations (`OssIndexClient`, `NvdClient` skeleton) for querying CVE databases.
*   **`lifecycle`**: Interfaces (`LifecycleClient`) and concrete implementations (`EolClient`) for checking End-of-Life status.
*   **`risk`**: The `RiskAnalysisService` merges vulnerability and lifecycle data into a `DependencyRiskProfile`, calculating a unified risk score.
*   **`policy`**: `PolicyEngine` enforces rules (like failing the build on critical vulnerabilities or EOL libraries).
*   **`report`**: Generators for HTML (`HtmlReportGenerator`), JSON (`JsonReportGenerator`), and Console parsing.

## 2. APIs Used

### Sonatype OSS Index
*   **Endpoint**: `https://ossindex.sonatype.org/api/v3/component-report`
*   **Usage**: Checks packages (via PURL) against known vulnerabilities (CVEs).
*   **Note**: The public, anonymous API is rate-limited. In production, it's recommended to configure authentication credentials (username and API token) to handle larger enterprise projects.

### endoflife.date API
*   **Endpoint**: `https://endoflife.date/api/{product}.json`
*   **Usage**: Checks if a specific technology/framework version (e.g., Spring Boot, Java) is currently End-of-Life (EOL) or actively supported.

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
                <failOnEol>false</failOnEol>
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
