package com.gracefulshutdown.adapter.http;

import com.gracefulshutdown.adapter.domain.Response;
import com.gracefulshutdown.adapter.error.CircuitBreaker;
import com.gracefulshutdown.adapter.error.ErrorHandler;
import com.gracefulshutdown.adapter.error.RetryPolicy;
import com.gracefulshutdown.adapter.util.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * ProcrClient handles HTTP communication with PROCR service with resilience patterns.
 * Integrates circuit breaker, retry policy, and error handling.
 */
@Service
public class ProcrClient {
    private static final Logger logger = LoggerFactory.getLogger(ProcrClient.class);

    private final RestTemplate restTemplate;
    private final String procrUrl;
    private final int timeout;
    private final YamlParser yamlParser;
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final ErrorHandler errorHandler;

    public ProcrClient(RestTemplate restTemplate,
                       @Value("${adapter.procr.url:http://localhost:8080}") String procrUrl,
                       @Value("${adapter.procr.timeout:10000}") int timeout,
                       CircuitBreaker circuitBreaker,
                       RetryPolicy retryPolicy,
                       ErrorHandler errorHandler) {
        this.restTemplate = restTemplate;
        this.procrUrl = procrUrl;
        this.timeout = timeout;
        this.yamlParser = new YamlParser();
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
        this.errorHandler = errorHandler;
        logger.info("ProcrClient initialized: url={}, timeout={}ms", procrUrl, timeout);
    }

    /**
     * Post YAML transaction to PROCR with retry logic and circuit breaker.
     * Returns null if circuit breaker is open or max retries exceeded.
     */
    public Response sendTransaction(String yamlTransaction, String txnId) throws Exception {
        // Check circuit breaker first
        if (!circuitBreaker.canAttempt()) {
            logger.warn("Transaction {} - Circuit breaker OPEN, rejecting request", txnId);
            long retryAfterMs = circuitBreaker.getTimeUntilRetry();
            throw new Exception("Circuit breaker open, retry after " + retryAfterMs + "ms");
        }
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt < retryPolicy.getMaxAttempts(); attempt++) {
            try {
                if (attempt > 0) {
                    errorHandler.logRetryAttempt(txnId, attempt + 1, retryPolicy.getMaxAttempts());
                    if (!retryPolicy.sleep(attempt)) {
                        // Sleep was interrupted
                        throw new InterruptedException("Retry sleep interrupted");
                    }
                }
                
                Response response = sendTransactionInternal(yamlTransaction, txnId);
                circuitBreaker.recordSuccess();
                return response;
                
            } catch (Exception e) {
                lastException = e;
                
                if (!errorHandler.isRetryable(e) || !retryPolicy.shouldRetry(attempt + 1)) {
                    // Not retryable or no more retries
                    circuitBreaker.recordFailure();
                    throw e;
                }
                
                logger.debug("Transaction {} - Attempt {}/{} failed, will retry: {}", 
                    txnId, attempt + 1, retryPolicy.getMaxAttempts(), e.getMessage());
            }
        }
        
        // All retries exhausted
        circuitBreaker.recordFailure();
        throw new Exception("Max retries exhausted", lastException);
    }

    /**
     * Internal method to actually post to PROCR.
     */
    private Response sendTransactionInternal(String yamlTransaction, String txnId) throws Exception {
        String url = procrUrl + "/process";
        
        logger.debug("Transaction {} - Posting to PROCR: url={}", txnId, url);

        try {
            // Set up headers for JSON content (PROCR expects APPLICATION_JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request (despite parameter name, this is JSON for PROCR)
            HttpEntity<String> request = new HttpEntity<>(yamlTransaction, headers);
            
            // Post to PROCR
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String jsonResponse = responseEntity.getBody();
                logger.debug("Transaction {} - Received response from PROCR (length={})", 
                    txnId, jsonResponse != null ? jsonResponse.length() : 0);
                
                // Parse JSON response to Response object
                Response response = parseJsonResponse(jsonResponse, txnId);
                logger.info("Transaction {} - Processed: status={}", txnId, response.getStatus());
                
                return response;
            } else {
                errorHandler.handleHttpError(txnId, responseEntity.getStatusCode().value(), 
                    responseEntity.getBody());
                throw new Exception("PROCR returned " + responseEntity.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            errorHandler.handleHttpError(txnId, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (HttpServerErrorException e) {
            errorHandler.handleHttpError(txnId, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            errorHandler.handleTransactionError(txnId, e, "PROCR POST");
            throw e;
        }
    }

    /**
     * Check PROCR health status
     */
    public boolean isProcrHealthy() {
        try {
            String url = procrUrl + "/health";
            logger.debug("Checking PROCR health: url={}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            boolean healthy = response.getStatusCode().is2xxSuccessful();
            logger.info("PROCR health check: {}", healthy ? "UP" : "DOWN");
            
            return healthy;
        } catch (RestClientException e) {
            logger.error("PROCR health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse JSON response to Response object using Jackson ObjectMapper
     */
    private Response parseJsonResponse(String jsonContent, String txnId) throws Exception {
        if (jsonContent == null) {
            errorHandler.handleParseError("Transaction " + txnId, "null", 
                new IllegalArgumentException("JSON response is null"));
            throw new IllegalArgumentException("JSON response is null");
        }
        
        try {
            // For now, parse manually or use YamlParser's JSON capability
            // The Response object should be created from JSON
            Response response = yamlParser.parseJsonToResponse(jsonContent);
            if (response == null) {
                errorHandler.handleParseError("Transaction " + txnId, jsonContent, 
                    new IllegalArgumentException("Failed to parse JSON response"));
                throw new IllegalArgumentException("Failed to parse JSON response");
            }
            return response;
        } catch (Exception e) {
            errorHandler.handleParseError("Transaction " + txnId, jsonContent, e);
            throw e;
        }
    }
}

