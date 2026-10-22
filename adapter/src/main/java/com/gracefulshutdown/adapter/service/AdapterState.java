package com.gracefulshutdown.adapter.service;

/**
 * AdapterState enum for session state machine
 */
public enum AdapterState {
    IDLE("Initial state, no session"),
    SESSION_STARTED("Session started, waiting for transactions"),
    PROCESSING("Processing transactions"),
    SESSION_ENDED("Session ended, no more transactions"),
    SHUTDOWN("Adapter shutting down"),
    ERROR("Error state");

    private final String description;

    AdapterState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
