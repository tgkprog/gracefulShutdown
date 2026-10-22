package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.domain.Transaction;

public class TxnData {
    private final Transaction transaction;
    private final long timestamp;
    private final int sequenceNum;

    public TxnData(Transaction transaction, int sequenceNum) {
        this.transaction = transaction;
        this.timestamp = System.currentTimeMillis();
        this.sequenceNum = sequenceNum;
    }

    public Transaction getTransaction() { return transaction; }
    public long getTimestamp() { return timestamp; }
    public int getSequenceNum() { return sequenceNum; }

    @Override
    public String toString() {
        return "TxnData{" + "txn=" + transaction.getTxnId() + 
               ", seq=" + sequenceNum + ", ts=" + timestamp + '}';
    }
}
