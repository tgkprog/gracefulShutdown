package com.gracefulshutdown.central.network;

import com.gracefulshutdown.central.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TransactionCache {
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public TransactionCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void put(String txnId, Transaction transaction) {
        cache.put(txnId, new CacheEntry(transaction, System.currentTimeMillis()));
    }

    public Transaction get(String txnId) {
        CacheEntry entry = cache.get(txnId);
        if (entry != null) {
            if (isExpired(entry)) {
                cache.remove(txnId);
                return null;
            }
            return entry.transaction;
        }
        return null;
    }

    public boolean containsKey(String txnId) {
        CacheEntry entry = cache.get(txnId);
        if (entry != null && !isExpired(entry)) {
            return true;
        }
        if (entry != null) {
            cache.remove(txnId);
        }
        return false;
    }

    public Transaction remove(String txnId) {
        CacheEntry entry = cache.remove(txnId);
        return entry != null ? entry.transaction : null;
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public void cleanup() {
        cache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.createdAt > ttlMillis;
    }

    private static class CacheEntry {
        Transaction transaction;
        long createdAt;

        CacheEntry(Transaction transaction, long createdAt) {
            this.transaction = transaction;
            this.createdAt = createdAt;
        }
    }
}
