package com.gracefulshutdown.adapter.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CircuitBreaker implements the circuit breaker pattern for PROCR communication.
 * Prevents cascading failures by temporarily blocking requests when PROCR is down.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: PROCR is down, requests are blocked
 * - HALF_OPEN: Testing if PROCR has recovered
 */
@Component
public class CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }
    
    @Value("${adapter.circuit_breaker.failure_threshold:5}")
    private int failureThreshold;
    
    @Value("${adapter.circuit_breaker.timeout_ms:30000}")
    private long timeoutMs;
    
    @Value("${adapter.circuit_breaker.half_open_max_calls:3}")
    private int halfOpenMaxCalls;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    /**
     * Check if request can proceed.
     * CLOSED: always true
     * OPEN: false (unless timeout expired)
     * HALF_OPEN: true until max calls reached
     */
    public boolean canAttempt() {
        State current = state.get();
        
        if (current == State.CLOSED) {
            return true;
        }
        
        if (current == State.OPEN) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceLastFailure > timeoutMs) {
                logger.info("CircuitBreaker - OPEN→HALF_OPEN (recovery timeout expired)");
                state.set(State.HALF_OPEN);
                halfOpenCallCount.set(0);
                return true;
            }
            return false;
        }
        
        // HALF_OPEN
        if (halfOpenCallCount.incrementAndGet() <= halfOpenMaxCalls) {
            logger.debug("CircuitBreaker - HALF_OPEN test call {}/{}", 
                halfOpenCallCount.get(), halfOpenMaxCalls);
            return true;
        }
        
        return false;
    }
    
    /**
     * Record successful request.
     */
    public void recordSuccess() {
        State current = state.get();
        
        if (current == State.CLOSED) {
            // Reset failure count on success
            failureCount.set(0);
            return;
        }
        
        if (current == State.HALF_OPEN) {
            logger.info("CircuitBreaker - HALF_OPEN→CLOSED (recovery successful)");
            state.set(State.CLOSED);
            failureCount.set(0);
            halfOpenCallCount.set(0);
        }
    }
    
    /**
     * Record failed request.
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        State current = state.get();
        
        if (current == State.CLOSED) {
            int count = failureCount.incrementAndGet();
            logger.warn("CircuitBreaker - Failure {}/{}", count, failureThreshold);
            
            if (count >= failureThreshold) {
                logger.error("CircuitBreaker - CLOSED→OPEN (failure threshold exceeded)");
                state.set(State.OPEN);
            }
            return;
        }
        
        if (current == State.HALF_OPEN) {
            logger.error("CircuitBreaker - HALF_OPEN→OPEN (recovery failed)");
            state.set(State.OPEN);
            halfOpenCallCount.set(0);
        }
    }
    
    /**
     * Get current circuit breaker state.
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Get failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Get time until next retry in milliseconds.
     */
    public long getTimeUntilRetry() {
        if (state.get() == State.CLOSED) {
            return 0;
        }
        
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        return Math.max(0, timeoutMs - timeSinceLastFailure);
    }
    
    /**
     * Reset circuit breaker (for testing).
     */
    public void reset() {
        logger.info("CircuitBreaker - RESET");
        state.set(State.CLOSED);
        failureCount.set(0);
        halfOpenCallCount.set(0);
        lastFailureTime.set(0);
    }
}
