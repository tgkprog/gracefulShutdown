package com.gracefulshutdown.adapter.tcp;

import com.gracefulshutdown.adapter.domain.Response;
import com.gracefulshutdown.adapter.domain.Transaction;
import com.gracefulshutdown.adapter.http.ProcrClient;
import com.gracefulshutdown.adapter.queue.ResponseCache;
import com.gracefulshutdown.adapter.queue.TransactionQueue;
import com.gracefulshutdown.adapter.service.JsonYamlConverter;
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
    private volatile boolean running = true;
    private int threadId;
    
    public ProcessorThread(int threadId,
                          TransactionQueue transactionQueue,
                          ResponseCache responseCache,
                          ProcrClient procrClient,
                          TcpMessageSender messageSender,
                          JsonYamlConverter converter) {
        super("ProcessorThread-" + threadId);
        this.threadId = threadId;
        this.transactionQueue = transactionQueue;
        this.responseCache = responseCache;
        this.procrClient = procrClient;
        this.messageSender = messageSender;
        this.converter = converter;
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
                
                logger.info("ProcessorThread-{}: Processing txn {} from client {}", 
                    threadId, txn.getId(), txn.getClientId());
                
                // Process transaction
                processTransaction(txn);
                
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
            // Step 1: Convert transaction to YAML
            String yamlRequest = converter.jsonToYaml(txn.toJson());
            logger.debug("ProcessorThread-{}: Converted txn {} to YAML (length: {})", 
                threadId, txn.getId(), yamlRequest.length());
            
            // Step 2: Send to PROCR and get response (with retry logic & circuit breaker)
            logger.info("ProcessorThread-{}: Sending txn {} to PROCR", threadId, txn.getId());
            Response response = procrClient.sendTransaction(yamlRequest, txn.getId());
            
            if (response == null) {
                logger.error("ProcessorThread-{}: No response from PROCR for txn {}", threadId, txn.getId());
                sendErrorResponse(txn.getId(), "No response from PROCR");
                return;
            }
            
            logger.info("ProcessorThread-{}: Received response for txn {}: status={}", 
                threadId, txn.getId(), response.getStatus());
            
            // Step 3: Cache the response
            responseCache.put(txn.getId(), response);
            
            // Step 4: Convert response from YAML to JSON
            String jsonResponse = converter.yamlToJson(response.toYaml());
            logger.debug("ProcessorThread-{}: Converted response {} to JSON (length: {})", 
                threadId, txn.getId(), jsonResponse.length());
            
            // Step 5: Send JSON response back to Central via TCP
            messageSender.sendJson(jsonResponse);
            logger.info("ProcessorThread-{}: Sent response for txn {} back to Central", 
                threadId, txn.getId());
            
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
     * Get thread ID.
     */
    public int getThreadId() {
        return threadId;
    }
}
