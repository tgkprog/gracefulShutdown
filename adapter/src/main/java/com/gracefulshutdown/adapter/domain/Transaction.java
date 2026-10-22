package com.gracefulshutdown.adapter.domain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transaction domain model for Adapter service
 */
public class Transaction {
    private String id;
    private String transaction_id;
    private String stan;
    private String type;
    private String narration;
    private Integer quantity;
    private Double rate;
    private Double taxes;
    private Double total;
    private String timestamp;
    private String amount;
    private String clientId;
    private long receivedAt;

    public Transaction() {
    }

    public Transaction(String transaction_id, String stan, String type, String narration,
                      Integer quantity, Double rate, Double taxes, Double total, String timestamp) {
        this.transaction_id = transaction_id;
        this.id = transaction_id;
        this.stan = stan;
        this.type = type;
        this.narration = narration;
        this.quantity = quantity;
        this.rate = rate;
        this.taxes = taxes;
        this.total = total;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.transaction_id = id;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
        this.id = transaction_id;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    /**
     * Convert transaction to JSON string.
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting transaction to JSON", e);
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", stan='" + stan + '\'' +
                ", type='" + type + '\'' +
                ", amount='" + amount + '\'' +
                ", clientId='" + clientId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
