# Adapter Graceful Shutdown Implementation

## Overview

Implemented graceful shutdown for the Adapter service that ensures no transactions are lost during shutdown. The adapter now:

1. **Notifies Central of session end** - Sends SESSION_END message in background thread
2. **Drains response cache** - Waits for all pending responses to be collected and sent back
3. **Monitors progress** - Logs remaining transaction count every 150ms
4. **Clean exit** - Stops threads only after cache is empty

## Architecture

### Spring Lifecycle Hook
```
AdapterApplication (Spring Boot)
    ↓
AdapterService @PostConstruct / @PreDestroy
    ↓
AdapterOrchestrator.start() / shutdown()
    ↓
Graceful shutdown logic
```

The adapter integrates with Spring's lifecycle through:
- `@PostConstruct` in AdapterService → initializes orchestrator
- `@PreDestroy` in AdapterService → triggers shutdown
- Spring automatically calls @PreDestroy when receiving SIGTERM

### Shutdown Flow

```
SIGTERM signal
    ↓
Spring context closing
    ↓
AdapterService.shutdown() called
    ↓
AdapterOrchestrator.shutdown() called
    ↓
Step 1: Start SessionEndNotifier thread
    ├─ Background thread sends SESSION_END to Central
    └─ Does NOT block cache monitoring
    ↓
Step 2: Drain cache (main thread)
    ├─ Poll every 150ms: "⏳ Waiting for X txns remaining"
    ├─ Loop continues indefinitely (NO TIMEOUT)
    └─ Exit only when cache empty (pending count == 0)
    ↓
Step 3: Stop all processing threads
    ├─ SenderThread
    ├─ ProcessorThreads[]
    └─ SessionManager
    ↓
Step 4: Close connections
    ├─ Close Central socket
    └─ Clear response cache
    ↓
✅ Shutdown complete
```

## Implementation Details

### 1. ResponseCache Enhancement

**File:** `ResponseCache.java`

Added method to monitor pending transactions:
```java
public int getPendingCount() {
    return cache.size();
}
```

**Why:** Main shutdown thread needs to poll cache size to determine when all responses have been collected.

### 2. SessionEndNotifier Thread

**File:** `AdapterOrchestrator.java` (inner class)

```java
private static class SessionEndNotifier implements Runnable {
    private final TcpMessageSender messageSender;
    
    @Override
    public void run() {
        messageSender.sendSessionEnd();  // Sends to Central
        logger.info("✅ SESSION_END sent successfully");
    }
}
```

**Why:** 
- Runs in background so it doesn't block cache monitoring
- Allows Central to receive session termination message while adapter is still draining
- Central can properly mark the session as complete

### 3. Cache Drain Monitoring

**File:** `AdapterOrchestrator.java` - `drainResponseCache()` method

```java
private void drainResponseCache() {
    while (true) {
        int pending = responseCache.getPendingCount();
        
        if (pending == 0) {
            logger.info("✅ Response cache drained - {} txns remaining", 0);
            break;
        }
        
        logger.info("⏳ Waiting for responses: {} txns remaining", pending);
        Thread.sleep(150);  // Poll every 150ms
    }
}
```

**Key Features:**
- **No timeout** - Continues polling indefinitely until cache is empty
- **150ms polling interval** - Provides visibility without excessive CPU usage
- **Clear logging** - User can see shutdown progress
- **Blocks main shutdown thread** - Ensures cache drain completes before stopping threads

## Guarantee: No Lost Transactions

### How it works

1. **Inbound transactions stop** - `running = false` stops accepting new transactions
2. **Pending responses collected** - Main thread waits for ResponseCache to drain
3. **Cache drain triggers response send** - ProcessorThreads collect responses and store in cache
4. **Responses sent to Central** - SenderThread sends cached responses back to Central
5. **Cache emptied** - As responses are sent, cache entries are removed
6. **Shutdown proceeds** - Only after cache is empty do threads stop

