package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.application.MetricsService;
import com.gracefulshutdown.central.application.ReceiverThreadPool;
import com.gracefulshutdown.central.application.SessionManager;
import com.gracefulshutdown.central.application.SessionState;
import com.gracefulshutdown.central.application.SenderThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServerHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);
    private final int port;
    private final SessionManager sessionManager;
    private final SenderThreadPool senderThreadPool;
    private final ReceiverThreadPool receiverThreadPool;
    private final OutgoingQueue outgoingQueue;
    private final IncomingQueue incomingQueue;
    private final MessageParser messageParser;
    private final TransactionCache transactionCache;
    private final MetricsService metricsService;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public TcpServerHandler(int port, SessionManager sessionManager, SenderThreadPool senderThreadPool,
                           ReceiverThreadPool receiverThreadPool, OutgoingQueue outgoingQueue,
                           IncomingQueue incomingQueue, MessageParser messageParser,
                           TransactionCache transactionCache, MetricsService metricsService) {
        this.port = port;
        this.sessionManager = sessionManager;
        this.senderThreadPool = senderThreadPool;
        this.receiverThreadPool = receiverThreadPool;
        this.outgoingQueue = outgoingQueue;
        this.incomingQueue = incomingQueue;
        this.messageParser = messageParser;
        this.transactionCache = transactionCache;
        this.metricsService = metricsService;
        setName("TcpServerHandler");
        setDaemon(false);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("TCP Server listening on port {}", port);
            
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    logger.info("Waiting for client connection...");
                    Socket clientSocket = serverSocket.accept();
                    
                    if (!running) break;
                    
                    handleClientConnection(clientSocket);
                    
                } catch (java.net.SocketException e) {
                    if (running) {
                        logger.error("Socket error", e);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in TCP server", e);
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                logger.error("Error closing server socket", e);
            }
            logger.info("TcpServerHandler stopped");
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        logger.info("Client connected: {}", clientSocket.getInetAddress());
        sessionManager.setState(SessionState.CONNECTED);
        metricsService.onAdapterConnect();
        
        try {
            clientSocket.setSoTimeout(10000);  // 10 second read timeout for adapter communication
            
            InputStream socketInput = clientSocket.getInputStream();
            OutputStream socketOutput = clientSocket.getOutputStream();
            
            TcpReaderThread readerThread = new TcpReaderThread(
                socketInput, incomingQueue, sessionManager, messageParser, metricsService, transactionCache
            );
            
            TcpWriterThread writerThread = new TcpWriterThread(outgoingQueue, socketOutput, metricsService, transactionCache);
            
            senderThreadPool.start();
            receiverThreadPool.start();
            
            readerThread.start();
            writerThread.start();
            
            try {
                readerThread.join();
                writerThread.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for reader/writer threads", e);
                Thread.currentThread().interrupt();
            }
            
            senderThreadPool.shutdown();
            receiverThreadPool.shutdown();
            
        } catch (Exception e) {
            logger.error("Error handling client connection", e);
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                logger.error("Error closing client socket", e);
            }
        }
        
        logger.info("Client disconnected");
        sessionManager.setState(SessionState.DISCONNECTED);
        metricsService.onAdapterDisconnect();
        cleanup();
    }

    private void cleanup() {
        outgoingQueue.clear();
        incomingQueue.clear();
        transactionCache.clear();
        logger.info("Cleanup complete");
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            logger.error("Error closing server socket", e);
        }
        this.interrupt();
    }
}
