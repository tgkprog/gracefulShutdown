package com.gracefulshutdown.central.network;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class IncomingQueueImpl implements IncomingQueue {
    private final BlockingQueue<AckData> queue;

    public IncomingQueueImpl(@Value("${txn.queue.capacity:100}") int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public void put(AckData ackData) throws InterruptedException {
        queue.put(ackData);
    }

    @Override
    public AckData take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public AckData poll() {
        return queue.poll();
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
