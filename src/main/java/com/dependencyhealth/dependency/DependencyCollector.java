package com.dependencyhealth.dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.Collections;
import java.util.Set;

public class DependencyCollector {

    private final MavenProject project;
    private final MavenSession session;
    private final DependencyGraphBuilder graphBuilder;

    public DependencyCollector(MavenProject project, MavenSession session, DependencyGraphBuilder graphBuilder) {
        this.project = project;
        this.session = session;
        this.graphBuilder = graphBuilder;
    }

    /**
     * Retrieves all resolved artifacts (direct + transitive).
     * Uses a recursive tree traversal to ensure 100% coverage.
     * @return Set of Artifacts
     */
    public Set<Artifact> getAllDependencies() {
        try {
            DependencyNode root = buildDependencyGraph();
            java.util.Set<Artifact> result = new java.util.HashSet<>();
            collectRecursive(root, result);
            return result;
        } catch (Exception e) {
            // Fallback to basic artifacts if graph building fails
            Set<Artifact> artifacts = project.getArtifacts();
            return artifacts != null ? artifacts : Collections.emptySet();
        }
    }

    private void collectRecursive(DependencyNode node, Set<Artifact> result) {
        if (node.getArtifact() != null) {
            result.add(node.getArtifact());
        }
        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                collectRecursive(child, result);
            }
        }
    }

    /**
     * Builds a full dependency graph for the project.
     * 
     * @return The root node of the dependency graph
     * @throws DependencyGraphBuilderException if graph generation fails
     */
    public DependencyNode buildDependencyGraph() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);

        return graphBuilder.buildDependencyGraph(buildingRequest, null);
    }
}
