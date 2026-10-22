# Adapter Service - Implementation Checklist

## 📋 Project Overview

**Purpose**: Convert JSON messages from Central to YAML for PROCR, and convert YAML responses back to JSON.

**Architecture**:
```
Central (JSON/TCP:5000) 
  → Adapter (Converter)
    → PROCR (YAML/HTTP:8080)
```

**Implementation Status**: ⏳ NOT STARTED (Specification Complete)

---

## Phase 1: Project Setup

### Maven Project Structure
- [ ] Create Maven project directory: `adapter/`
- [ ] Create `adapter/pom.xml` with:
  - [ ] Java 26 compatibility
  - [ ] Spring Boot 3.4.0 dependency
  - [ ] Jackson (JSON) dependency
  - [ ] SnakeYAML dependency
  - [ ] SLF4J logging dependency
  - [ ] Maven compiler plugin (java 26)
- [ ] Create directory structure:
  - [ ] `src/main/java/com/gracefulshutdown/adapter/`
  - [ ] `src/main/resources/`
  - [ ] `src/test/java/`
- [ ] Create `application.properties` file

### Spring Boot Configuration
- [ ] Create `AdapterApplication.java` with `@SpringBootApplication`
- [ ] Create `AdapterConfig.java` with `@Configuration`
- [ ] Configure logging (SLF4J with application.properties)
- [ ] Set port to 5000 (listening for Central)
- [ ] Configure Jackson ObjectMapper bean

---

## Phase 2: Domain Models & Converters

### Domain Models
- [ ] Create `Transaction.java` POJO
  - [ ] Fields: txn_id, stan, type, narration, quantity, rate, taxes, total, timestamp
  - [ ] Getters/setters
  - [ ] toString() method
- [ ] Create `SessionMessage.java` POJO
  - [ ] Field: message_type
  - [ ] Support: SESSION_START, SESSION_END
- [ ] Create `Response.java` POJO
  - [ ] Fields: response_type, txn_id, status, message, timestamp
  - [ ] Getters/setters

### JSON/YAML Converters
- [ ] Create `JsonYamlConverter.java` interface with methods:
  - [ ] `transactionToYaml(Transaction): String`
  - [ ] `yamlToResponse(String): Response`
  - [ ] `sessionMessageToYaml(SessionMessage): String`
- [ ] Create `JsonYamlConverterImpl.java` implementing:
  - [ ] Use ObjectMapper for JSON parsing
  - [ ] Use SnakeYAML for YAML generation
  - [ ] Add "---" delimiter for PROCR
  - [ ] Handle type conversions (numbers, strings, booleans)
  - [ ] Add comprehensive error handling
  - [ ] Add logging for all conversions
- [ ] Create unit tests for converter:
  - [ ] Test JSON to YAML conversion
  - [ ] Test YAML to JSON conversion
  - [ ] Test type preservation
  - [ ] Test special characters/escaping

---

## Phase 3: TCP Server (Incoming)

### TCP Connection Handler
- [ ] Create `TcpServer.java` with:
  - [ ] ServerSocket on port 5000
  - [ ] Accept single client connection
  - [ ] Log connection establishment (with timestamp and IP)
  - [ ] Handle connection closure gracefully
  - [ ] Thread-safe socket operations
- [ ] Create `TcpServerThread.java`:
  - [ ] Extend Thread
  - [ ] Run ServerSocket.accept() in loop
  - [ ] Handle client connections
  - [ ] Pass socket to message handler
  - [ ] Log all connection events

### Message Reader
- [ ] Create `TcpMessageReader.java`:
  - [ ] Read JSON messages line-by-line from socket
  - [ ] Buffer messages until complete JSON object received
  - [ ] Detect JSON object boundaries ({}  or [])
  - [ ] Parse JSON with ObjectMapper
  - [ ] Route to appropriate handler:
    - [ ] SESSION_START messages
    - [ ] SESSION_END messages
    - [ ] Transaction messages
  - [ ] Log all received messages
  - [ ] Handle read timeouts
  - [ ] Handle connection errors

### Message Sender
- [ ] Create `TcpMessageSender.java`:
  - [ ] Send JSON acknowledgments back to Central
  - [ ] Format: `{"response_type":"APPROVE","txn_id":"TXN-xxx",...}`
  - [ ] One message per line
  - [ ] Flush after each send
  - [ ] Log all sent messages
  - [ ] Handle write errors
  - [ ] Use synchronized writes (prevent corruption)

---

