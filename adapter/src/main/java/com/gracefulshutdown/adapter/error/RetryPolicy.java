package com.gracefulshutdown.adapter.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RetryPolicy implements configurable retry logic with exponential backoff.
 * Used for retrying transient failures like timeouts and connection errors.
 */
@Component
public class RetryPolicy {
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);
    
    @Value("${adapter.retry.max_attempts:3}")
    private int maxAttempts;
    
    @Value("${adapter.retry.initial_delay_ms:100}")
    private long initialDelayMs;
    
    @Value("${adapter.retry.max_delay_ms:5000}")
    private long maxDelayMs;
    
    /**
     * Check if retry is allowed for given attempt number.
     */
    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxAttempts;
    }
    
    /**
     * Get delay before next retry in milliseconds.
     * Uses exponential backoff: delay = initialDelay * 2^(attemptNumber-1)
     * Capped at maxDelayMs
     */
    public long getRetryDelayMs(int attemptNumber) {
        if (!shouldRetry(attemptNumber)) {
            return 0;
        }
        
        // Exponential backoff with cap
        long delay = initialDelayMs * (long) Math.pow(2, attemptNumber - 1);
        return Math.min(delay, maxDelayMs);
    }
    
    /**
     * Sleep before retry. Can be interrupted.
     */
    public boolean sleep(int attemptNumber) {
        long delayMs = getRetryDelayMs(attemptNumber);
        if (delayMs == 0) {
            return false;
        }
        
        try {
            logger.debug("Retry delay: {}ms before attempt {}", delayMs, attemptNumber + 1);
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException e) {
            logger.debug("Retry sleep interrupted");
            return false;
        }
    }
    
    /**
     * Get max retry attempts.
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
