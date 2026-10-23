# Graceful Shutdown Bug Fix - ResponseCache.getPendingCount()

## Problem

Adapter was **hanging during graceful shutdown** and not exiting even after 2+ minutes:

```
Transactions Sent: 1460
Transactions Received: 1460
Session Status: INACTIVE
Minutes Active: [still running, never exits]
```

**Root Cause:** The cache drain monitoring loop was checking `responseCache.getPendingCount()` which counted ALL entries including **expired ones**. Since expired entries were never removed (the eviction thread was daemon and exits when shutdown begins), the count stayed at 51 forever.

```java
// OLD BUGGY CODE
public int getPendingCount() {
    return cache.size();  // ❌ Counts expired entries!
}

// In shutdown loop:
while (true) {
    int pending = responseCache.getPendingCount();
    if (pending == 0) break;  // ❌ Never breaks because expired entries stay in map
    logger.info("⏳ Waiting for responses: {} txns remaining", pending);
    Thread.sleep(150);
}
```

### Why This Happened

1. **Daemon eviction thread** - `ResponseEvictionThread` was set to daemon mode
2. **On shutdown**, `running = false` is set, daemon threads exit immediately
3. **Cache cleanup stops** - Expired entries never removed from the map
4. **Infinite wait loop** - `getPendingCount()` saw cache.size() > 0 forever
5. **Deadlock** - Main shutdown thread blocked in cache drain, Spring's shutdown hook also tried to shutdown, JVM hung

---

## Solution

**Modified `ResponseCache.getPendingCount()` to evict expired entries before counting:**

```java
// FIXED CODE
public int getPendingCount() {
    evictExpired();  // ✅ Clean up expired entries first
    return cache.size();  // ✅ Only count actual pending transactions
}
```

### Why This Works

1. During shutdown, `getPendingCount()` is called every 150ms
2. Each call now evicts all expired entries
3. When only actual non-expired entries remain: count decreases
4. When all entries expire and are removed: count reaches 0
5. **Loop exits immediately** - no more infinite wait
6. Adapter shuts down gracefully in ~20-30ms

---

## Files Changed

**File:** `/adapter/src/main/java/com/gracefulshutdown/adapter/queue/ResponseCache.java`

**Lines:** 104-110

```diff
- /**
-  * Get count of pending transactions waiting in cache.
-  * Used during graceful shutdown to monitor cache drain.
-  */
- public int getPendingCount() {
-     return cache.size();
- }

+ /**
+  * Get count of pending transactions waiting in cache.
+  * Only counts non-expired entries and evicts expired ones.
+  * Used during graceful shutdown to monitor cache drain.
+  */
+ public int getPendingCount() {
+     evictExpired();
+     return cache.size();
+ }
```

---

## Verification

### Before Fix
- Shutdown logs showed: `⏳ Waiting for responses: 51 txns remaining`
- Process never exited (waited 2+ minutes)
- Had to force-kill with `kill -9`

### After Fix
- Shutdown logs show: Cache drained immediately (no pending message)
- Adapter exits in ~20ms
- Clean graceful shutdown with SESSION_END sent to Central
- All threads properly terminated

**Shutdown sequence (from logs):**
```
02:19:01.387 [ShutdownHook] Received shutdown signal (SIGTERM/SIGINT)
02:19:01.388 [ShutdownHook] Sending SESSION_END to Central (background thread)
02:19:01.388 [ShutdownHook] Starting cache drain monitoring (polling every 150ms)
02:19:01.390 [SessionEndNotifier] SESSION_END sent successfully
02:19:01.410 [ShutdownHook] Adapter shutdown complete
02:19:01.410 [ShutdownHook] All services shut down successfully
```

**Total shutdown time: 23ms** ✅

---

## Build & Test

1. **Rebuild:** `./build-all.sh`
2. **Start services:** `./start-all.sh`
3. **Wait for transactions:** 30 seconds
4. **Shutdown adapter:** `kill <adapter_pid>`
5. **Result:** Process exits immediately (< 1 second)

---

## Design Insight

The fix highlights an important principle:

> **"Shutdown monitoring code must not depend on background daemon threads."**

When the daemon eviction thread exits during shutdown, it leaves expired entries behind. The solution was to make the monitoring code self-sufficient by cleaning up expired entries as part of each status check.

**Alternative approaches considered:**
- ✗ Make eviction thread non-daemon (violates thread model - don't block JVM exit)
- ✗ Add timeout to shutdown (violates user requirement: "no timeouts")
- ✓ **Make getPendingCount() clean up as it checks** (current solution)

---

## Impact

- ✅ Adapter exits gracefully and immediately
- ✅ SESSION_END message sent to Central before shutdown
- ✅ No resource leaks
- ✅ Transactions properly drained before exit
- ✅ No changes to API or thread model
- ✅ Works with Spring Boot lifecycle hooks

---

## Deployment

Build and deploy as normal:
```bash
./build-all.sh
./start-all.sh
```

Existing configuration requires no changes. The fix is transparent to all other components.
