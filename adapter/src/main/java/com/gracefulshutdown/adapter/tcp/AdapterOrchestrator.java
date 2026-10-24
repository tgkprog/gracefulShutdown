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
 * - Connects to Central Server (TCP client)
 * - Creates SenderThread for reading transactions from Central
 * - Manages pool of ProcessorThreads (send to PROCR, receive responses)
 * - Coordinates graceful shutdown
 */
public class AdapterOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(AdapterOrchestrator.class);
    private static final long MANDATORY_SHUTDOWN_WAIT_MS = 11000L;
    
    private final int processorThreadCount;
    private final long responseCheckTtlMs;
    private final long gracefulShutdownTimeoutMs;
    
    private TcpClientConnector tcpClientConnector;
    private SenderThread senderThread;
    private List<ProcessorThread> processorThreads;
    private TransactionQueue transactionQueue;
    private ResponseCache responseCache;
    private SessionManager sessionManager;
    private ProcrClient procrClient;
    private JsonYamlConverter converter;
    private volatile boolean running = false;
    private Socket centralSocket;
    private volatile boolean procrHealthy = false;
    private volatile boolean sessionStarted = false;
    private TcpMessageSender messageSender;
    
    public AdapterOrchestrator(int processorThreadCount,
                               long responseCheckTtlMs,
                               long gracefulShutdownTimeoutMs,
                               TcpClientConnector tcpClientConnector,
                               ProcrClient procrClient,
                               JsonYamlConverter converter) {
        this.processorThreadCount = processorThreadCount;
        this.responseCheckTtlMs = responseCheckTtlMs;
        this.gracefulShutdownTimeoutMs = gracefulShutdownTimeoutMs;
        this.tcpClientConnector = tcpClientConnector;
        this.procrClient = procrClient;
        this.converter = converter;
        this.processorThreads = new ArrayList<>();
        
        logger.info("AdapterOrchestrator configured with {} processor threads", processorThreadCount);
    }
    
    /**
     * Start the adapter: connect to Central and initialize components.
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
        
        // Connect to Central Server (auto-retry enabled)
        logger.info("🔌 Connecting to Central Server at {}:{}", 
            tcpClientConnector.getCentralHost(), tcpClientConnector.getCentralPort());
        centralSocket = tcpClientConnector.connectToCentral();
        
        logger.info("✅ Connected to Central Server: {}", centralSocket.getInetAddress());
        
        // Create TcpMessageSender for sending messages to Central
        messageSender = new TcpMessageSender(centralSocket);
        
        // Create and start SenderThread (reads from Central)
        senderThread = new SenderThread(centralSocket, transactionQueue, sessionManager, converter);
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
                converter,
                sessionManager
            );
            processorThread.start();
            processorThreads.add(processorThread);
        }
        logger.info("Started {} ProcessorThreads", processorThreadCount);
        
        // Start background task to evict expired responses
        startResponseEvictionTask();
        
        // Start PROCR health check and session auto-start
        startProcrHealthCheckTask();
        
        running = true;
        logger.info("✅ Adapter Orchestrator started successfully");
    }
    
    /**
     * Start background task to monitor PROCR health and auto-start session when available.
     * Checks every 10 seconds for PROCR availability.
     */
    private void startProcrHealthCheckTask() {
        Thread healthCheckThread = new Thread(() -> {
            logger.info("PROCR health check task started (checking every 10 seconds)");
            while (running) {
                try {
                    boolean currentHealth = procrClient.isProcrHealthy();
                    
                    // Transition: PROCR became healthy and session not yet started
                    if (currentHealth && !procrHealthy && !sessionStarted) {
                        logger.info("✅ PROCR became available - auto-starting session");
                        procrHealthy = true;
                        
                        try {
                            // Send SESSION_START message to Central
                            messageSender.sendSessionStart();
                            // Also start session locally in Adapter
                            sessionManager.startSession();
                            sessionStarted = true;
                            logger.info("✅ SESSION_START sent to Central and session started locally");
                        } catch (Exception e) {
                            logger.error("❌ Error sending SESSION_START to Central", e);
                            sessionStarted = false;
                        }
                    }
                    // Transition: PROCR became unhealthy
                    else if (!currentHealth && procrHealthy) {
                        logger.warn("⚠️ PROCR became unavailable");
                        procrHealthy = false;
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    if (running) {
                        logger.debug("PROCR health check interrupted");
                    }
                    break;
                } catch (Exception e) {
                    logger.error("Error in PROCR health check task", e);
                }
            }
            logger.info("PROCR health check task stopped");
        }, "ProcrHealthCheckThread");
        healthCheckThread.setDaemon(false);
        healthCheckThread.start();
    }
    
    /**
     * Start background task to periodically evict expired responses from cache.
     */
    private void startResponseEvictionTask() {
        Thread evictionThread = new Thread(() -> {
            logger.info("Response eviction task started (checking every 5 seconds)");
            while (running) {
                try {
                    Thread.sleep(1300);
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
     * Gracefully shutdown the adapter with proper cache draining.
     * 
     * Steps:
     * 1. Notify Central of SESSION_END in background thread (non-blocking)
     * 2. Main thread polls ResponseCache every 150ms until empty
     * 3. Stop all processing threads
     * 4. Close connections
     */
    public void shutdown() {
        if (!running) {
            logger.info("Adapter already stopped");
            return;
        }
        
        logger.info("Initiating graceful shutdown of Adapter Orchestrator");
        running = false;

        Thread sessionEndThread = sessionEndTask();

        shutdownSteps2to5(sessionEndThread);

        // Wait for threads to complete
        waitForThreadCompletion();
        
        // ===============================================
        // Step 6: Close connections and cleanup
        // ===============================================
        // Close Central socket
        if (centralSocket != null && !centralSocket.isClosed()) {
            try {
                logger.info("Closing connection to Central Server");
                centralSocket.close();
            } catch (Exception e) {
                logger.error("Error closing Central socket", e);
            }
        }
        
        // Clear response cache
        if (responseCache != null) {
            responseCache.clear();
        }
        
        logger.info("✅ Adapter Orchestrator shutdown complete");
    }

    private void shutdownSteps2to5(Thread sessionEndThread) {
        // ===============================================
        // Step 2: Mandatory wait to allow final response propagation
        // ===============================================
        logger.info("⏳ Mandatory shutdown wait: {} ms", MANDATORY_SHUTDOWN_WAIT_MS);
        try {
            Thread.sleep(MANDATORY_SHUTDOWN_WAIT_MS);
        } catch (InterruptedException e) {
            logger.warn("Mandatory shutdown wait interrupted", e);
            Thread.currentThread().interrupt();
        }

        // ===============================================
        // Step 3: Wait for pending work to drain
        // ===============================================
        logger.info("⏳ Starting pending-work drain monitoring (polling every 150ms)");
        drainResponseCache();

        // ===============================================
        // Step 4: Wait for SESSION_END send thread completion (concurrent with drain above)
        // ===============================================
        if (sessionEndThread != null) {
            try {
                logger.info("⏳ Waiting for SESSION_END thread completion...");
                sessionEndThread.join(10000);  // timeout
                if (sessionEndThread.isAlive()) {
                    logger.warn("⚠️  SessionEndNotifier thread did not complete within 10 seconds");
                } else {
                    logger.info("✅ SESSION_END thread completed");
                }
            } catch (InterruptedException e) {
                logger.error("❌ Interrupted while waiting for SESSION_END thread", e);
                Thread.currentThread().interrupt();
            }
        }

        // ===============================================
        // Step 5: Stop intake and processing threads
        // ===============================================
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
    }

    private Thread sessionEndTask() {
        Thread sessionEndThread = null;

        // ===============================================
        // Step 1: Send SESSION_END to Central (background)
        // ===============================================
        if (sessionManager != null && sessionManager.isSessionActive() && messageSender != null) {
            logger.info("🔔 Sending SESSION_END to Central");
            sessionEndThread = new Thread(new SessionEndNotifier(messageSender), "SessionEndNotifier");
            sessionEndThread.setDaemon(false);
            sessionEndThread.start();
        }
        return sessionEndThread;
    }

    /**
     * Monitor and wait for all pending work to drain completely.
     * Checks queue depth, in-flight processor work, and cache depth.
     * Polls every 150ms.
     */
    private void drainResponseCache() {
        while (true) {
            int queuePending = transactionQueue != null ? transactionQueue.size() : 0;
            int cachePending = responseCache != null ? responseCache.getPendingCount() : 0;
            int processingPending = getBusyProcessorCount();
            int totalPending = queuePending + cachePending + processingPending;

            if (totalPending == 0) {
                logger.info("✅ Pending work drained - queue=0, processing=0, cache=0");
                break;
            }

            logger.info("⏳ Waiting for pending work: total={}, queue={}, processing={}, cache={}",
                totalPending, queuePending, processingPending, cachePending);
            
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                logger.warn("Cache drain monitoring interrupted");
                break;
            }
        }
    }
    
    /**
     * Inner class: SessionEndNotifier sends SESSION_END message to Central.
     * Runs in background thread to avoid blocking cache drain monitoring.
     */
    private static class SessionEndNotifier implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(SessionEndNotifier.class);
        private final TcpMessageSender messageSender;
        
        public SessionEndNotifier(TcpMessageSender messageSender) {
            this.messageSender = messageSender;
        }
        
        @Override
        public void run() {
            try {
                logger.info("📤 SessionEndNotifier: Sending SESSION_END message to Central");
                messageSender.sendSessionEnd();
                logger.info("✅ SessionEndNotifier: SESSION_END sent successfully");
            } catch (Exception e) {
                logger.error("❌ SessionEndNotifier: Failed to send SESSION_END to Central", e);
            }
        }
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
        status.append("  Central Connection: ").append(centralSocket != null && !centralSocket.isClosed() ? "Connected" : "Disconnected").append("\n");
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

    /**
     * Get count of processor threads actively processing a transaction.
     */
    private int getBusyProcessorCount() {
        if (processorThreads == null) {
            return 0;
        }
        int count = 0;
        for (ProcessorThread thread : processorThreads) {
            if (thread.isProcessingTxn()) {
                count++;
            }
        }
        return count;
    }
}
