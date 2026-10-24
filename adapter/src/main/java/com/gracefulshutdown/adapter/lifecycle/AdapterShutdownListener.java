package com.gracefulshutdown.adapter.lifecycle;

import com.gracefulshutdown.adapter.service.AdapterService;
import com.gracefulshutdown.adapter.tcp.AdapterOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Implements Spring's SmartLifecycle to manage adapter shutdown.
 * 
 * This provides fine-grained control over the shutdown phase within
 * Spring's smart shutdown lifecycle, ensuring the adapter shuts down
 * at the appropriate time during application shutdown.
 */
@Component
public class AdapterShutdownListener implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(AdapterShutdownListener.class);
    
    private final AdapterService adapterService;
    private volatile boolean running = false;
    
    public AdapterShutdownListener(AdapterService adapterService) {
        this.adapterService = adapterService;
    }
    
    @Override
    public void start() {
        logger.info("AdapterShutdownListener started");
        running = true;
    }
    
    @Override
    public void stop() {
        logger.info("🔴 Spring smart lifecycle stop event - performing adapter pre-shutdown");
        performPreShutdown();
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPhase() {
        // Higher phase values are stopped FIRST during shutdown
        // Return Integer.MAX_VALUE to ensure adapter shuts down early
        return Integer.MAX_VALUE;
    }
    
    /**
     * Perform pre-shutdown operations on the adapter orchestrator.
     * Contains all pre-shutdown logic that runs in the run method.
     */
    private void performPreShutdown() {
        try {
            AdapterOrchestrator orchestrator = adapterService.getOrchestrator();
            
            if (orchestrator != null && orchestrator.isRunning()) {
                logger.info("Performing adapter pre-shutdown operations");
                orchestrator.shutdown();
                logger.info("Orchestrator shutdown complete");
            }
            
            logger.info("✅ Adapter pre-shutdown completed successfully");
        } catch (Exception e) {
            logger.error("❌ Error during adapter pre-shutdown", e);
            // Continue shutdown even if pre-shutdown fails
        }
    }
}

