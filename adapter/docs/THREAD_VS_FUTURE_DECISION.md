# Thread vs Future: Why `sessionEndThread.start()` Was Chosen

## Overview

In `AdapterOrchestrator#shutdown()`, the SESSION_END notification is sent using a **direct `Thread` with `.start()`** rather than a `Future` or `ExecutorService`. This document explains the architectural reasoning behind this choice.

**Location:** `adapter/src/main/java/com/gracefulshutdown/adapter/tcp/AdapterOrchestrator.java:212-214`

```java
Thread sessionEndThread = new Thread(new SessionEndNotifier(messageSender), "SessionEndNotifier");
sessionEndThread.setDaemon(false);
sessionEndThread.start();
```

---

## Context: Shutdown Sequence

The graceful shutdown follows a strict three-phase sequence:

```
Phase 1 (Background):  Send SESSION_END to Central (non-blocking)
       ↓
Phase 2 (Main Thread): Poll ResponseCache until empty (blocking)
       ↓
Phase 3 (Sequential):  Stop all processing threads
```

The key requirement is that **Phase 1 must not block Phase 2**. The adapter must simultaneously:
- Send a message to Central (Phase 1)
- Drain pending responses from the cache (Phase 2)
- Without either one blocking the other

---

## Alternative Approaches Considered

