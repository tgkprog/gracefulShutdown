package com.gracefulshutdown.adapter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages session lifecycle and state transitions for adapter.
 * Coordinates between TCP reader thread and processor threads.
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    private final AtomicReference<AdapterState> currentState;
    private volatile long sessionStartTime;
    private volatile long sessionEndTime;
    private volatile boolean sessionStartSent = false;
    
    public SessionManager() {
        this.currentState = new AtomicReference<>(AdapterState.IDLE);
        this.sessionStartTime = 0;
        this.sessionEndTime = 0;
        this.sessionStartSent = false;
        logger.info("SessionManager initialized in state: {}", currentState.get());
    }
    
    /**
     * Mark that SESSION_START has been sent to Central.
     */
    public void markSessionStartSent() {
        this.sessionStartSent = true;
        logger.info("SESSION_START sent to Central marked");
    }
    
    /**
     * Check if SESSION_START has been sent.
     */
    public boolean isSessionStartSent() {
        return sessionStartSent;
    }
    
    /**
     * Transition from IDLE to SESSION_STARTED.
     */
    public boolean startSession() {
        boolean success = currentState.compareAndSet(AdapterState.IDLE, AdapterState.SESSION_STARTED);
        if (success) {
            sessionStartTime = System.currentTimeMillis();
            logger.info("Session started at {}", sessionStartTime);
        } else {
            logger.warn("Cannot start session: current state is {}", currentState.get());
        }
        return success;
    }
    
    /**
     * Transition from SESSION_STARTED to PROCESSING.
     * Enforces strict state machine: SESSION_STARTED → PROCESSING
     */
    public boolean beginProcessing() {
        boolean success = currentState.compareAndSet(AdapterState.SESSION_STARTED, AdapterState.PROCESSING);
        if (success) {
            logger.info("Session processing started");
        } else {
            logger.warn("Cannot begin processing: current state is {} (expected SESSION_STARTED)", currentState.get());
        }
        return success;
    }

    /**
     * Transition from PROCESSING to SESSION_ENDED.
     * Enforces strict state machine: PROCESSING → SESSION_ENDED
     */
    public boolean endSession() {
        boolean success = currentState.compareAndSet(AdapterState.PROCESSING, AdapterState.SESSION_ENDED);
        if (success) {
            sessionEndTime = System.currentTimeMillis();
            logger.info("Session ended at {}. Duration: {} ms", 
                sessionEndTime, sessionEndTime - sessionStartTime);
        } else {
            AdapterState current = currentState.get();
            logger.warn("Cannot end session: current state is {} (expected PROCESSING)", current);
        }
        return success;
    }
    
    /**
     * Transition from any state to SHUTDOWN.
     */
    public boolean shutdown() {
        AdapterState previous = currentState.getAndSet(AdapterState.SHUTDOWN);
        logger.info("Adapter shutting down. Previous state: {}", previous);
        return true;
    }
    
    /**
     * Get current session state.
     */
    public AdapterState getState() {
        return currentState.get();
    }
    
    /**
     * Check if session is active (SESSION_STARTED or PROCESSING).
     */
    public boolean isSessionActive() {
        AdapterState state = currentState.get();
        return state == AdapterState.SESSION_STARTED || state == AdapterState.PROCESSING;
    }
    
    /**
     * Check if processing is active.
     */
    public boolean isProcessing() {
        return currentState.get() == AdapterState.PROCESSING;
    }
    
    /**
     * Check if session is ended.
     */
    public boolean isSessionEnded() {
        return currentState.get() == AdapterState.SESSION_ENDED;
    }
    
    /**
     * Get session duration in milliseconds.
     */
    public long getSessionDuration() {
        if (sessionStartTime == 0) {
            return 0;
        }
        if (sessionEndTime == 0) {
            return System.currentTimeMillis() - sessionStartTime;
        }
        return sessionEndTime - sessionStartTime;
    }
    
    /**
     * Reset to IDLE state.
     */
    public void reset() {
        currentState.set(AdapterState.IDLE);
        sessionStartTime = 0;
        sessionEndTime = 0;
        logger.info("SessionManager reset to IDLE");
    }
    
    /**
     * Get session info as string.
     */
    @Override
    public String toString() {
        return "SessionManager{" +
                "state=" + currentState.get() +
                ", duration=" + getSessionDuration() + "ms" +
                '}';
    }
}
