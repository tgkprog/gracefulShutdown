package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.application.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TcpWriterThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TcpWriterThread.class);
    private final OutgoingQueue outgoingQueue;
    private final OutputStream socketOutput;
    private final ObjectMapper jsonMapper;
    private final MetricsService metricsService;
    private final TransactionCache transactionCache;
    private volatile boolean running = true;

    public TcpWriterThread(OutgoingQueue outgoingQueue, OutputStream socketOutput, MetricsService metricsService,
                          TransactionCache transactionCache) {
        this.outgoingQueue = outgoingQueue;
        this.socketOutput = socketOutput;
        this.metricsService = metricsService;
        this.transactionCache = transactionCache;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.registerModule(new JavaTimeModule());
        this.jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        setName("TcpWriterThread");
        setDaemon(false);
    }

    @Override
    public void run() {
        logger.info("TcpWriterThread started");
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    TxnData txnData = outgoingQueue.take();
                    String json = jsonMapper.writeValueAsString(txnData.getTransaction());
                    byte[] data = (json + "\n").getBytes(StandardCharsets.UTF_8);
                    
                    synchronized (socketOutput) {
                        socketOutput.write(data);
                        socketOutput.flush();
                    }
                    
                    transactionCache.put(txnData.getTransaction().getTxnId(), txnData.getTransaction());
                    metricsService.recordTransactionSent();
                    logger.debug("Sent transaction: {} (seq: {})", 
                                txnData.getTransaction().getTxnId(), txnData.getSequenceNum());
                } catch (InterruptedException e) {
                    logger.debug("TcpWriterThread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in TcpWriterThread", e);
        } finally {
            logger.info("TcpWriterThread stopped");
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
