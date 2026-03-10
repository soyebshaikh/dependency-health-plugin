package com.dependencyhealth.mojo;

import com.dependencyhealth.nvd.NvdApiSynchronizer;
import com.dependencyhealth.nvd.NvdDatabaseManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal to synchronize the local NVD SQLite database with the NIST API.
 */
@Mojo(name = "sync-nvd", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class NvdSyncMojo extends AbstractMojo {

    @Parameter(property = "nvdApiKey")
    private String nvdApiKey;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("Starting NVD Database Synchronization...");
        getLog().info("------------------------------------------------------------------------");

        try {
            NvdDatabaseManager dbManager = new NvdDatabaseManager();
            NvdApiSynchronizer synchronizer = new NvdApiSynchronizer(dbManager, nvdApiKey);
            
            // Note: Since we are in a Mojo, we can't easily handle hours of sync if the connection is slow,
            // but for incremental syncs this is perfect. 
            // For the first run, the user might need to leave it running.
            
            synchronizer.sync();
            
            getLog().info("NVD Synchronization completed successfully.");
        } catch (Exception e) {
            getLog().error("NVD Synchronization failed", e);
            throw new MojoExecutionException("Failed to sync NVD data: " + e.getMessage(), e);
        }
    }
}
