package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class TransactionQueueImpl implements TransactionQueue {
    private final BlockingQueue<Transaction> queue;

    public TransactionQueueImpl(@Value("${txn.queue.capacity:100}") int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public void put(Transaction transaction) throws InterruptedException {
        queue.put(transaction);
    }

    @Override
    public Transaction take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
