package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.application.MetricsService;
import com.gracefulshutdown.central.application.ReceiverThreadPool;
import com.gracefulshutdown.central.application.SessionManager;
import com.gracefulshutdown.central.application.SenderThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private final SessionManager sessionManager;
    private final SenderThreadPool senderThreadPool;
    private final ReceiverThreadPool receiverThreadPool;
    private final OutgoingQueue outgoingQueue;
    private final IncomingQueue incomingQueue;
    private final MessageParser messageParser;
    private final TransactionCache transactionCache;
    private final MetricsService metricsService;
    private final int port;
    private TcpServerHandler tcpServerHandler;

    public ConnectionManager(SessionManager sessionManager, SenderThreadPool senderThreadPool,
                            ReceiverThreadPool receiverThreadPool, OutgoingQueue outgoingQueue,
                            IncomingQueue incomingQueue, MessageParser messageParser,
                            TransactionCache transactionCache, MetricsService metricsService,
                            @Value("${tcp.server.port:5009}") int port) {
        this.sessionManager = sessionManager;
        this.senderThreadPool = senderThreadPool;
        this.receiverThreadPool = receiverThreadPool;
        this.outgoingQueue = outgoingQueue;
        this.incomingQueue = incomingQueue;
        this.messageParser = messageParser;
        this.transactionCache = transactionCache;
        this.metricsService = metricsService;
        this.port = port;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting ConnectionManager");
        tcpServerHandler = new TcpServerHandler(port, sessionManager, senderThreadPool,
                                               receiverThreadPool, outgoingQueue, incomingQueue,
                                               messageParser, transactionCache, metricsService);
        tcpServerHandler.start();
        logger.info("ConnectionManager started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping ConnectionManager");
        if (tcpServerHandler != null) {
            tcpServerHandler.shutdown();
            try {
                tcpServerHandler.join(10000);
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for server handler", e);
                Thread.currentThread().interrupt();
            }
        }
        logger.info("ConnectionManager stopped");
    }
}
