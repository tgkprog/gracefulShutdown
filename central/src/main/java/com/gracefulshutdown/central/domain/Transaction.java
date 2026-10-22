package com.gracefulshutdown.central.domain;

import java.time.Instant;

public class Transaction {
    private String txnId;
    private String stan;
    private String txnType;
    private String narration;
    private int quantity;
    private double rate;
    private double taxes;
    private double total;
    private Instant timestamp;

    public Transaction() {
    }

    public Transaction(String txnId, String stan, String txnType, String narration,
                      int quantity, double rate, double taxes, double total, Instant timestamp) {
        this.txnId = txnId;
        this.stan = stan;
        this.txnType = txnType;
        this.narration = narration;
        this.quantity = quantity;
        this.rate = rate;
        this.taxes = taxes;
        this.total = total;
        this.timestamp = timestamp;
    }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public double getTaxes() { return taxes; }
    public void setTaxes(double taxes) { this.taxes = taxes; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Transaction{" +
                "txnId='" + txnId + '\'' +
                ", stan='" + stan + '\'' +
                ", txnType='" + txnType + '\'' +
                ", narration='" + narration + '\'' +
                ", quantity=" + quantity +
                ", rate=" + rate +
                ", taxes=" + taxes +
                ", total=" + total +
                ", timestamp=" + timestamp +
                '}';
    }
}
