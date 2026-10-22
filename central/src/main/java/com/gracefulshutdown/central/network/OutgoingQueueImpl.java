package com.gracefulshutdown.central.network;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OutgoingQueueImpl implements OutgoingQueue {
    private final BlockingQueue<TxnData> queue;

    public OutgoingQueueImpl(@Value("${txn.queue.capacity:100}") int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public void put(TxnData txnData) throws InterruptedException {
        queue.put(txnData);
    }

    @Override
    public TxnData take() throws InterruptedException {
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
