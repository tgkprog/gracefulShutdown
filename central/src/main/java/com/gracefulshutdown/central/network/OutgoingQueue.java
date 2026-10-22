package com.gracefulshutdown.central.network;

public interface OutgoingQueue {
    void put(TxnData txnData) throws InterruptedException;
    TxnData take() throws InterruptedException;
    boolean isEmpty();
    int size();
    void clear();
}
