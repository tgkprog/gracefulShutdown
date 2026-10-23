package com.gracefulshutdown.adapter.service;

import com.gracefulshutdown.adapter.http.ProcrClient;
import com.gracefulshutdown.adapter.tcp.AdapterOrchestrator;
import com.gracefulshutdown.adapter.tcp.TcpClientConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * AdapterService manages the lifecycle of the adapter and orchestrator.
 * Connects to Central on startup and handles graceful shutdown.
 */
@Service
public class AdapterService {
    private static final Logger logger = LoggerFactory.getLogger(AdapterService.class);
    
    private final TcpClientConnector tcpClientConnector;
    private final ProcrClient procrClient;
    private final JsonYamlConverter converter;
    
    @Value("${adapter.processor.thread_count:2}")
    private int processorThreadCount;
    
    @Value("${adapter.cache.ttl_ms:30000}")
    private long responseCacheTtlMs;
    
    @Value("${adapter.shutdown.timeout_ms:15000}")
    private long gracefulShutdownTimeoutMs;
    
    private AdapterOrchestrator orchestrator;
    
    public AdapterService(TcpClientConnector tcpClientConnector,
                         ProcrClient procrClient,
                         JsonYamlConverter converter) {
        this.tcpClientConnector = tcpClientConnector;
        this.procrClient = procrClient;
        this.converter = converter;
        logger.info("AdapterService instantiated");
    }
    
    /**
     * Initialize adapter after Spring context is ready.
     * Connects to Central and starts orchestrator with auto-retry.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing AdapterService");
        
        try {
            // Verify PROCR is healthy
            logger.info("Checking PROCR health...");
            if (!procrClient.isProcrHealthy()) {
                logger.warn("WARNING: PROCR is not responding. Adapter will still start but may fail when processing.");
            } else {
                logger.info("✅ PROCR is healthy and ready");
            }
            
            // Create orchestrator with TCP client connector
            orchestrator = new AdapterOrchestrator(
                processorThreadCount,
                responseCacheTtlMs,
                gracefulShutdownTimeoutMs,
                tcpClientConnector,
                procrClient,
                converter
            );
            
            // Start orchestrator (this will connect to Central and start threads)
            orchestrator.start();
            
            logger.info("✅ AdapterService initialized successfully");
            logger.info("Configuration: processors={}, cacheTTL={}ms, shutdownTimeout={}ms",
                processorThreadCount, responseCacheTtlMs, gracefulShutdownTimeoutMs);
            
        } catch (Exception e) {
            logger.error("❌ Error initializing AdapterService", e);
            throw new RuntimeException("Failed to initialize adapter", e);
        }
    }
    
    /**
     * Gracefully shutdown adapter when Spring context is closing.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down AdapterService");
        
        if (orchestrator != null && orchestrator.isRunning()) {
            try {
                orchestrator.shutdown();
                logger.info("Orchestrator shutdown complete");
            } catch (Exception e) {
                logger.error("Error shutting down orchestrator", e);
            }
        }
        
        logger.info("AdapterService shutdown complete");
    }
    
    /**
     * Get orchestrator status.
     */
    public String getStatus() {
        if (orchestrator == null) {
            return "Orchestrator not initialized";
        }
        return orchestrator.getStatus();
    }
    
    /**
     * Check if adapter is running.
     */
    public boolean isRunning() {
        return orchestrator != null && orchestrator.isRunning();
    }
    
    /**
     * Get session manager from orchestrator.
     */
    public SessionManager getSessionManager() {
        if (orchestrator == null) {
            return null;
        }
        return orchestrator.getSessionManager();
    }
}
