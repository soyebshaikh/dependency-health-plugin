package com.dependencyhealth.sbom;

import org.apache.maven.artifact.Artifact;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class SbomGenerator {

    /**
     * Generates a CycloneDX SBOM from the given project and dependencies.
     *
     * @param projectGroupId    The group ID of the root project
     * @param projectArtifactId The artifact ID of the root project
     * @param projectVersion    The version of the root project
     * @param dependencies      The resolved artifacts (direct + transitive)
     * @param outputFile        The file to save the target SBOM JSON
     * @throws IOException If saving fails
     */
    public void generateSbom(String projectGroupId, String projectArtifactId, String projectVersion,
                             Set<Artifact> dependencies, File outputFile) throws IOException {

        Bom bom = new Bom();
        
        // Setup Metadata
        Metadata metadata = new Metadata();
        metadata.setTimestamp(new Date());

        Tool tool = new Tool();
        tool.setVendor("DependencyHealth");
        tool.setName("DependencyHealthMavenPlugin");
        tool.setVersion("1.0.0");
        metadata.setTools(Collections.singletonList(tool));
        
        Component rootComponent = createComponent(projectGroupId, projectArtifactId, projectVersion, "library");
        metadata.setComponent(rootComponent);
        bom.setMetadata(metadata);

        // Add Dependencies as Components
        if (dependencies != null) {
            for (Artifact artifact : dependencies) {
                Component component = createComponent(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        null);
                bom.addComponent(component);
            }
        }

        // Generate JSON
        BomJsonGenerator bomGenerator = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_14, bom);
        String bomString = bomGenerator.toJsonString();

        // Write to file
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(bomString);
        }
    }

    private Component createComponent(String groupId, String artifactId, String version, String type) {
        Component component = new Component();
        component.setGroup(groupId);
        component.setName(artifactId);
        component.setVersion(version);
        if (type != null) {
            component.setType(Component.Type.valueOf(type.toUpperCase()));
        } else {
            component.setType(Component.Type.LIBRARY);
        }

        // Generate Package URL (purl)
        // Format: pkg:maven/{groupId}/{artifactId}@{version}
        String purl = String.format("pkg:maven/%s/%s@%s", groupId, artifactId, version);
        component.setPurl(purl);
        component.setBomRef(purl);

        return component;
    }
}
