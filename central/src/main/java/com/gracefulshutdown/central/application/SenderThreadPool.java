package com.gracefulshutdown.central.application;

import com.gracefulshutdown.central.network.OutgoingQueue;
import com.gracefulshutdown.central.network.TxnData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SenderThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(SenderThreadPool.class);
    private final TransactionGenerator generator;
    private final SessionManager sessionManager;
    private final OutgoingQueue outgoingQueue;
    private final int poolSize;
    private final int ratInterval;
    private final int rateCount;
    private ExecutorService executor;
    private volatile boolean running = false;

    public SenderThreadPool(TransactionGenerator generator, SessionManager sessionManager,
                           OutgoingQueue outgoingQueue,
                           @Value("${txn.sender.pool.size:2}") int poolSize,
                           @Value("${txn.rate.interval:10000}") int ratInterval,
                           @Value("${txn.rate.count:4}") int rateCount) {
        this.generator = generator;
        this.sessionManager = sessionManager;
        this.outgoingQueue = outgoingQueue;
        this.poolSize = poolSize;
        this.ratInterval = ratInterval;
        this.rateCount = rateCount;
    }

    public void start() {
        if (running) {
            logger.warn("SenderThreadPool already running");
            return;
        }
        
        running = true;
        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "SenderThread-" + System.nanoTime());
            t.setDaemon(false);
            return t;
        });
        
        for (int i = 0; i < poolSize; i++) {
            executor.submit(this::senderLoop);
        }
        logger.info("SenderThreadPool started with {} threads", poolSize);
    }

    private void senderLoop() {
        Random random = new Random();
        int sequenceNum = 0;
        
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (!sessionManager.isActive()) {
                    Thread.sleep(100);
                    continue;
                }
                
                if (sessionManager.getState() == SessionState.SESSION_END_REQUESTED) {
                    logger.info("SenderThread {} received SESSION_END_REQUESTED, stopping", 
                               Thread.currentThread().getName());
                    break;
                }
                
                for (int i = 0; i < rateCount; i++) {
                    if (!sessionManager.isActive()) {
                        break;
                    }
                    
                    String txnId = generator.generateTxnId();
                    String stan = generator.generateStan();
                    String txnType = generator.generateTxnType();
                    String narration = generator.generateNarration(txnType);
                    int quantity = generator.generateQuantity();
                    double rate = generator.generateRate();
                    double taxes = generator.calculateTaxes(quantity, rate);
                    double total = generator.calculateTotal(quantity, rate, taxes);
                    
                    var transaction = new com.gracefulshutdown.central.domain.Transaction(
                        txnId, stan, txnType, narration, quantity, rate, taxes, total, Instant.now()
                    );
                    
                    TxnData txnData = new TxnData(transaction, sequenceNum++);
                    outgoingQueue.put(txnData);
                    logger.debug("Generated transaction: {}", txnId);
                    
                    long randomDelay = random.nextLong(ratInterval / rateCount);
                    Thread.sleep(randomDelay);
                }
                
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.debug("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error in sender thread", e);
        }
        logger.info("{} stopped", Thread.currentThread().getName());
    }

    public void shutdown() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        logger.info("SenderThreadPool shutdown");
    }

    public boolean isRunning() {
        return running;
    }
}
