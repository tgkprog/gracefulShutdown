package com.gracefulshutdown.adapter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.SpringApplication;

/**
 * ShutdownHookManager handles graceful shutdown on SIGTERM and SIGINT signals.
 * Ensures clean exit of all threads and resources.
 */
@Component
public class ShutdownHookManager {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownHookManager.class);
    
    private final AdapterService adapterService;
    private final ApplicationContext applicationContext;
    
    public ShutdownHookManager(AdapterService adapterService, ApplicationContext applicationContext) {
        this.adapterService = adapterService;
        this.applicationContext = applicationContext;
        registerShutdownHook();
        logger.info("ShutdownHookManager initialized");
    }
    
    /**
     * Register JVM shutdown hook for graceful exit.
     * Catches SIGTERM (kill -15) and SIGINT (Ctrl+C).
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("======================================");
            logger.info("Received shutdown signal (SIGTERM/SIGINT)");
            logger.info("======================================");
            
            try {
                // Gracefully shutdown the adapter
                if (adapterService.isRunning()) {
                    logger.info("Initiating graceful shutdown of adapter services...");
                    adapterService.shutdown();
                    logger.info("Adapter shutdown complete");
                }
                
                logger.info("All services shut down successfully");
                logger.info("Adapter process exiting cleanly");
                
            } catch (Exception e) {
                logger.error("Error during graceful shutdown: {}", e.getMessage(), e);
                System.exit(1);
            }
        }, "ShutdownHook"));
    }
}