## Phase 4: HTTP Client (Outgoing to PROCR)

### PROCR HTTP Client
- [ ] Create `ProcrClient.java`:
  - [ ] Use RestTemplate or WebClient (Spring)
  - [ ] Configure base URL: `http://localhost:8080` (configurable)
  - [ ] Implement `postTransaction(String yamlTransaction): String`
    - [ ] POST to `/process` endpoint
    - [ ] Set Content-Type: `text/yaml`
    - [ ] Set timeout: 10 seconds (configurable)
    - [ ] Handle HTTP 200 response
    - [ ] Parse YAML response
    - [ ] Log request and response
  - [ ] Implement error handling:
    - [ ] HTTP timeout: log error, discard transaction
    - [ ] HTTP error (4xx/5xx): log error, discard
    - [ ] Connection refused: log error, buffer or discard
    - [ ] Retry strategy: None (fail fast)

### Health Check (Optional)
- [ ] Create method to check PROCR availability:
  - [ ] GET `/health` endpoint
  - [ ] Log health status
  - [ ] Use for connection readiness checks

---

## Phase 5: Message Queues

### Transaction Queue
- [ ] Create `TransactionQueue.java` interface:
  - [ ] `enqueue(Transaction): void`
  - [ ] `dequeue(): Transaction`
  - [ ] `size(): int`
  - [ ] `isEmpty(): boolean`
- [ ] Create `TransactionQueueImpl.java`:
  - [ ] Use BlockingQueue<Transaction> with capacity 100
  - [ ] Enqueue blocks if queue full
  - [ ] Dequeue blocks if queue empty
  - [ ] Log queue size when exceeding 80 items
  - [ ] Implement clear() for shutdown
  - [ ] Thread-safe operations

### Response Cache (Optional)
- [ ] Create `ResponseCache.java`:
  - [ ] Store responses by txn_id temporarily
  - [ ] Hold until Central receives acknowledgment
  - [ ] TTL: 30 seconds (configurable)
  - [ ] For debugging/retry scenarios

---

## Phase 6: State Management

### Adapter State Machine
- [ ] Create `AdapterState.java` enum:
  - [ ] DISCONNECTED (initial)
  - [ ] LISTENING (TCP server running)
  - [ ] ACTIVE (SESSION_START received)
  - [ ] STOPPING (SESSION_END received)
  - [ ] ERROR (connection failed)
- [ ] Create `StateManager.java`:
  - [ ] AtomicReference<AdapterState> for thread-safe transitions
  - [ ] Methods: getCurrentState(), transitionTo(newState)
  - [ ] Validate allowed transitions
  - [ ] Log all state changes with timestamp
  - [ ] Restrict actions based on state:
    - [ ] Only accept transactions in ACTIVE state
    - [ ] Only process queue in ACTIVE/STOPPING states

### Session Management
- [ ] Create `SessionManager.java`:
  - [ ] Track current session status
  - [ ] Handle SESSION_START messages
  - [ ] Handle SESSION_END messages
  - [ ] Coordinate with StateManager
  - [ ] Clear queue on SESSION_END (after processing remaining items)

---

## Phase 7: Threading

### Sender Thread (Central → Queue)
- [ ] Create `SenderThread.java`:
  - [ ] Read from TcpMessageReader
  - [ ] Classify message type (SESSION_START/END, Transaction)
  - [ ] For transactions: convert JSON → YAML, enqueue
  - [ ] For sessions: update SessionManager state
  - [ ] Log all incoming messages
  - [ ] Handle parse errors gracefully
  - [ ] Run until SESSION_END received

### Processor Thread (Queue → PROCR → Central)
- [ ] Create `ProcessorThread.java`:
  - [ ] Dequeue transactions from queue
  - [ ] Convert to YAML (JsonYamlConverter)
  - [ ] POST to PROCR via ProcrClient
  - [ ] Receive YAML response
  - [ ] Convert response back to JSON
  - [ ] Send JSON acknowledgment to Central via TcpMessageSender
  - [ ] Log each processing step
  - [ ] Handle timeouts and errors
  - [ ] Continue processing until queue empty and SESSION_END received

### Main Orchestrator
- [ ] Create `AdapterOrchestrator.java`:
  - [ ] Manage thread lifecycle
  - [ ] Start TcpServer on app startup
  - [ ] Start SenderThread
  - [ ] Start ProcessorThread
  - [ ] Graceful shutdown on app close
  - [ ] Coordinate state transitions between threads
  - [ ] Log all lifecycle events

