package com.gracefulshutdown.adapter.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;

/**
 * TcpClientConnector connects to Central Server as a TCP client
 * Implements auto-reconnect with exponential backoff
 */
@Service
public class TcpClientConnector {
    private static final Logger logger = LoggerFactory.getLogger(TcpClientConnector.class);

    private final String centralHost;
    private final int centralPort;
    private final int maxRetries;
    private final long initialRetryDelayMs;
    private final long maxRetryDelayMs;

    public TcpClientConnector(
            @Value("${adapter.central.host:localhost}") String centralHost,
            @Value("${adapter.central.port:5009}") int centralPort,
            @Value("${adapter.central.max.retries:30}") int maxRetries,
            @Value("${adapter.central.retry.initial.delay.ms:1000}") long initialRetryDelayMs,
            @Value("${adapter.central.retry.max.delay.ms:30000}") long maxRetryDelayMs) {
        this.centralHost = centralHost;
        this.centralPort = centralPort;
        this.maxRetries = maxRetries;
        this.initialRetryDelayMs = initialRetryDelayMs;
        this.maxRetryDelayMs = maxRetryDelayMs;
        logger.info("TcpClientConnector initialized: central={}:{}, maxRetries={}, retryDelay={}ms~{}ms",
                centralHost, centralPort, maxRetries, initialRetryDelayMs, maxRetryDelayMs);
    }

    /**
     * Connect to Central Server with automatic retry and exponential backoff
     */
    public Socket connectToCentral() throws IOException {
        long retryDelayMs = initialRetryDelayMs;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                logger.info("Attempting to connect to Central (attempt {}/{}): {}:{}", 
                    attempt, maxRetries, centralHost, centralPort);
                
                Socket socket = new Socket(centralHost, centralPort);
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(4000);  // 4 second read timeout (less than join timeout 5000ms)
                
                logger.info("✅ Successfully connected to Central Server at {}:{} on attempt {}", 
                    centralHost, centralPort, attempt);
                return socket;
                
            } catch (IOException e) {
                logger.warn("❌ Connection attempt {} failed: {} - Retrying in {}ms...", 
                    attempt, e.getMessage(), retryDelayMs);
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        logger.error("Interrupted during retry delay", ie);
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while connecting to Central", ie);
                    }
                    
                    // Exponential backoff with cap
                    retryDelayMs = Math.min(retryDelayMs * 2, maxRetryDelayMs);
                } else {
                    logger.error("❌ Failed to connect to Central after {} attempts", maxRetries);
                    throw new IOException("Could not connect to Central after " + maxRetries + " attempts", e);
                }
            }
        }
        
        throw new IOException("Failed to connect to Central: exhausted all retry attempts");
    }

    public String getCentralHost() {
        return centralHost;
    }

    public int getCentralPort() {
        return centralPort;
    }
}
