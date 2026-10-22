package com.gracefulshutdown.adapter.service;

import com.gracefulshutdown.adapter.error.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RetryPolicy exponential backoff logic.
 */
public class RetryPolicyTest {
    
    @BeforeEach
    public void setUp() {
    }
    
    @Test
    public void testRetryPolicyInitialization() {
        RetryPolicy rp = new RetryPolicy();
        assertNotNull(rp);
        assertTrue(rp.getMaxAttempts() > 0);
    }
    
    @Test
    public void testRetryPolicyShouldRetry() {
        RetryPolicy rp = new RetryPolicy();
        assertTrue(rp.shouldRetry(0));
        assertTrue(rp.shouldRetry(1));
    }
    
    @Test
    public void testRetryPolicyGetDelay() {
        RetryPolicy rp = new RetryPolicy();
        long delay1 = rp.getRetryDelayMs(1);
        long delay2 = rp.getRetryDelayMs(2);
        
        assertTrue(delay1 >= 0);
        assertTrue(delay2 >= delay1);  // Exponential backoff
    }
    
    @Test
    public void testRetryPolicyMaxAttempts() {
        RetryPolicy rp = new RetryPolicy();
        assertEquals(3, rp.getMaxAttempts());
    }
}
