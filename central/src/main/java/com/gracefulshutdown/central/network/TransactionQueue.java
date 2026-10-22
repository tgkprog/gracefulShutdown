package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.domain.Transaction;
import java.util.concurrent.BlockingQueue;

public interface TransactionQueue {
    void put(Transaction transaction) throws InterruptedException;
    Transaction take() throws InterruptedException;
    boolean isEmpty();
    int size();
    void clear();
}