### Example Timeline

```
Time  | Action                              | Cache Size
------|-------------------------------------|----------
T0    | SIGTERM received                    | 42
T50ms | "⏳ Waiting for responses: 42 txns" | 42
T200ms| "⏳ Waiting for responses: 38 txns" | 38
T350ms| "⏳ Waiting for responses: 25 txns" | 25
T500ms| "⏳ Waiting for responses: 12 txns" | 12
T650ms| "⏳ Waiting for responses: 3 txns"  | 3
T800ms| "⏳ Waiting for responses: 1 txn"   | 1
T950ms| "✅ Response cache drained"         | 0
      | Threads stopped                    | -
      | Socket closed                      | -
      | ✅ Shutdown complete               | -
```

## Usage

### Starting Adapter
```bash
cd /data/code/gt/gracefulShutdown/adapter
./run
```

### Graceful Shutdown
```bash
# Send SIGTERM (graceful shutdown)
kill <adapter-pid>

# Monitor logs:
# ⏳ Waiting for responses: 42 txns remaining
# ⏳ Waiting for responses: 25 txns remaining
# ⏳ Waiting for responses: 0 txns remaining
# ✅ Response cache drained
# ✅ Adapter Orchestrator shutdown complete
```

### Force Kill (if needed)
```bash
# Force kill if graceful shutdown takes too long (uses kill -9)
kill -9 <adapter-pid>
```

## Benefits

✅ **No lost transactions** - All responses collected before shutdown
✅ **Clean session termination** - SESSION_END sent to Central
✅ **Progress visibility** - Logs show remaining transaction count
✅ **No artificial timeouts** - Waits as long as needed
✅ **Spring integration** - Works with standard Spring lifecycle
✅ **Non-blocking design** - Session notification doesn't delay cache drain
✅ **Predictable behavior** - Only exit condition is empty cache

## Testing

### Test 1: Verify Logs During Shutdown

1. Start adapter and send transactions
2. SIGTERM (kill -15) the adapter
3. Watch logs for:
   ```
   🔔 Sending SESSION_END to Central (background thread)
   ⏳ Starting cache drain monitoring
   ⏳ Waiting for responses: X txns remaining
   ⏳ Waiting for responses: Y txns remaining
   ✅ Response cache drained
   ✅ SessionEndNotifier: SESSION_END sent successfully
   ✅ Adapter Orchestrator shutdown complete
   ```

### Test 2: Verify No Lost Transactions

1. Send N transactions to adapter
2. Gracefully shutdown during processing
3. Verify all N responses appear in Central dashboard
4. Verify transaction count matches before and after

### Test 3: Verify SESSION_END Received at Central

1. Shutdown adapter
2. Check Central logs for SESSION_END message
3. Verify session marked as ended in Central's SessionManager

## Configuration

No new configuration required. Existing properties still apply:

```properties
adapter.processor.thread_count=2          # Number of processor threads
adapter.cache.ttl_ms=30000                # Response cache TTL
adapter.shutdown.timeout_ms=15000         # (Not used in cache drain, for thread completion)
```

## Files Modified

1. **ResponseCache.java**
   - Added: `getPendingCount()` method
   - Purpose: Monitor cache size during shutdown

2. **AdapterOrchestrator.java**
   - Modified: `shutdown()` method completely redesigned
   - Added: `drainResponseCache()` method
   - Added: `SessionEndNotifier` inner class
   - Purpose: Implement graceful shutdown with cache monitoring

## Behavior Changes

### Before Implementation
- Adapter would stop threads immediately on SIGTERM
- Some pending responses might not be sent
- No visibility into shutdown progress
- SESSION_END not always sent

### After Implementation
- Adapter waits for all responses before stopping threads
- All responses guaranteed to be sent before exit
- Clear progress logging every 150ms
- SESSION_END always sent (unless socket fails)
- No transactions lost during graceful shutdown

