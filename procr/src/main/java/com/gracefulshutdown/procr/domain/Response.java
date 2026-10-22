package com.gracefulshutdown.procr.domain;

/**
 * Response domain model for PROCR service
 */
public class Response {
    private String response_type;  // APPROVE or REJECT
    private String txn_id;
    private String status;         // SUCCESS or FAILED
    private String message;
    private String timestamp;

    public Response() {
    }

    public Response(String response_type, String txn_id, String status, String message, String timestamp) {
        this.response_type = response_type;
        this.txn_id = txn_id;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getResponse_type() {
        return response_type;
    }

    public void setResponse_type(String response_type) {
        this.response_type = response_type;
    }

    public String getTxn_id() {
        return txn_id;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Response{" +
                "response_type='" + response_type + '\'' +
                ", txn_id='" + txn_id + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
