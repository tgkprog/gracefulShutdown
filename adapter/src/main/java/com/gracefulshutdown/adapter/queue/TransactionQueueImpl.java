package com.gracefulshutdown.adapter.queue;

import com.gracefulshutdown.adapter.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TransactionQueueImpl implements thread-safe transaction queue
 */
@Service
public class TransactionQueueImpl implements TransactionQueue {
    private static final Logger logger = LoggerFactory.getLogger(TransactionQueueImpl.class);

    private final BlockingQueue<Transaction> queue;
    private final int warnThreshold;

    public TransactionQueueImpl(@Value("${adapter.queue.capacity:100}") int capacity,
                               @Value("${adapter.queue.warn_threshold:80}") int warnThreshold) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.warnThreshold = warnThreshold;
        logger.info("TransactionQueue initialized: capacity={}, warn_threshold={}", capacity, warnThreshold);
    }

    @Override
    public boolean enqueue(Transaction transaction) {
        try {
            queue.put(transaction);
            
            int currentSize = queue.size();
            if (currentSize >= warnThreshold) {
                logger.warn("Transaction queue size warning: {} items (capacity approaching limit)", currentSize);
            } else {
                logger.debug("Transaction enqueued: id={}, queue_size={}", transaction.getId(), currentSize);
            }
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted while enqueuing transaction", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public Transaction dequeue() throws InterruptedException {
        Transaction transaction = queue.take();
        logger.debug("Transaction dequeued: id={}, queue_size={}", transaction.getId(), queue.size());
        return transaction;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void clear() {
        queue.clear();
        logger.info("Transaction queue cleared");
    }
}
