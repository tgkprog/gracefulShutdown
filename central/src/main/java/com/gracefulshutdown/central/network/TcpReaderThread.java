package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.application.SessionManager;
import com.gracefulshutdown.central.application.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TcpReaderThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TcpReaderThread.class);
    private final InputStream socketInput;
    private final IncomingQueue incomingQueue;
    private final SessionManager sessionManager;
    private final MessageParser messageParser;
    private volatile boolean running = true;

    public TcpReaderThread(InputStream socketInput, IncomingQueue incomingQueue,
                          SessionManager sessionManager, MessageParser messageParser) {
        this.socketInput = socketInput;
        this.incomingQueue = incomingQueue;
        this.sessionManager = sessionManager;
        this.messageParser = messageParser;
        setName("TcpReaderThread");
        setDaemon(false);
    }

    @Override
    public void run() {
        logger.info("TcpReaderThread started");
        BufferedReader reader = new BufferedReader(new InputStreamReader(socketInput, StandardCharsets.UTF_8));
        StringBuilder messageBuilder = new StringBuilder();
        
        try {
            String line;
            while (running && !Thread.currentThread().isInterrupted()) {
                line = reader.readLine();
                if (line == null) {
                    logger.info("Socket closed by peer");
                    break;
                }
                
                if (line.equals("---")) {
                    String yaml = messageBuilder.toString().trim();
                    messageBuilder = new StringBuilder();
                    
                    if (!yaml.isEmpty()) {
                        handleMessage(yaml);
                    }
                } else {
                    if (messageBuilder.length() > 0) {
                        messageBuilder.append("\n");
                    }
                    messageBuilder.append(line);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading from socket", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error("Error closing reader", e);
            }
            logger.info("TcpReaderThread stopped");
        }
    }

    private void handleMessage(String yaml) {
        try {
            MessageParser.MessageType messageType = messageParser.parseMessageType(yaml);
            
            switch (messageType) {
                case SESSION_START:
                    logger.info("Received SESSION_START");
                    sessionManager.startSession();
                    break;
                    
                case SESSION_END:
                    logger.info("Received SESSION_END");
                    sessionManager.requestSessionEnd();
                    break;
                    
                case TXN_ACKNOWLEDGE:
                    logger.debug("Received TXN_ACKNOWLEDGE");
                    try {
                        var ack = messageParser.parseAcknowledgment(yaml);
                        incomingQueue.put(new AckData(ack));
                    } catch (Exception e) {
                        logger.error("Error parsing acknowledgment", e);
                    }
                    break;
                    
                default:
                    logger.warn("Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error handling message", e);
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
