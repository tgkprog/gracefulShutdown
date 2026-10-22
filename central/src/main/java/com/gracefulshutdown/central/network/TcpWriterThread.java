package com.gracefulshutdown.central.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TcpWriterThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TcpWriterThread.class);
    private final OutgoingQueue outgoingQueue;
    private final OutputStream socketOutput;
    private final ObjectMapper yamlMapper;
    private volatile boolean running = true;

    public TcpWriterThread(OutgoingQueue outgoingQueue, OutputStream socketOutput) {
        this.outgoingQueue = outgoingQueue;
        this.socketOutput = socketOutput;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
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
                    String yaml = yamlMapper.writeValueAsString(txnData.getTransaction());
                    byte[] data = (yaml + "\n---\n").getBytes(StandardCharsets.UTF_8);
                    
                    synchronized (socketOutput) {
                        socketOutput.write(data);
                        socketOutput.flush();
                    }
                    
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