---

## Phase 8: Spring Integration

### Spring Boot Components
- [ ] Create `@Component` for TcpServer
- [ ] Create `@Component` for StateManager
- [ ] Create `@Component` for SessionManager
- [ ] Create `@Component` for ProcrClient
- [ ] Create `@Bean` for ObjectMapper
- [ ] Create `@Bean` for SnakeYAML Yaml instance
- [ ] Create `@Bean` for TransactionQueue

### Lifecycle Management
- [ ] Create `@PostConstruct` method:
  - [ ] Initialize TCP server
  - [ ] Start sender thread
  - [ ] Start processor thread
  - [ ] Log startup completion
- [ ] Create `@PreDestroy` method:
  - [ ] Graceful shutdown
  - [ ] Stop threads
  - [ ] Close TCP connections
  - [ ] Clear queues
  - [ ] Log shutdown completion

### Health Endpoint (Optional)
- [ ] Create REST endpoint: GET `/health`
- [ ] Return status:
  - [ ] Adapter state (ACTIVE/DISCONNECTED/etc)
  - [ ] Queue size
  - [ ] PROCR connectivity status
  - [ ] Central connectivity status
- [ ] Format: JSON or YAML

---

## Phase 9: Logging & Monitoring

### Logging Implementation
- [ ] Use SLF4J throughout
- [ ] Create logger for each class
- [ ] Log levels:
  - [ ] DEBUG: Message details (contents, fields)
  - [ ] INFO: State transitions, connections, disconnections
  - [ ] WARN: Queue near capacity (>80 items), conversion issues
  - [ ] ERROR: Connection failures, parse errors, HTTP errors

### Key Log Points
- [ ] TCP connection established (IP, port)
- [ ] TCP connection closed
- [ ] SESSION_START received
- [ ] SESSION_END received
- [ ] Transaction received: `{txn_id, type, quantity, total}`
- [ ] JSON to YAML conversion
- [ ] YAML POST to PROCR (URL, content-type)
- [ ] YAML response received: `{response_type, status}`
- [ ] YAML to JSON conversion
- [ ] Acknowledgment sent to Central
- [ ] Queue size updates (especially when high)
- [ ] Timeout/error events with stack trace
- [ ] State transitions with timestamp

### Configuration
- [ ] Add to application.properties:
  ```properties
  logging.level.root=INFO
  logging.level.com.gracefulshutdown.adapter=DEBUG
  logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
  ```

---

## Phase 10: Configuration

### application.properties
- [ ] Create with all configurable parameters:
  ```properties
  server.port=5000
  adapter.central.port=5000
  adapter.procr.url=http://localhost:8080
  adapter.procr.process.endpoint=/process
  adapter.procr.timeout=10000
  adapter.queue.capacity=100
  adapter.queue.warn.threshold=80
  logging.level.root=INFO
  logging.level.com.gracefulshutdown.adapter=DEBUG
  ```
- [ ] Document each property
- [ ] Support environment variable overrides (optional)

### Configuration Class
- [ ] Create `AdapterProperties.java` with `@ConfigurationProperties`
- [ ] Add all parameters as properties
- [ ] Add getters/setters
- [ ] Use `@Value` annotations in classes

---

## Phase 11: Error Handling

### Connection Errors
- [ ] Handle Central disconnect:
  - [ ] Log error
  - [ ] Transition to DISCONNECTED state
  - [ ] Close PROCR connection
  - [ ] Clear queues
  - [ ] Shutdown gracefully
- [ ] Handle PROCR unavailable:
  - [ ] Log error (first time only to avoid spam)
  - [ ] Buffer transactions in queue (up to capacity)
  - [ ] Retry connection on next POST attempt
  - [ ] Discard oldest transaction if queue full

### Parse Errors
- [ ] Handle invalid JSON from Central:
  - [ ] Log error with content sample
  - [ ] Skip message
  - [ ] Continue reading next message
  - [ ] Don't crash
- [ ] Handle invalid YAML from PROCR:
  - [ ] Log error
  - [ ] Discard response
  - [ ] Don't send acknowledgment
  - [ ] Continue to next transaction

### HTTP Errors
- [ ] Handle timeout (>10s):
  - [ ] Log timeout warning
  - [ ] Discard transaction
  - [ ] Continue processing next
- [ ] Handle 4xx errors:
  - [ ] Log error with status code
  - [ ] Discard transaction
  - [ ] Don't retry
