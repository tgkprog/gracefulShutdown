# Adapter Service - Complete Implementation Guide

## Overview

The Adapter Service is a critical middleware component that converts JSON messages from the Central service into YAML for the PROCR processor, and converts YAML responses back to JSON. It implements resilience patterns including circuit breaker, retry logic, and comprehensive error handling.

## Architecture

```
┌─────────────────┐
│    Central      │
│  (JSON/TCP:5000)│
└────────┬────────┘
         │
         │ JSON Protocol
         ↓
┌──────────────────────┐
│      Adapter         │
│   (TCP Server :5000) │
│                      │
│  ┌────────────────┐  │
│  │  SenderThread  │  │  Reads JSON from Central
│  └────────┬───────┘  │  Enqueues transactions
│           ↓          │
│  ┌────────────────┐  │
│  │TransactionQueue│  │  BlockingQueue (capacity: 100)
│  └────────┬───────┘  │
│           ↓          │
│  ┌────────────────┐  │
│  │ProcessorThread │  │  Pool size: 2 (configurable)
│  │      ×2        │  │  Processes via PROCR
│  └────────┬───────┘  │
│           ↓          │
│  ┌────────────────┐  │
│  │ResponseCache   │  │  TTL: 30s (configurable)
│  └─────────────────┘  │
│                      │
│  ┌────────────────┐  │
│  │CircuitBreaker  │  │  State: CLOSED/OPEN/HALF_OPEN
│  └─────────────────┘  │
└──────────┬────────────┘
           │
           │ HTTP / YAML Protocol
           ↓
        ┌──────────┐
        │  PROCR   │
        │ (HTTP:80)│
        └──────────┘
```

## Components

### 1. SenderThread
- **Purpose**: Reads JSON messages from Central via TCP
- **Responsibilities**:
  - Accept Central connection (port 5000)
  - Parse incoming JSON lines
  - Classify message types (SESSION_START, SESSION_END, TRANSACTION)
  - Update session state
  - Enqueue transactions to processing queue
  - Send ACK responses back to Central

### 2. ProcessorThread Pool
- **Purpose**: Process transactions through PROCR pipeline
- **Pool Size**: 2 (configurable via `adapter.processor.thread_count`)
- **Per-Thread Responsibilities**:
  1. Dequeue transaction from queue
  2. Convert JSON → YAML
  3. POST to PROCR /process endpoint (with retry logic)
  4. Receive YAML response
  5. Cache response with TTL
  6. Convert YAML → JSON
  7. Send JSON response back to Central via TCP

### 3. TransactionQueue
- **Type**: BlockingQueue with capacity limit
- **Capacity**: 100 (configurable)
- **Thread-Safe**: Yes
- **Behavior**:
  - Enqueue blocks if full (or returns false)
  - Dequeue blocks if empty
  - Triggers warning at 80% capacity

### 4. ResponseCache
- **Type**: ConcurrentHashMap with TTL
- **TTL**: 30 seconds (configurable)
- **Purpose**: Store PROCR responses for retry/audit
- **Expiration**: Lazy eviction on retrieval + background daemon thread

### 5. SessionManager
- **Purpose**: Track session lifecycle and state
- **States**:
  - IDLE: No active session
  - SESSION_STARTED: Central sent SESSION_START
  - PROCESSING: Actively processing transactions
  - SESSION_ENDED: Central sent SESSION_END
  - SHUTDOWN: Adapter shutting down
- **Thread-Safe**: Yes (AtomicReference)

### 6. ErrorHandler
- **Purpose**: Centralized error classification and logging
- **Capabilities**:
  - Classify error types (timeout, connection, parse, HTTP)
  - Determine if error is retryable
  - Suggest retry delays

### 7. CircuitBreaker
- **Purpose**: Prevent cascading failures when PROCR is down
- **States**:
  - CLOSED: Normal operation (requests pass through)
  - OPEN: PROCR is down (requests blocked)
  - HALF_OPEN: Testing if PROCR recovered
- **Failure Threshold**: 5 (configurable)
- **Timeout**: 30 seconds (configurable)
- **Half-Open Max Calls**: 3 (configurable)

### 8. RetryPolicy
- **Purpose**: Implement exponential backoff for transient failures
- **Max Attempts**: 3 (configurable)
- **Initial Delay**: 100ms (configurable)
- **Max Delay**: 5000ms (configurable)
- **Backoff Formula**: delay = initialDelay × 2^(attemptNumber-1)

### 9. ShutdownHookManager
- **Purpose**: Handle graceful shutdown on SIGTERM/SIGINT
- **Functionality**:
  - Register JVM shutdown hook
  - Gracefully stop all threads
  - Close connections
  - Exit cleanly

### 10. HealthController
- **Endpoints**:
  - `GET /health` - Simple health check (UP/DOWN)
  - `GET /health/detailed` - Full status with metrics
  - `GET /metrics` - Metrics only (for monitoring systems)
- **Metrics Exposed**:
  - Adapter running status
  - Session state and duration
  - Queue size and capacity
  - Cache size
  - Circuit breaker state

## Message Protocols

### Central → Adapter (JSON over TCP:5000)

