package com.gracefulshutdown.adapter.controller;

import com.gracefulshutdown.adapter.error.CircuitBreaker;
import com.gracefulshutdown.adapter.queue.ResponseCache;
import com.gracefulshutdown.adapter.queue.TransactionQueue;
import com.gracefulshutdown.adapter.service.AdapterService;
import com.gracefulshutdown.adapter.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check and metrics endpoint for Adapter monitoring.
 * Provides operational status, queue metrics, and circuit breaker state.
 */
@RestController
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    private final AdapterService adapterService;
    private final TransactionQueue transactionQueue;
    private final ResponseCache responseCache;
    private final CircuitBreaker circuitBreaker;
    
    public HealthController(AdapterService adapterService,
                           TransactionQueue transactionQueue,
                           ResponseCache responseCache,
                           CircuitBreaker circuitBreaker) {
        this.adapterService = adapterService;
        this.transactionQueue = transactionQueue;
        this.responseCache = responseCache;
        this.circuitBreaker = circuitBreaker;
    }
    
    /**
     * Simple health check endpoint.
     * Returns 200 if adapter is running, 503 otherwise.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isRunning = adapterService.isRunning();
        response.put("status", isRunning ? "UP" : "DOWN");
        response.put("timestamp", System.currentTimeMillis());
        response.put("adapter_running", isRunning);
        
        HttpStatus status = isRunning ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * Detailed health and metrics endpoint.
     * Returns comprehensive status including queue metrics and circuit breaker state.
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> healthDetailed() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isRunning = adapterService.isRunning();
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", isRunning ? "UP" : "DOWN");
        
        // Session information
        SessionManager sessionManager = adapterService.getSessionManager();
        if (sessionManager != null) {
            response.put("session_active", sessionManager.isSessionActive());
            response.put("session_duration_ms", sessionManager.getSessionDuration());
        } else {
            response.put("session_active", false);
            response.put("session_duration_ms", 0);
        }
        
        // Queue metrics
        Map<String, Object> queueMetrics = new HashMap<>();
        int queueCapacity = transactionQueue.getCapacity();
        int queueSize = transactionQueue.size();
        queueMetrics.put("size", queueSize);
        queueMetrics.put("capacity", queueCapacity);
        queueMetrics.put("percent_full", (int) (100.0 * queueSize / queueCapacity));
        response.put("queue", queueMetrics);
        
        // Cache metrics
        Map<String, Object> cacheMetrics = new HashMap<>();
        cacheMetrics.put("size", responseCache.size());
        response.put("cache", cacheMetrics);
        
        // Circuit breaker state
        Map<String, Object> cbMetrics = new HashMap<>();
        cbMetrics.put("state", circuitBreaker.getState().toString());
        cbMetrics.put("failure_count", circuitBreaker.getFailureCount());
        cbMetrics.put("time_until_retry_ms", circuitBreaker.getTimeUntilRetry());
        response.put("circuit_breaker", cbMetrics);
        
        // Adapter status
        response.put("adapter_status", adapterService.getStatus());
        
        HttpStatus status = isRunning && circuitBreaker.getState() != CircuitBreaker.State.OPEN 
            ? HttpStatus.OK 
            : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * Metrics-only endpoint (for monitoring systems).
     * Returns key metrics without full status.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("queue_size", transactionQueue.size());
        metrics.put("cache_size", responseCache.size());
        metrics.put("circuit_breaker_state", circuitBreaker.getState().toString());
        metrics.put("circuit_breaker_failures", circuitBreaker.getFailureCount());
        
        SessionManager sessionManager = adapterService.getSessionManager();
        if (sessionManager != null) {
            metrics.put("session_duration_ms", sessionManager.getSessionDuration());
        }
        
        return ResponseEntity.ok(metrics);
    }
}
