# Bug Fix: SESSION_START Message Protocol Issue

## Summary
Fixed critical bug where Adapter's SESSION_START and SESSION_END messages were sent in YAML format instead of JSON, causing Central to reject them as "Unknown message type".

## Root Cause
Two separate issues were discovered:

### Issue 1: Race Condition in AdapterOrchestrator
**File**: `/adapter/src/main/java/com/gracefulshutdown/adapter/tcp/AdapterOrchestrator.java`

**Problem**: The `running` flag was set to `true` AFTER background health check tasks were started. Since the ProcrHealthCheckThread checks `while(running)` in its loop, the thread would exit immediately if `running` was still false when the thread began execution.

**Fix**: Moved `running = true` to BEFORE starting background tasks:
```java
// Set running flag FIRST
running = true;

// Then start background tasks
startResponseEvictionTask();
startProcrHealthCheckTask();
```

### Issue 2: Message Format Mismatch
**File**: `/adapter/src/main/java/com/gracefulshutdown/adapter/tcp/TcpMessageSender.java`

**Problem**: SESSION_START and SESSION_END messages were being sent in YAML format:
```
message_type: SESSION_START
---
```

But Central's MessageParser expects JSON:
```
{"message_type":"SESSION_START"}
---
```

This caused Central's TcpReaderThread to log "Unknown message type" for every SESSION_START/SESSION_END message.

**Fix**: Changed message format to JSON:
```java
public void sendSessionStart() throws IOException {
    String sessionStartMsg = "{\"message_type\":\"SESSION_START\"}\n---\n";
    send(sessionStartMsg);
}

public void sendSessionEnd() throws IOException {
    String sessionEndMsg = "{\"message_type\":\"SESSION_END\"}\n---\n";
    send(sessionEndMsg);
}
```

## Impact

### Before Fix
- Adapter: SESSION_START was sent but thread exited immediately (race condition)
- Central: Received SESSION_START but rejected it as unknown message type
- Result: Dashboard showed "no session" even though adapter was connected
- Transactions were flowing but not properly tracked in session context

### After Fix
- Adapter: SESSION_START sends successfully at startup when PROCR becomes available
- Central: Properly parses and processes SESSION_START message
- Result: Dashboard correctly shows session as ACTIVE
- Transactions properly tracked and counted in session metrics

## Testing Results

### Startup Sequence
1. Adapter starts and connects to Central ✅
2. Adapter waits for PROCR health check (10 second intervals)
3. PROCR health check succeeds
4. Adapter sends SESSION_START in JSON format ✅
5. Central receives and processes SESSION_START ✅
6. Session status changes to ACTIVE ✅
7. Transactions begin flowing ✅

### Log Evidence
**Adapter Log** (01:57:23):
```
01:57:23.331 [ProcrHealthCheckThread] INFO - ✅ PROCR became available - auto-starting session
01:57:23.333 [ProcrHealthCheckThread] INFO - ✅ SESSION_START sent to Central and session started locally
```

**Central Log** (01:57:23):
```
2026-10-24 01:57:23.371 [TcpReaderThread] INFO - Received SESSION_START
```

### Dashboard
✅ Adapter Connection: CONNECTED
✅ Session Status: ACTIVE
✅ Transactions Sent: Incrementing
✅ Transactions Received: Incrementing
✅ Response Times: Tracking

## Files Modified
1. `/adapter/src/main/java/com/gracefulshutdown/adapter/tcp/AdapterOrchestrator.java`
   - Lines: Moved `running = true` before `startResponseEvictionTask()` and `startProcrHealthCheckTask()`
   - Impact: Ensures background threads see `running=true` flag

2. `/adapter/src/main/java/com/gracefulshutdown/adapter/tcp/TcpMessageSender.java`
   - Lines: Updated `sendSessionStart()` and `sendSessionEnd()` methods
   - Impact: Messages now sent in JSON format compatible with Central's parser

## Build & Deployment
```bash
cd /data/code/gt/gracefulShutdown
./build-all.sh
```

All services rebuild successfully:
- ✅ Central: 66M
- ✅ PROCR: 21M
- ✅ Adapter: 21M

## Verification Checklist
- [x] Adapter sends SESSION_START when PROCR becomes available
- [x] Central receives SESSION_START message
- [x] Central recognizes message type (not "Unknown")
- [x] Session state transitions to ACTIVE
- [x] Transactions are generated and sent
- [x] Adapter processes and responds to transactions
- [x] Central receives and counts transaction responses
- [x] Dashboard metrics update in real-time
- [x] Graceful shutdown still works properly
- [x] No timeouts or hanging threads

## Design Notes
- SESSION_START is only sent once per adapter startup
- Message format changed from YAML to JSON to match Central's MessageParser
- ProcrHealthCheckThread now remains alive for entire lifetime (no race condition)
- Background threads properly coordinate with main thread via `running` flag

## Performance Impact
**None** - Fix adds no overhead beyond proper message format conversion.

## Backward Compatibility
**Not applicable** - Protocol message format change requires both sides to be updated. Central already expected JSON format; adapter is now corrected to match.
