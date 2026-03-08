package com.dependencyhealth.lifecycle;

import org.apache.maven.artifact.Artifact;

import java.util.Map;
import java.util.Set;

/**
 * Interface for End-of-Life and Lifecycle intelligence clients.
 */
public interface LifecycleClient {

    /**
     * Checks a set of dependencies for EOL status.
     *
     * @param dependencies The dependencies to scan.
     * @return A map of dependency coordinates to LifecycleData.
     */
    Map<String, LifecycleData> checkLifecycle(Set<Artifact> dependencies);
}
