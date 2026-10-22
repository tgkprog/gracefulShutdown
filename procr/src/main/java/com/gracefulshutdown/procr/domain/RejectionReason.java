package com.gracefulshutdown.procr.domain;

/**
 * Rejection reason enum for PROCR service (1% rejection scenarios)
 */
public enum RejectionReason {
    INSUFFICIENT_FUNDS("Insufficient Funds"),
    CARD_CLOSED("Card Closed"),
    NETWORK_DOWN("Network Down");

    private final String message;

    RejectionReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