#### SESSION_START
```json
{
  "message_type": "SESSION_START",
  "timestamp": 1698081600000
}
```

#### TRANSACTION
```json
{
  "message_type": "TRANSACTION",
  "transaction_id": "TXN-20241023-001",
  "stan": "000001",
  "type": "PURCHASE",
  "narration": "Online payment",
  "quantity": 1,
  "rate": 99.99,
  "taxes": 9.99,
  "total": 109.98,
  "timestamp": 1698081605000
}
```

#### SESSION_END
```json
{
  "message_type": "SESSION_END",
  "timestamp": 1698081610000
}
```

### Adapter Acknowledgments (JSON over TCP:5000)

```json
{
  "ack_status": "OK",
  "message": "Transaction received and queued"
}
```

### Adapter → PROCR (YAML over HTTP:8080)

```yaml
---
transaction_id: TXN-20241023-001
stan: "000001"
type: PURCHASE
narration: Online payment
quantity: 1
rate: 99.99
taxes: 9.99
total: 109.98
timestamp: 1698081605000
```

### PROCR → Adapter (YAML over HTTP:8080)

```yaml
---
transaction_id: TXN-20241023-001
status: APPROVED
message: "Transaction processed successfully"
processing_time_ms: 150
timestamp: 1698081605150
```

### Adapter → Central (JSON over TCP:5000)

```json
{
  "transaction_id": "TXN-20241023-001",
  "status": "APPROVED",
  "message": "Transaction processed successfully",
  "processing_time_ms": 150,
  "timestamp": 1698081605150
}
```

## Configuration

### application.properties

```properties
# Server
server.port=8081
server.servlet.context-path=/

# TCP Server
adapter.tcp.port=5000
adapter.tcp.backlog=1

# PROCR Client
adapter.procr.url=http://localhost:8080
adapter.procr.timeout=10000

# Message Queue
adapter.queue.capacity=100
adapter.queue.warn_threshold=80

# Response Cache
adapter.cache.ttl_ms=30000

# Threading
adapter.processor.thread_count=2
adapter.shutdown.timeout_ms=15000

# Circuit Breaker
adapter.circuit_breaker.failure_threshold=5
adapter.circuit_breaker.timeout_ms=30000
adapter.circuit_breaker.half_open_max_calls=3

# Retry Policy
adapter.retry.max_attempts=3
adapter.retry.initial_delay_ms=100
adapter.retry.max_delay_ms=5000

# Logging
logging.level.root=INFO
logging.level.com.gracefulshutdown.adapter=DEBUG
logging.level.org.springframework=INFO
```

## Build & Run

### Build
```bash
cd /data/code/gt/gracefulShutdown/adapter
mvn clean package -DskipTests
```

### Run
```bash
java -jar target/adapter-json-yaml-converter-1.0.0.jar
```

**Expected Output**:
```
2024-10-23 01:50:00,123 - Adapter Service initialized
2024-10-23 01:50:00,456 - TCP Server listening on port 5000
2024-10-23 01:50:00,789 - PROCR health check: UP
2024-10-23 01:50:00,999 - Adapter ready to accept connections
```

### Health Check
```bash
curl http://localhost:8081/health
```

Response:
```json
{
  "status": "UP",
  "timestamp": 1698081605000,
  "adapter_running": true
}
```

### Detailed Health
```bash
curl http://localhost:8081/health/detailed
```

Response:
```json
{
  "timestamp": 1698081605000,
  "status": "UP",
  "session_active": true,
  "session_duration_ms": 5000,
  "queue": {
    "size": 2,
    "capacity": 100,
    "percent_full": 2
  },
  "cache": {
    "size": 5
  },
  "circuit_breaker": {
    "state": "CLOSED",
    "failure_count": 0,
    "time_until_retry_ms": 0
  },
  "adapter_status": "Running"
}
```

## Error Handling

### Retryable Errors
- Socket timeouts (10s default)
- Connection refused
- IO exceptions
- **Retry Strategy**: Exponential backoff (100ms, 200ms, 400ms)

### Non-Retryable Errors
- JSON/YAML parse errors
- Thread interruption
- Invalid transactions

### Circuit Breaker Behavior
1. **CLOSED State** (Normal):
   - Requests pass through to PROCR
   - Failure count incremented on error
   - Resets to 0 on success

2. **OPEN State** (PROCR Down):
   - Requests blocked immediately
   - Returns error without calling PROCR
   - Waits for timeout (30s)

3. **HALF_OPEN State** (Testing Recovery):
   - Allows limited requests (max 3)
   - If successful: transitions to CLOSED
   - If failed: transitions back to OPEN

## Graceful Shutdown

When receiving SIGTERM or SIGINT:

1. **Signal Handler** receives signal
2. **Stop accepting** new messages from Central
3. **Process remaining** transactions in queue (with timeout)
4. **Close connections**:
   - TCP connection to Central
   - HTTP connections to PROCR
5. **Clear caches** and queues
6. **Exit cleanly** with status code 0

## Monitoring & Debugging

### Key Metrics
- Queue size (target: < 50)
- Cache size (target: < 100)
- Circuit breaker state (target: CLOSED)
- Session duration
- Transaction processing time

