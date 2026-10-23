package com.gracefulshutdown.central.domain;

import java.time.Instant;

public class Acknowledgment {
    private String txnId;
    private String stan;
    private String response_type;  // APPROVE or REJECT (from PROCR)
    private String status;         // SUCCESS or FAILED (processing status)
    private String reason;
    private Instant timestamp;

    public Acknowledgment() {
    }

    public Acknowledgment(String txnId, String stan, String response_type, String status, String reason, Instant timestamp) {
        this.txnId = txnId;
        this.stan = stan;
        this.response_type = response_type;
        this.status = status;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getResponse_type() { return response_type; }
    public void setResponse_type(String response_type) { this.response_type = response_type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Acknowledgment{" +
                "txnId='" + txnId + '\'' +
                ", stan='" + stan + '\'' +
                ", response_type='" + response_type + '\'' +
                ", status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
