package com.gracefulshutdown.adapter.queue;

import com.gracefulshutdown.adapter.domain.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResponseCache TTL logic.
 */
public class ResponseCacheTest {
    
    @BeforeEach
    public void setUp() {
    }
    
    @Test
    public void testResponseCacheInitialization() {
        ResponseCache cache = new ResponseCache(1000);
        assertNotNull(cache);
        assertEquals(0, cache.size());
    }
    
    @Test
    public void testResponseCachePutAndGet() {
        ResponseCache cache = new ResponseCache(1000);
        Response response = createTestResponse("TXN-001", "APPROVED");
        
        cache.put("TXN-001", response);
        Response retrieved = cache.get("TXN-001");
        
        assertNotNull(retrieved);
    }
    
    @Test
    public void testResponseCacheContains() {
        ResponseCache cache = new ResponseCache(1000);
        Response response = createTestResponse("TXN-002", "APPROVED");
        
        cache.put("TXN-002", response);
        assertTrue(cache.contains("TXN-002"));
        assertFalse(cache.contains("TXN-NOTFOUND"));
    }
    
    @Test
    public void testResponseCacheSize() {
        ResponseCache cache = new ResponseCache(1000);
        cache.put("TXN-001", createTestResponse("TXN-001", "APPROVED"));
        cache.put("TXN-002", createTestResponse("TXN-002", "APPROVED"));
        
        assertEquals(2, cache.size());
    }
    
    @Test
    public void testResponseCacheClear() {
        ResponseCache cache = new ResponseCache(1000);
        cache.put("TXN-001", createTestResponse("TXN-001", "APPROVED"));
        cache.put("TXN-002", createTestResponse("TXN-002", "APPROVED"));
        
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }
    
    private Response createTestResponse(String txnId, String status) {
        Response response = new Response();
        response.setTxn_id(txnId);
        response.setStatus(status);
        response.setMessage("Test response");
        return response;
    }
}
