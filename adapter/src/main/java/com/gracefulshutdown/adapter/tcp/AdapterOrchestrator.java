package com.gracefulshutdown.adapter.tcp;

import com.gracefulshutdown.adapter.http.ProcrClient;
import com.gracefulshutdown.adapter.queue.ResponseCache;
import com.gracefulshutdown.adapter.queue.TransactionQueue;
import com.gracefulshutdown.adapter.queue.TransactionQueueImpl;
import com.gracefulshutdown.adapter.service.JsonYamlConverter;
import com.gracefulshutdown.adapter.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the adapter lifecycle:
 * - Manages TCP server for receiving connections
 * - Creates SenderThread for each connection (reads from Central)
 * - Manages pool of ProcessorThreads (send to PROCR, receive responses)
 * - Coordinates graceful shutdown
 */
public class AdapterOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(AdapterOrchestrator.class);
    
    private final int processorThreadCount;
    private final long responseCheckTtlMs;
    private final long gracefulShutdownTimeoutMs;
    
    private TcpServer tcpServer;
    private SenderThread senderThread;
    private List<ProcessorThread> processorThreads;
    private TransactionQueue transactionQueue;
    private ResponseCache responseCache;
    private SessionManager sessionManager;
    private ProcrClient procrClient;
    private JsonYamlConverter converter;
    private volatile boolean running = false;
    
    public AdapterOrchestrator(int processorThreadCount,
                               long responseCheckTtlMs,
                               long gracefulShutdownTimeoutMs,
                               TcpServer tcpServer,
                               ProcrClient procrClient,
                               JsonYamlConverter converter) {
        this.processorThreadCount = processorThreadCount;
        this.responseCheckTtlMs = responseCheckTtlMs;
        this.gracefulShutdownTimeoutMs = gracefulShutdownTimeoutMs;
        this.tcpServer = tcpServer;
        this.procrClient = procrClient;
        this.converter = converter;
        this.processorThreads = new ArrayList<>();
        
        logger.info("AdapterOrchestrator configured with {} processor threads", processorThreadCount);
    }
    
    /**
     * Start the adapter: initialize components and start all threads.
     */
    public void start() throws Exception {
        if (running) {
            logger.warn("Adapter already running");
            return;
        }
        
        logger.info("Starting Adapter Orchestrator");
        
        // Initialize components
        transactionQueue = new TransactionQueueImpl(100, 80);
        responseCache = new ResponseCache(responseCheckTtlMs);
        sessionManager = new SessionManager();
        
        // Start TCP server
        logger.info("Starting TCP Server");
        tcpServer.start();
        
        // Wait for client connection
        logger.info("Waiting for connection from Central...");
        Socket clientSocket = tcpServer.acceptConnection();
        
        if (clientSocket == null) {
            throw new RuntimeException("Failed to accept client connection");
        }
        
        logger.info("Accepted connection from Central: {}", clientSocket.getInetAddress());
        
        // Create and start SenderThread (reads from Central)
        TcpMessageSender messageSender = new TcpMessageSender(clientSocket);
        senderThread = new SenderThread(clientSocket, transactionQueue, sessionManager, converter);
        senderThread.start();
        logger.info("SenderThread started");
        
        // Create and start ProcessorThreads pool (send to PROCR)
        for (int i = 0; i < processorThreadCount; i++) {
            ProcessorThread processorThread = new ProcessorThread(
                i, 
                transactionQueue, 
                responseCache, 
                procrClient, 
                messageSender,
                converter
            );
            processorThread.start();
            processorThreads.add(processorThread);
        }
        logger.info("Started {} ProcessorThreads", processorThreadCount);
        
        // Start background task to evict expired responses
        startResponseEvictionTask();
        
        running = true;
        logger.info("Adapter Orchestrator started successfully");
    }
    
    /**
     * Start background task to periodically evict expired responses from cache.
     */
    private void startResponseEvictionTask() {
        Thread evictionThread = new Thread(() -> {
            logger.info("Response eviction task started (checking every 5 seconds)");
            while (running) {
                try {
                    Thread.sleep(5000);
                    responseCache.evictExpired();
                } catch (InterruptedException e) {
                    if (running) {
                        logger.warn("Response eviction task interrupted");
                    }
                    break;
                }
            }
            logger.info("Response eviction task stopped");
        }, "ResponseEvictionThread");
        evictionThread.setDaemon(true);
        evictionThread.start();
    }
    
    /**
     * Gracefully shutdown the adapter and all threads.
     */
    public void shutdown() {
        if (!running) {
            logger.info("Adapter already stopped");
            return;
        }
        
        logger.info("Initiating graceful shutdown of Adapter Orchestrator");
        running = false;
        
        // Update session state
        if (sessionManager != null && sessionManager.isSessionActive()) {
            sessionManager.shutdown();
        }
        
        // Stop SenderThread
        if (senderThread != null && senderThread.isRunning()) {
            logger.info("Stopping SenderThread");
            senderThread.shutdown();
        }
        
        // Stop all ProcessorThreads
        if (processorThreads != null) {
            for (ProcessorThread processorThread : processorThreads) {
                if (processorThread.isRunning()) {
                    logger.info("Stopping {}", processorThread.getName());
                    processorThread.shutdown();
                }
            }
        }
        
        // Wait for threads to complete
        waitForThreadCompletion();
        
        // Stop TCP server
        if (tcpServer != null) {
            try {
                logger.info("Stopping TCP Server");
                tcpServer.stop();
            } catch (Exception e) {
                logger.error("Error stopping TCP server", e);
            }
        }
        
        // Clear response cache
        if (responseCache != null) {
            responseCache.clear();
        }
        
        logger.info("Adapter Orchestrator shutdown complete");
    }
    
    /**
     * Wait for all threads to complete within timeout.
     */
    private void waitForThreadCompletion() {
        long startTime = System.currentTimeMillis();
        
        // Wait for SenderThread
        if (senderThread != null) {
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = gracefulShutdownTimeoutMs - elapsed;
                if (remaining > 0) {
                    senderThread.join(remaining);
                    if (senderThread.isAlive()) {
                        logger.warn("SenderThread did not complete within timeout");
                    } else {
                        logger.info("SenderThread completed");
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for SenderThread", e);
            }
        }
        
        // Wait for all ProcessorThreads
        if (processorThreads != null) {
            for (ProcessorThread processorThread : processorThreads) {
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = gracefulShutdownTimeoutMs - elapsed;
                    if (remaining > 0) {
                        processorThread.join(remaining);
                        if (processorThread.isAlive()) {
                            logger.warn("{} did not complete within timeout", processorThread.getName());
                        } else {
                            logger.info("{} completed", processorThread.getName());
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted waiting for {}", processorThread.getName(), e);
                }
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Thread shutdown completed in {} ms", totalTime);
    }
    
    /**
     * Check if orchestrator is running.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get session manager.
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    /**
     * Get transaction queue.
     */
    public TransactionQueue getTransactionQueue() {
        return transactionQueue;
    }
    
    /**
     * Get response cache.
     */
    public ResponseCache getResponseCache() {
        return responseCache;
    }
    
    /**
     * Get status information.
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("AdapterOrchestrator Status:\n");
        status.append("  Running: ").append(running).append("\n");
        status.append("  Sender: ").append(senderThread != null && senderThread.isRunning() ? "Active" : "Inactive").append("\n");
        status.append("  Processors: ").append(getActiveProcessorCount()).append("/").append(processorThreadCount).append("\n");
        status.append("  Queue Size: ").append(transactionQueue != null ? transactionQueue.size() : "N/A").append("\n");
        status.append("  Cache Size: ").append(responseCache != null ? responseCache.size() : "N/A").append("\n");
        status.append("  Session: ").append(sessionManager != null ? sessionManager.getState() : "N/A").append("\n");
        return status.toString();
    }
    
    /**
     * Get count of active processor threads.
     */
    private int getActiveProcessorCount() {
        if (processorThreads == null) {
            return 0;
        }
        int count = 0;
        for (ProcessorThread thread : processorThreads) {
            if (thread.isRunning()) {
                count++;
            }
        }
        return count;
    }
}
