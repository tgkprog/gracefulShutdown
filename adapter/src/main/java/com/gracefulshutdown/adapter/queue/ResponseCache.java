package com.gracefulshutdown.adapter.queue;

import com.gracefulshutdown.adapter.domain.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for PROCR responses with TTL support.
 * Maps transaction ID to response with expiration time.
 */
public class ResponseCache {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCache.class);
    
    private static class CachedResponse {
        final Response response;
        final long expiryTime;
        
        CachedResponse(Response response, long ttlMs) {
            this.response = response;
            this.expiryTime = System.currentTimeMillis() + ttlMs;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final long ttlMs;
    
    public ResponseCache(long ttlMs) {
        this.ttlMs = ttlMs;
        logger.info("ResponseCache initialized with TTL: {} ms", ttlMs);
    }
    
    /**
     * Put response in cache with TTL.
     */
    public void put(String transactionId, Response response) {
        cache.put(transactionId, new CachedResponse(response, ttlMs));
        logger.debug("Cached response for txn {}: status={}", transactionId, response.getStatus());
    }
    
    /**
     * Get response from cache if not expired.
     */
    public Response get(String transactionId) {
        CachedResponse cached = cache.get(transactionId);
        if (cached == null) {
            logger.debug("Response not found in cache for txn: {}", transactionId);
            return null;
        }
        
        if (cached.isExpired()) {
            cache.remove(transactionId);
            logger.warn("Response expired in cache for txn: {}", transactionId);
            return null;
        }
        
        logger.debug("Retrieved cached response for txn: {}", transactionId);
        return cached.response;
    }
    
    /**
     * Check if response exists and is not expired.
     */
    public boolean contains(String transactionId) {
        return get(transactionId) != null;
    }
    
    /**
     * Remove response from cache.
     */
    public void remove(String transactionId) {
        cache.remove(transactionId);
        logger.debug("Removed response from cache for txn: {}", transactionId);
    }
    
    /**
     * Clear all expired entries from cache.
     */
    public void evictExpired() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int finalSize = cache.size();
        if (initialSize > finalSize) {
            logger.info("Evicted {} expired entries from cache", initialSize - finalSize);
        }
    }
    
    /**
     * Get cache size.
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Get count of pending transactions waiting in cache.
     * Only counts non-expired entries and evicts expired ones.
     * Used during graceful shutdown to monitor cache drain.
     */
    public int getPendingCount() {
        evictExpired();
        return cache.size();
    }
    
    /**
     * Clear all entries.
     */
    public void clear() {
        cache.clear();
        logger.info("Response cache cleared");
    }
}
