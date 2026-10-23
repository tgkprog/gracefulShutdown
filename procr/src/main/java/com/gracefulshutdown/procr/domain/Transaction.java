package com.gracefulshutdown.procr.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transaction domain model for PROCR service
 */
public class Transaction {
@JsonProperty("txn_id")
    @JsonAlias({"txnId", "transaction_id"})
    private String txn_id;
    
    @JsonProperty("stan")
    private String stan;
    
    @JsonProperty("type")
    @JsonAlias("txnType")
    private String type;
    
    @JsonProperty("narration")
    private String narration;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("rate")
    private Double rate;
    
    @JsonProperty("taxes")
    private Double taxes;
    
    @JsonProperty("total")
    private Double total;
    
    @JsonProperty("timestamp")
    private String timestamp;

    public Transaction() {
    }

    public Transaction(String txn_id, String stan, String type, String narration,
                      Integer quantity, Double rate, Double taxes, Double total, String timestamp) {
        this.txn_id = txn_id;
        this.stan = stan;
        this.type = type;
        this.narration = narration;
        this.quantity = quantity;
        this.rate = rate;
        this.taxes = taxes;
        this.total = total;
        this.timestamp = timestamp;
    }

    public String getTxn_id() {
        return txn_id;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
    }

    public String getStan() {
        return stan;
    }

    public void setStan(String stan) {
        this.stan = stan;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getTaxes() {
        return taxes;
    }

    public void setTaxes(Double taxes) {
        this.taxes = taxes;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txn_id='" + txn_id + '\'' +
                ", stan='" + stan + '\'' +
                ", type='" + type + '\'' +
                ", narration='" + narration + '\'' +
                ", quantity=" + quantity +
                ", rate=" + rate +
                ", taxes=" + taxes +
                ", total=" + total +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