### Debug Logging
Enable with:
```properties
logging.level.com.gracefulshutdown.adapter=DEBUG
```

Key log points:
- Transaction received from Central
- Transaction enqueued
- JSON ↔ YAML conversions
- PROCR POST request/response
- Response cached
- Response sent to Central
- Circuit breaker state changes
- Retry attempts

## Performance Tuning

### Default Configuration
- 2 processor threads: Handles ~10 txn/sec
- 100 queue capacity: Buffer for burst
- 30s cache TTL: Balance memory/performance
- 3 retry attempts: Balance reliability/latency

### Recommendations

**For High Throughput** (> 100 txn/sec):
```properties
adapter.processor.thread_count=4
adapter.queue.capacity=200
adapter.retry.max_attempts=2
adapter.retry.max_delay_ms=1000
```

**For Low Latency** (< 1s per transaction):
```properties
adapter.processor.thread_count=1
adapter.queue.capacity=50
adapter.retry.max_attempts=1
adapter.retry.initial_delay_ms=50
```

**For Reliability** (no loss):
```properties
adapter.processor.thread_count=2
adapter.queue.capacity=500
adapter.retry.max_attempts=5
adapter.circuit_breaker.failure_threshold=10
adapter.circuit_breaker.timeout_ms=60000
```

## Troubleshooting

### Problem: Adapter won't start
- Check PROCR is running on port 8080
- Check port 5000 is not in use: `lsof -i :5000`
- Check logs for errors: `tail -f adapter.log`

### Problem: Transactions not processing
- Check circuit breaker state: `curl http://localhost:8081/health/detailed`
- If OPEN: PROCR is down or failed 5+ times
- Check PROCR logs for errors

### Problem: Slow processing
- Check queue size: `curl http://localhost:8081/metrics`
- If queue > 80%: increase processor threads
- Check PROCR response time

### Problem: Memory leak
- Monitor cache size: `curl http://localhost:8081/metrics`
- Reduce TTL if cache growing: `adapter.cache.ttl_ms=10000`

## Testing

### Unit Tests
```bash
mvn test -DskipTests=false
```

Tests included:
- CircuitBreakerTest: State machine transitions
- RetryPolicyTest: Exponential backoff logic
- ResponseCacheTest: TTL and expiration

### Integration Test (Manual)
```bash
# Terminal 1: Start PROCR
cd procr && java -jar target/*.jar

# Terminal 2: Start Adapter
cd adapter && java -jar target/*.jar

# Terminal 3: Start Central
cd central && java -jar target/*.jar

# Terminal 4: Monitor
watch -n 1 'curl -s http://localhost:8081/health/detailed | jq .'
```

## Files & Classes

### Core Orchestration
- `AdapterApplication.java` - Spring Boot entry point
- `AdapterService.java` - Spring lifecycle management
- `ShutdownHookManager.java` - Graceful shutdown

### Network
- `TcpServer.java` - TCP server (listening)
- `TcpMessageSender.java` - TCP client (sending)
- `SenderThread.java` - Read JSON from Central
- `ProcrClient.java` - HTTP client to PROCR

### Processing
- `ProcessorThread.java` - Transaction processor
- `AdapterOrchestrator.java` - Thread coordinator

### Queuing & Caching
- `TransactionQueue.java` / `TransactionQueueImpl.java`
- `ResponseCache.java`

### State Management
- `SessionManager.java` - Session state machine
- `AdapterState.java` - State enum

### Resilience
- `CircuitBreaker.java` - Circuit breaker pattern
- `RetryPolicy.java` - Retry logic
- `ErrorHandler.java` - Error classification

### REST
- `HealthController.java` - Health & metrics endpoints

### Utilities
- `JsonYamlConverter.java` / `JsonYamlConverterImpl.java` - Format conversion
- `YamlParser.java` - YAML parsing
- `AdapterConfig.java` - Spring configuration

## Deployment

### Docker
```dockerfile
FROM openjdk:21-slim
COPY target/adapter-json-yaml-converter-1.0.0.jar /app.jar
EXPOSE 5000 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: adapter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: adapter
  template:
    metadata:
      labels:
        app: adapter
    spec:
      containers:
      - name: adapter
        image: adapter-json-yaml-converter:1.0.0
        ports:
        - containerPort: 5000
        - containerPort: 8081
        env:
        - name: ADAPTER_PROCR_URL
          value: "http://procr:8080"
        livenessProbe:
          httpGet:
            path: /health
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/detailed
            port: 8081
          initialDelaySeconds: 5
          periodSeconds: 5
```

## Related Documentation

- [Central Service Checklist](../IMPLEMENTATION_CHECKLIST.md)
- [PROCR Service Guide](../PROCR_IMPLEMENTATION_CHECKLIST.md)
- [Master Implementation Checklist](../MASTER_IMPLEMENTATION_CHECKLIST.md)
- [README](../README.md)

---

**Last Updated**: October 23, 2024  
**Status**: ✅ IMPLEMENTATION COMPLETE (Phases 1-14)  
**Build**: ✅ JAR 21 MB, 0 errors, 100% success
