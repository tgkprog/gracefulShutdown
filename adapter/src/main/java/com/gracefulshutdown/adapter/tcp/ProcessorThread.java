package com.gracefulshutdown.adapter.tcp;

import com.gracefulshutdown.adapter.domain.Response;
import com.gracefulshutdown.adapter.domain.Transaction;
import com.gracefulshutdown.adapter.http.ProcrClient;
import com.gracefulshutdown.adapter.queue.ResponseCache;
import com.gracefulshutdown.adapter.queue.TransactionQueue;
import com.gracefulshutdown.adapter.service.JsonYamlConverter;
import com.gracefulshutdown.adapter.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Thread that processes transactions by:
 * 1. Dequeuing transaction from queue
 * 2. Converting JSON to YAML
 * 3. Sending to PROCR via HTTP
 * 4. Receiving YAML response from PROCR
 * 5. Converting YAML to JSON
 * 6. Sending JSON response back to Central via TCP
 */
public class ProcessorThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorThread.class);
    
    private final TransactionQueue transactionQueue;
    private final ResponseCache responseCache;
    private final ProcrClient procrClient;
    private final TcpMessageSender messageSender;
    private final JsonYamlConverter converter;
    private final SessionManager sessionManager;
    private volatile boolean running = true;
    private volatile boolean processingTxn = false;
    private volatile boolean processingStarted = false;
    private int threadId;
    
    public ProcessorThread(int threadId,
                          TransactionQueue transactionQueue,
                          ResponseCache responseCache,
                          ProcrClient procrClient,
                          TcpMessageSender messageSender,
                          JsonYamlConverter converter,
                          SessionManager sessionManager) {
        super("ProcessorThread-" + threadId);
        this.threadId = threadId;
        this.transactionQueue = transactionQueue;
        this.responseCache = responseCache;
        this.procrClient = procrClient;
        this.messageSender = messageSender;
        this.converter = converter;
        this.sessionManager = sessionManager;
        setDaemon(false);
    }
    
    @Override
    public void run() {
        logger.info("ProcessorThread-{} started", threadId);
        
        while (running) {
            try {
                // Dequeue transaction (blocks if queue is empty)
                Transaction txn = transactionQueue.dequeue();
                
                if (txn == null) {
                    // Thread interrupted or queue closed
                    break;
                }
                
                // On first transaction, transition SESSION_STARTED → PROCESSING
                if (!processingStarted) {
                    if (sessionManager.beginProcessing()) {
                        processingStarted = true;
                        logger.info("ProcessorThread-{}: State transition: SESSION_STARTED → PROCESSING", threadId);
                    } else {
                        logger.warn("ProcessorThread-{}: Failed to transition to PROCESSING state", threadId);
                    }
                }
                
                logger.info("ProcessorThread-{}: Processing txn {} from client {}", 
                    threadId, txn.getId(), txn.getClientId());
                
                // Process transaction
                processingTxn = true;
                try {
                    processTransaction(txn);
                } finally {
                    processingTxn = false;
                }
                
            } catch (InterruptedException e) {
                if (running) {
                    logger.warn("ProcessorThread-{}: Interrupted while waiting for transaction", threadId);
                } else {
                    logger.info("ProcessorThread-{}: Interrupted during shutdown", threadId);
                }
                break;
            } catch (Exception e) {
                logger.error("ProcessorThread-{}: Error processing transaction", threadId, e);
            }
        }
        
        logger.info("ProcessorThread-{} stopped", threadId);
    }
    
    /**
     * Process single transaction through PROCR.
     */
    private void processTransaction(Transaction txn) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Get transaction as JSON (PROCR expects JSON, not YAML)
            String jsonRequest = txn.toJson();
            logger.debug("ProcessorThread-{}: Prepared txn {} as JSON (length: {})", 
                threadId, txn.getId(), jsonRequest.length());
            
            // Step 2: Send to PROCR and get response (with retry logic & circuit breaker)
            logger.info("ProcessorThread-{}: Sending txn {} to PROCR", threadId, txn.getId());
            Response response = procrClient.sendTransaction(jsonRequest, txn.getId());
            
            if (response == null) {
                logger.error("ProcessorThread-{}: No response from PROCR for txn {}", threadId, txn.getId());
                sendErrorResponse(txn.getId(), "No response from PROCR");
                return;
            }
            
            logger.info("ProcessorThread-{}: Received response for txn {}: status={}", 
                threadId, txn.getId(), response.getStatus());
            
            // Step 3: Cache the response
            responseCache.put(txn.getId(), response);
            
            // Step 4: Send JSON response back to Central via TCP
            String jsonResponse = response.toJson();
            logger.debug("ProcessorThread-{}: Prepared response {} as JSON (length: {})", 
                threadId, txn.getId(), jsonResponse.length());
            
            // Step 5: Send JSON response back to Central via TCP
            messageSender.sendJson(jsonResponse);
            logger.info("ProcessorThread-{}: Sent response for txn {} back to Central", 
                threadId, txn.getId());

            // Step 6: Remove from cache after successful delivery to Central
            responseCache.remove(txn.getId());
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("ProcessorThread-{}: Completed txn {} in {} ms", 
                threadId, txn.getId(), duration);
            
        } catch (Exception e) {
            logger.error("ProcessorThread-{}: Error processing txn {}", threadId, txn.getId(), e);
            try {
                sendErrorResponse(txn.getId(), e.getMessage());
            } catch (IOException ioe) {
                logger.error("ProcessorThread-{}: Failed to send error response", threadId, ioe);
            }
        }
    }
    
    /**
     * Send error response back to Central.
     */
    private void sendErrorResponse(String transactionId, String errorMsg) throws IOException {
        String errorJson = "{\"transaction_id\": \"" + transactionId + "\", " +
            "\"status\": \"REJECTED\", " +
            "\"reason\": \"" + errorMsg + "\"}";
        messageSender.sendJson(errorJson);
        logger.warn("ProcessorThread-{}: Sent error response for txn {}", threadId, transactionId);
    }
    
    /**
     * Signal thread to stop gracefully.
     */
    public void shutdown() {
        logger.info("ProcessorThread-{} shutdown requested", threadId);
        running = false;
        // Interrupt if waiting on queue
        this.interrupt();
    }
    
    /**
     * Check if thread is running.
     */
    public boolean isRunning() {
        return running && this.isAlive();
    }

    /**
     * Check if thread is actively processing a transaction.
     */
    public boolean isProcessingTxn() {
        return processingTxn;
    }
    
    /**
     * Get thread ID.
     */
    public int getThreadId() {
        return threadId;
    }
}