- [ ] Handle 5xx errors:
  - [ ] Log error
  - [ ] Buffer and retry (optional)
  - [ ] Or discard after N retries

---

## Phase 12: Testing

### Unit Tests
- [ ] Test JsonYamlConverter:
  - [ ] Test JSON→YAML conversion
  - [ ] Test YAML→JSON conversion
  - [ ] Test type preservation (numbers, strings)
  - [ ] Test special characters
  - [ ] Test missing fields (error cases)

- [ ] Test TransactionQueue:
  - [ ] Test enqueue/dequeue
  - [ ] Test capacity limit
  - [ ] Test blocking behavior

- [ ] Test StateManager:
  - [ ] Test state transitions
  - [ ] Test invalid transitions (error cases)
  - [ ] Test concurrent access

### Integration Tests (Manual)
- [ ] Start Central server
- [ ] Start Adapter
- [ ] Connect from Central
- [ ] Send SESSION_START
- [ ] Send 5 test transactions
- [ ] Verify conversions logged correctly
- [ ] Verify PROCR receives YAML
- [ ] Verify responses received
- [ ] Verify acknowledgments sent to Central
- [ ] Send SESSION_END
- [ ] Verify graceful shutdown
- [ ] Check logs for errors

### Load Tests (Optional)
- [ ] Send 100 transactions in rapid succession
- [ ] Verify queue doesn't overflow
- [ ] Verify no messages are lost
- [ ] Check memory usage
- [ ] Monitor thread count

---

## Phase 13: Documentation

### Code Documentation
- [ ] Add class-level JavaDoc comments
- [ ] Add method-level JavaDoc comments
- [ ] Document configuration parameters
- [ ] Add README.md for Adapter:
  - [ ] Architecture overview
  - [ ] Build instructions
  - [ ] Run instructions
  - [ ] Configuration guide
  - [ ] Troubleshooting

### API Documentation
- [ ] Document TCP message formats (JSON examples)
- [ ] Document HTTP endpoints used (PROCR)
- [ ] Document error responses
- [ ] Document configuration options

### Deployment Guide (Optional)
- [ ] Docker build instructions
- [ ] Environment variables
- [ ] Systemd service file (for Linux)
- [ ] Monitoring recommendations

---

## Phase 14: Build & Deployment

### Maven Build
- [ ] Execute: `mvn clean package`
- [ ] Verify: JAR file created in `target/`
- [ ] Verify: Size reasonable (< 100MB)
- [ ] Verify: No compilation errors

### Run & Verify
- [ ] Execute: `java -jar target/adapter-*.jar`
- [ ] Verify: Server starts and listens on port 5000
- [ ] Verify: Logs show startup messages
- [ ] Verify: Ready for client connections

### Artifact
- [ ] Build successful JAR: `adapter-transaction-converter-1.0.0.jar`
- [ ] Executable with all dependencies
- [ ] Ready for deployment

---

## Implementation Order (Recommended)

1. **Setup** (Phase 1-2): Maven project + domain models + converters
2. **Network** (Phase 3-4): TCP server + HTTP client
3. **Core Logic** (Phase 5-7): Queues + state management + threading
4. **Integration** (Phase 8-9): Spring lifecycle + logging
5. **Configuration** (Phase 10): Properties + config class
6. **Robustness** (Phase 11-12): Error handling + tests
7. **Finalization** (Phase 13-14): Documentation + build

---

## Dependencies to Add

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.0</version>
</dependency>

<!-- Jackson (JSON) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
</dependency>

<!-- SnakeYAML (YAML) -->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>

<!-- SLF4J Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>

<!-- Logback (SLF4J implementation) -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>
```

---

## Success Criteria

- ✅ Adapter connects to Central on port 5000
- ✅ Receives SESSION_START and transitions to ACTIVE state
- ✅ Receives JSON transactions from Central
- ✅ Converts JSON to YAML format
- ✅ POSTs YAML to PROCR /process endpoint
- ✅ Receives YAML responses from PROCR
- ✅ Converts YAML responses back to JSON
- ✅ Sends JSON acknowledgments to Central
- ✅ Receives SESSION_END and gracefully shuts down
- ✅ All operations logged comprehensively
- ✅ No message loss
- ✅ No crashes or hangs
- ✅ JAR builds successfully
- ✅ Runs without errors

---

**Status**: ⏳ READY FOR IMPLEMENTATION

**Reference Documentation**: `/data/code/gt/gracefulShutdown/adapter/docs/req.html`

**Last Updated**: October 23, 2024
