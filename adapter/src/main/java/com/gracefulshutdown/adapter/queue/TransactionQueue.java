package com.gracefulshutdown.adapter.queue;

import com.gracefulshutdown.adapter.domain.Transaction;

/**
 * TransactionQueue interface for thread-safe transaction queueing
 */
public interface TransactionQueue {
    
    /**
     * Enqueue transaction (blocks if queue full).
     * Returns true if successful, false if interrupted.
     */
    boolean enqueue(Transaction transaction);
    
    /**
     * Dequeue transaction (blocks if queue empty)
     */
    Transaction dequeue() throws InterruptedException;
    
    /**
     * Get current queue size
     */
    int size();
    
    /**
     * Check if queue is empty
     */
    boolean isEmpty();
    
    /**
     * Clear queue
     */
    void clear();
}
