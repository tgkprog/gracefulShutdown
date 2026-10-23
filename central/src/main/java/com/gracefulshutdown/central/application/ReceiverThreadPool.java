package com.gracefulshutdown.central.application;

import com.gracefulshutdown.central.network.AckData;
import com.gracefulshutdown.central.network.IncomingQueue;
import com.gracefulshutdown.central.network.TransactionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ReceiverThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverThreadPool.class);
    private final IncomingQueue incomingQueue;
    private final TransactionCache transactionCache;
    private final int poolSize;
    private ExecutorService executor;
    private volatile boolean running = false;

    public ReceiverThreadPool(IncomingQueue incomingQueue, TransactionCache transactionCache,
                             @Value("${txn.receiver.pool.size:2}") int poolSize) {
        this.incomingQueue = incomingQueue;
        this.transactionCache = transactionCache;
        this.poolSize = poolSize;
    }

    public void start() {
        if (running) {
            logger.warn("ReceiverThreadPool already running");
            return;
        }
        
        running = true;
        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "ReceiverThread-" + System.nanoTime());
            t.setDaemon(false);
            return t;
        });
        
        for (int i = 0; i < poolSize; i++) {
            executor.submit(this::receiverLoop);
        }
        logger.info("ReceiverThreadPool started with {} threads", poolSize);
    }

    private void receiverLoop() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    AckData ackData = incomingQueue.take();
                    var ack = ackData.getAcknowledgment();
                    
                    transactionCache.remove(ack.getTxnId());
                    logger.info("Received ACK for {} (response_type: {}, status: {})", 
                        ack.getTxnId(), ack.getResponse_type(), ack.getStatus());
                    
                } catch (InterruptedException e) {
                    logger.debug("{} interrupted", Thread.currentThread().getName());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in receiver thread", e);
        }
        logger.info("{} stopped", Thread.currentThread().getName());
    }

    public void shutdown() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        logger.info("ReceiverThreadPool shutdown");
    }

    public boolean isRunning() {
        return running;
    }
}
