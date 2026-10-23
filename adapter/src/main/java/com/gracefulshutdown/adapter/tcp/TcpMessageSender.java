package com.gracefulshutdown.adapter.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * TcpMessageSender sends JSON messages back to Central
 */
public class TcpMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(TcpMessageSender.class);

    private final Socket socket;
    private final PrintWriter writer;
    private final Object writeLock = new Object();

    public TcpMessageSender(Socket socket) throws IOException {
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        logger.debug("TcpMessageSender initialized for socket");
    }

    /**
     * Send SESSION_START message to Central
     */
    public void sendSessionStart() throws IOException {
        String sessionStartMsg = "message_type: SESSION_START\n---\n";
        send(sessionStartMsg);
    }

    /**
     * Send SESSION_END message to Central
     */
    public void sendSessionEnd() throws IOException {
        String sessionEndMsg = "message_type: SESSION_END\n---\n";
        send(sessionEndMsg);
    }

    /**
     * Send JSON message back to Central
     */
    public void sendJson(String jsonMessage) throws IOException {
        // Add YAML delimiter so Central's TcpReaderThread can parse it
        String messageWithDelimiter = jsonMessage + "\n---\n";
        send(messageWithDelimiter);
    }

    /**
     * Send acknowledgment message back to Central
     */
    public void sendAck(String status, String message) throws IOException {
        String jsonAck = String.format("{\"ack_status\":\"%s\",\"message\":\"%s\"}", status, message);
        send(jsonAck);
    }

    /**
     * Send response message back to Central
     */
    public void sendResponse(String jsonResponse) throws IOException {
        send(jsonResponse);
    }

    /**
     * Send raw message
     */
    private void send(String message) throws IOException {
        synchronized (writeLock) {
            logger.debug("Sending message (length={})", message.length());
            writer.println(message);
            writer.flush();
            logger.debug("Message sent");
        }
    }

    public void close() {
        try {
            writer.close();
            logger.debug("TcpMessageSender closed");
        } catch (Exception e) {
            logger.error("Error closing writer: {}", e.getMessage());
        }
    }
}
