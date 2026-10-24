package com.gracefulshutdown.adapter.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ErrorHandler provides centralized error handling and logging.
 * Classifies errors and determines recovery strategy.
 */
@Component
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    /**
     * Handle transaction processing error.
     */
    public void handleTransactionError(String txnId, Exception e, String context) {
        if (e instanceof java.net.SocketTimeoutException) {
            logger.error("Transaction {} - TIMEOUT in {}: {}", txnId, context, e.getMessage());
        } else if (e instanceof java.net.ConnectException) {
            logger.error("Transaction {} - CONNECTION_ERROR in {}: {}", txnId, context, e.getMessage());
        } else if (e instanceof java.io.IOException) {
            logger.error("Transaction {} - IO_ERROR in {}: {}", txnId, context, e.getMessage());
        } else if (e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error("Transaction {} - JSON_PARSE_ERROR in {}: {}", txnId, context, e.getMessage());
        } else if (e instanceof org.yaml.snakeyaml.error.YAMLException) {
            logger.error("Transaction {} - YAML_PARSE_ERROR in {}: {}", txnId, context, e.getMessage());
        } else {
            logger.error("Transaction {} - UNEXPECTED_ERROR in {}: {}", txnId, context, e.getMessage(), e);
        }
    }
    
    /**
     * Handle HTTP error response from PROCR.
     */
    public void handleHttpError(String txnId, int statusCode, String responseBody) {
        if (statusCode >= 500 && statusCode < 600) {
            logger.error("Transaction {} - SERVER_ERROR from PROCR ({}): {}", txnId, statusCode, responseBody);
        } else if (statusCode >= 400 && statusCode < 500) {
            logger.error("Transaction {} - CLIENT_ERROR from PROCR ({}): {}", txnId, statusCode, responseBody);
        } else {
            logger.error("Transaction {} - UNKNOWN_HTTP_ERROR ({}): {}", txnId, statusCode, responseBody);
        }
    }
    
    /**
     * Handle connection error.
     */
    public void handleConnectionError(String context, Exception e) {
        logger.error("{} - CONNECTION_ERROR: {} ({})", context, e.getClass().getSimpleName(), e.getMessage());
    }
    
    /**
     * Handle parsing error.
     */
    public void handleParseError(String context, String content, Exception e) {
        String truncated = content != null && content.length() > 100 
            ? content.substring(0, 100) + "..." 
            : content;
        logger.warn("{} - PARSE_ERROR: {} Content: {}", context, e.getMessage(), truncated);
    }
    
    /**
     * Log retry attempt.
     */
    public void logRetryAttempt(String txnId, int attemptNumber, int maxAttempts) {
        logger.info("Transaction {} - Retry attempt {}/{}", txnId, attemptNumber, maxAttempts);
    }
    
    /**
     * Determine if error is retryable.
     */
    public boolean isRetryable(Exception e) {
        if (e instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (e instanceof java.net.ConnectException) {
            return true;
        }
        if (e instanceof java.io.IOException) {
            return true;
        }
        if (e instanceof InterruptedException) {
            return false; // Don't retry on interrupt
        }
        // Parser exceptions are not retryable
        return false;
    }
    
    /**
     * Get suggested retry delay in milliseconds.
     */
    public long getRetryDelayMs(int attemptNumber, long baseDelayMs) {
        // Exponential backoff: baseDelay * 2^attemptNumber
        return baseDelayMs * (long) Math.pow(2, attemptNumber - 1);
    }
}
