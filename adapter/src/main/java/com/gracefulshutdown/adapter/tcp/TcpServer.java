package com.gracefulshutdown.adapter.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;

/**
 * TcpServer listens for incoming connections from Central
 */
@Service
public class TcpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final int backlog;
    
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public TcpServer(@Value("${adapter.tcp.port:5000}") int port,
                     @Value("${adapter.tcp.backlog:1}") int backlog) {
        this.port = port;
        this.backlog = backlog;
        logger.info("TcpServer initialized: port={}, backlog={}", port, backlog);
    }

    /**
     * Start TCP server listening for incoming connections
     */
    public void start() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            logger.warn("TCP server is already running");
            return;
        }

        serverSocket = new ServerSocket(port, backlog);
        running = true;
        logger.info("TCP Server listening on port {} at {}", port, Instant.now());
    }

    /**
     * Accept connection from client (blocking call).
     * Returns the accepted Socket or null if server is stopped.
     */
    public Socket acceptConnection() throws IOException {
        if (serverSocket == null || serverSocket.isClosed()) {
            logger.error("TCP server is not running");
            return null;
        }
        
        try {
            logger.info("Waiting for connection from Central...");
            Socket clientSocket = serverSocket.accept();
            logger.info("Accepted connection from {}: {}", 
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort());
            return clientSocket;
        } catch (SocketTimeoutException e) {
            logger.warn("Accept timeout");
            return null;
        } catch (IOException e) {
            if (running) {
                logger.error("Error accepting connection", e);
            }
            return null;
        }
    }

    /**
     * Stop TCP server
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.info("TCP ServerSocket closed");
            }
        } catch (IOException e) {
            logger.error("Error closing server socket: {}", e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    public int getPort() {
        return port;
    }
}