### ❌ Option 1: ExecutorService with Future
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<?> future = executor.submit(new SessionEndNotifier(messageSender));
// Continue without waiting for future.get()
```

**Pros:**
- Thread pooling (though unnecessary here - only 1 thread)
- Standard framework (AsyncTaskExecutor in Spring)
- Can call `.get()` later to wait if needed

**Cons:**
- ❌ **ExecutorService must be stored as field** → lifecycle complexity
- ❌ **Requires explicit shutdown** in JVM shutdown hook or Spring lifecycle
- ❌ **Not designed for one-shot fire-and-forget** tasks
- ❌ Thread pool overhead for single message
- ❌ Must handle graceful vs immediate shutdown semantics
- ❌ Risk of executor threads not completing during JVM shutdown

### ❌ Option 2: ExecutorService with Future + Spring SmartLifecycle
```java
@Component
class ShutdownExecutorManager implements SmartLifecycle {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Override
    public void stop() {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

**Pros:**
- Proper Spring integration
- Lifecycle-managed shutdown

**Cons:**
- ❌ **Over-engineered** for one-off task
- ❌ **Adds component complexity** to codebase
- ❌ **New resource to manage and test**
- ❌ Requires another registered bean
- ❌ Hidden dependency (must remember to manage executor lifecycle)

### ❌ Option 3: Spring's @Async
```java
@Async
public void sendSessionEndAsync() {
    messageSender.sendSessionEnd();
}

// In shutdown():
sendSessionEndAsync();
```

**Pros:**
- Minimal code
- Spring-managed thread pool

**Cons:**
- ❌ **Decorator pattern hides threading** → hard to debug
- ❌ **Can't set thread name** ("SessionEndNotifier")
- ❌ **Requires Spring thread pool** for simple one-off task
- ❌ **Harder to control** thread behavior (daemon, priority, etc.)
- ❌ **Ties business logic to Spring framework** (testability issue)

### ❌ Option 4: CompletableFuture
```java
CompletableFuture.runAsync(() -> messageSender.sendSessionEnd())
    .exceptionally(ex -> {
        logger.error("Failed", ex);
        return null;
    });
```

**Pros:**
- Modern Java API
- Can chain operations

**Cons:**
- ❌ **Uses ForkJoinPool by default** (wrong resource)
- ❌ **Still requires custom ExecutorService** if you want control
- ❌ **Overkill** for single message with no chaining
- ❌ **Harder to test** (callback-based completion)

---

## ✅ Why Direct Thread Was Chosen

### 1. **Perfect Fit for Fire-and-Forget Semantics**

The SESSION_END notification is truly fire-and-forget:
- Send once
- No waiting for result
- No chaining of operations
- No need to poll completion status

```java
// Direct Thread: Intent is crystal clear
Thread sessionEndThread = new Thread(new SessionEndNotifier(...));
sessionEndThread.start();

// vs ExecutorService: Requires explanation
// Why is this future not stored? Will it be awaited?
Future<?> future = executor.submit(...);
```

### 2. **Zero Resource Management Overhead**

**Direct Thread:**
- Created when needed
- Garbage collected after completion
- No lifecycle management needed
- No bean registration
- No executor pool to shut down

**ExecutorService:**
- Must be stored as field
- Must implement lifecycle hooks
- Must explicitly shutdown in Spring lifecycle or JVM shutdown
- Risk of orphaned threads if shutdown fails

### 3. **Explicit Control & Debugging**

Direct thread allows precise control:

```java
Thread sessionEndThread = new Thread(new SessionEndNotifier(messageSender), "SessionEndNotifier");
sessionEndThread.setDaemon(false);  // ✅ Will complete even after main thread exits
sessionEndThread.start();
```

Named thread shows clearly in logs and debugging:
```
2026-10-24 16:38:19 INFO  [SessionEndNotifier] 📤 Sending SESSION_END message to Central
2026-10-24 16:38:19 INFO  [SessionEndNotifier] ✅ SESSION_END sent successfully
```

ExecutorService hides the thread name or uses generic names like "pool-1-thread-1".

### 4. **Shutdown Behavior is Critical**

The `setDaemon(false)` is semantically important:

```java
sessionEndThread.setDaemon(false);
```

This ensures:
- Thread completes even if main thread finishes first
- JVM won't exit until SESSION_END is sent
- No messages are lost during shutdown

With ExecutorService, this must be managed by:
- Storing shutdown state
- Checking in JVM shutdown hook
- Coordinating with Spring lifecycle

**The direct approach is more explicit.**

### 5. **Aligns with Orchestrator's Single-Threaded Architecture**

The `AdapterOrchestrator` creates threads explicitly for each specific role:
- `SenderThread` - sends transactions
- `ProcessorThread[]` - processes responses
- `SessionEndNotifier` - sends session end notification

All are created explicitly with `new Thread(runnable)`. This is **consistent** with the existing pattern.

Using `ExecutorService` would break this pattern and introduce hidden thread pooling.

### 6. **No Hidden Surprises During Shutdown**

Direct Thread approach:
- Thread pool callbacks don't interfere with shutdown sequence
- Clear control of when threads start and complete
- No unexpected thread pool behavior

ExecutorService approach requires:
- Ensure executor.shutdown() is called
- Handle executor.awaitTermination() timeouts
- Deal with rejected tasks if executor is shutdown
- Risk of race conditions between Spring lifecycle and executor lifecycle

---

## Trade-offs Analysis

| Aspect | Direct Thread | ExecutorService |
|--------|---------------|-----------------|
| **Code Complexity** | ✅ Low | ❌ Medium-High |
| **Resource Overhead** | ✅ None | ❌ Thread pool + fields |
| **Lifecycle Management** | ✅ Automatic | ❌ Manual |
| **Thread Naming** | ✅ Explicit | ⚠️ Generic |
| **Debugging** | ✅ Clear | ⚠️ Obscured |
| **Scalability** | ⚠️ OK for 1 task | ✅ OK for many tasks |
| **Framework Integration** | ✅ None (not needed) | ❌ Unnecessary coupling |
| **Testing** | ✅ Easy | ⚠️ Harder (mocking executor) |
| **Shutdown Safety** | ✅ Guaranteed | ⚠️ Requires discipline |

---

## When ExecutorService WOULD Be Better

ExecutorService would be appropriate if:

1. **Multiple SESSION_END notifications** need to be sent simultaneously
   - Currently: Only 1 per shutdown
   - If this changes: Consider ExecutorService

2. **Thread pooling with reuse** across many operations
   - Currently: Only used once during shutdown
   - If used frequently: Consider pooling

3. **Queuing and backpressure** needed
   - Currently: Fire-and-forget
   - If queue management needed: ExecutorService helps

4. **Reactive/async chains** with multiple stages
   - Currently: Single synchronous operation
   - If chaining CompletableFuture stages: Consider reactive framework

**None of these conditions apply to the current implementation.**

---

## Design Pattern Applied

This is the **"Background Worker"** pattern:

```
Main Thread (Shutdown)
    │
    ├─→ [Background Thread] → Send SESSION_END → Complete & exit
    │
    └─→ [Drain Cache] → Poll until empty → Continue
```

The pattern is **simple and direct**, not requiring frameworks or pools.

---

## Concurrency Safety Guarantee

The `setDaemon(false)` ensures the contract is maintained:

```
Shutdown Sequence Timeline
═══════════════════════════════════════════════════════════════

T0: shutdown() called
    │
    ├─→ sessionEndThread.start() ─→ [Background: send SESSION_END]
    │
    ├─→ drainResponseCache() ────→ [Main: poll until empty]
    │
    ├─→ sessionManager.shutdown()
    │
    ├─→ senderThread.shutdown()
    │
    └─→ waitForThreadCompletion()

Guarantees:
- Background thread (Daemon=false) completes even if main thread exits early
- No "incomplete shutdown" scenarios
- SESSION_END is GUARANTEED to be sent before JVM exits
```

---

## Conclusion

**Direct `Thread.start()` was chosen because:**

1. It perfectly matches the fire-and-forget semantics
2. Zero resource overhead or lifecycle management needed
3. Explicit and easy to debug
4. Consistent with existing orchestrator threading pattern
5. Clear shutdown guarantees with `setDaemon(false)`
6. Simpler than ExecutorService for this single use case

**ExecutorService would be over-engineering** for a one-shot notification task.

### Recommendation

Keep the direct Thread approach. Only reconsider if:
- Multiple concurrent SESSION_END notifications needed
- SessionEndNotifier grows into a family of shutdown tasks
- Thread pooling becomes valuable elsewhere in the adapter

For now, **simplicity and clarity win**. ✅

---

## References

- Java Thread Documentation: https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html
- Thread.setDaemon(false) semantics: Ensures thread completes before JVM exit
- ExecutorService: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
- Related Code:
  - `AdapterOrchestrator.java:212-214` - Thread creation
  - `AdapterOrchestrator.java:300-319` - SessionEndNotifier implementation
  - `AdapterShutdownListener.java` - Spring SmartLifecycle integration

