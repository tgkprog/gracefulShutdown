package com.gracefulshutdown.central.network;

public interface IncomingQueue {
    void put(AckData ackData) throws InterruptedException;
    AckData take() throws InterruptedException;
    AckData poll();
    boolean isEmpty();
    int size();
    void clear();
}
