package com.gracefulshutdown.adapter.service;

import com.gracefulshutdown.adapter.error.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitBreaker state machine.
 */
public class CircuitBreakerTest {
    
    @BeforeEach
    public void setUp() {
    }
    
    @Test
    public void testCircuitBreakerInitialization() {
        CircuitBreaker cb = new CircuitBreaker();
        assertNotNull(cb);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.canAttempt());
    }
    
    @Test
    public void testCircuitBreakerRecordSuccess() {
        CircuitBreaker cb = new CircuitBreaker();
        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(0, cb.getFailureCount());
    }
    
    @Test
    public void testCircuitBreakerRecordFailure() {
        CircuitBreaker cb = new CircuitBreaker();
        cb.recordFailure();
        assertTrue(cb.getFailureCount() > 0);
    }
    
    @Test
    public void testCircuitBreakerReset() {
        CircuitBreaker cb = new CircuitBreaker();
        cb.recordFailure();
        cb.reset();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(0, cb.getFailureCount());
    }
}
