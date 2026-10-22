package com.gracefulshutdown.adapter.tcp;

import com.gracefulshutdown.adapter.domain.SessionMessage;
import com.gracefulshutdown.adapter.domain.Transaction;
import com.gracefulshutdown.adapter.service.JsonYamlConverter;
import com.gracefulshutdown.adapter.service.SessionManager;
import com.gracefulshutdown.adapter.queue.TransactionQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

/**
 * Thread that reads JSON messages from TCP socket and enqueues transactions.
 * Handles SESSION_START, SESSION_END, and Transaction messages.
 * Updates adapter state based on message types.
 */
public class SenderThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SenderThread.class);
    
    private final Socket clientSocket;
    private final TransactionQueue transactionQueue;
    private final SessionManager sessionManager;
    private final JsonYamlConverter converter;
    private final TcpMessageSender messageSender;
    private volatile boolean running = true;
    
    public SenderThread(Socket clientSocket, 
                        TransactionQueue transactionQueue,
                        SessionManager sessionManager,
                        JsonYamlConverter converter) {
        super("SenderThread");
        this.clientSocket = clientSocket;
        this.transactionQueue = transactionQueue;
        this.sessionManager = sessionManager;
        this.converter = converter;
        try {
            this.messageSender = new TcpMessageSender(clientSocket);
        } catch (IOException e) {
            logger.error("Error creating TcpMessageSender", e);
            throw new RuntimeException(e);
        }
        setDaemon(false);
    }
    
    @Override
    public void run() {
        logger.info("SenderThread started. Listening for messages from Central");
        ObjectMapper mapper = new ObjectMapper();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    handleMessage(line.trim(), mapper);
                } catch (Exception e) {
                    logger.error("Error processing message: {}", line, e);
                    // Send error acknowledgment
                    try {
                        messageSender.sendJson("{\"error\": \"" + e.getMessage() + "\"}");
                    } catch (IOException ioe) {
                        logger.error("Failed to send error response", ioe);
                    }
                }
            }
            
            logger.info("SenderThread: Connection closed by client");
            
        } catch (IOException e) {
            if (running) {
                logger.error("SenderThread: Error reading from socket", e);
            } else {
                logger.info("SenderThread: Gracefully shut down");
            }
        } finally {
            running = false;
            try {
                clientSocket.close();
                logger.info("SenderThread: Socket closed");
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }
    }
    
    /**
     * Process incoming message based on type.
     */
    private void handleMessage(String jsonMessage, ObjectMapper mapper) throws Exception {
        logger.debug("Received message: {}", jsonMessage);
        
        // Parse JSON to determine message type
        var jsonNode = mapper.readTree(jsonMessage);
        String messageType = jsonNode.get("message_type") != null ? 
            jsonNode.get("message_type").asText() : null;
        
        if ("SESSION_START".equals(messageType)) {
            handleSessionStart();
        } else if ("SESSION_END".equals(messageType)) {
            handleSessionEnd();
        } else if ("TRANSACTION".equals(messageType) || jsonNode.has("transaction_id")) {
            handleTransaction(jsonMessage);
        } else {
            logger.warn("Unknown message type: {}", messageType);
            messageSender.sendJson("{\"error\": \"Unknown message type\"}");
        }
    }
    
    /**
     * Handle SESSION_START message.
     */
    private void handleSessionStart() throws IOException {
        logger.info("Received SESSION_START from Central");
        
        boolean success = sessionManager.startSession();
        if (success) {
            String ackJson = "{\"ack_status\": \"OK\", \"message\": \"Session started\"}";
            messageSender.sendJson(ackJson);
            logger.info("SESSION_START acknowledged");
        } else {
            String ackJson = "{\"ack_status\": \"ERROR\", \"message\": \"Session already started\"}";
            messageSender.sendJson(ackJson);
            logger.warn("SESSION_START rejected: session already active");
        }
    }
    
    /**
     * Handle SESSION_END message.
     */
    private void handleSessionEnd() throws IOException {
        logger.info("Received SESSION_END from Central");
        
        boolean success = sessionManager.endSession();
        if (success) {
            String ackJson = "{\"ack_status\": \"OK\", \"message\": \"Session ended\"}";
            messageSender.sendJson(ackJson);
            logger.info("SESSION_END acknowledged. Session duration: {} ms", 
                sessionManager.getSessionDuration());
        } else {
            String ackJson = "{\"ack_status\": \"ERROR\", \"message\": \"No active session\"}";
            messageSender.sendJson(ackJson);
            logger.warn("SESSION_END rejected: no active session");
        }
    }
    
    /**
     * Handle Transaction message.
     */
    private void handleTransaction(String jsonMessage) throws IOException {
        logger.info("Received TRANSACTION from Central");
        
        if (!sessionManager.isSessionActive()) {
            String ackJson = "{\"ack_status\": \"ERROR\", \"message\": \"No active session\"}";
            messageSender.sendJson(ackJson);
            logger.warn("Transaction rejected: session not active");
            return;
        }
        
        // Parse JSON and extract transaction details
        ObjectMapper mapper = new ObjectMapper();
        var jsonNode = mapper.readTree(jsonMessage);
        
        String transactionId = jsonNode.has("transaction_id") ? 
            jsonNode.get("transaction_id").asText() : UUID.randomUUID().toString();
        String amount = jsonNode.has("amount") ? 
            jsonNode.get("amount").asText() : "0";
        String clientId = jsonNode.has("client_id") ? 
            jsonNode.get("client_id").asText() : "UNKNOWN";
        
        // Create Transaction object
        Transaction txn = new Transaction();
        txn.setId(transactionId);
        txn.setAmount(amount);
        txn.setClientId(clientId);
        txn.setReceivedAt(System.currentTimeMillis());
        
        // Enqueue for processing
        boolean enqueued = transactionQueue.enqueue(txn);
        if (enqueued) {
            String ackJson = "{\"ack_status\": \"OK\", \"message\": \"Transaction enqueued\", " +
                "\"transaction_id\": \"" + transactionId + "\"}";
            messageSender.sendJson(ackJson);
            logger.info("Transaction {} enqueued for processing", transactionId);
        } else {
            String ackJson = "{\"ack_status\": \"ERROR\", \"message\": \"Queue full\"}";
            messageSender.sendJson(ackJson);
            logger.warn("Transaction {} rejected: queue full", transactionId);
        }
    }
    
    /**
     * Signal thread to stop gracefully.
     */
    public void shutdown() {
        logger.info("SenderThread shutdown requested");
        running = false;
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing socket during shutdown", e);
        }
    }
    
    /**
     * Check if thread is running.
     */
    public boolean isRunning() {
        return running;
    }
}
