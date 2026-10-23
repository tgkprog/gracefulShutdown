package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.domain.Acknowledgment;

public class AckData {
    private final Acknowledgment acknowledgment;
    private final long receivedAt;//easier if ISO date with time zone for humans to read, remove long add date time

    public AckData(Acknowledgment acknowledgment) {
        this.acknowledgment = acknowledgment;
        this.receivedAt = System.currentTimeMillis();
    }

    public Acknowledgment getAcknowledgment() { return acknowledgment; }
    public long getReceivedAt() { return receivedAt; }

    @Override
    public String toString() {
        return "AckData{" + "txn=" + acknowledgment.getTxnId() + 
               ", status=" + acknowledgment.getStatus() + ", at=" + receivedAt + '}';
    }
}
