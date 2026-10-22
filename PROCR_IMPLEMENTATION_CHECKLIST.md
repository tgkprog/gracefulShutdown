# PROCR Service - Implementation Checklist

## 📋 Project Overview

**Purpose**: Process transactions with approval/rejection logic, configurable response delays, and comprehensive logging.

**Architecture**:
```
PROCR Service (Transaction Processor)
  ├─ Input: YAML over HTTP (POST /process)
  ├─ Processing: Approve/Reject with delay
  └─ Output: YAML over HTTP (HTTP 200)
```

**Implementation Status**: ⏳ NOT STARTED (Specification Complete)

---

## Phase 1: Project Setup

### Maven Project Structure
- [ ] Create Maven project directory: `procr/`
- [ ] Create `procr/pom.xml` with:
  - [ ] Java 26 compatibility
  - [ ] Spring Boot 3.4.0 dependency
  - [ ] SnakeYAML dependency (YAML support)
  - [ ] SLF4J logging dependency
  - [ ] Maven compiler plugin (java 26)
- [ ] Create directory structure:
  - [ ] `src/main/java/com/gracefulshutdown/procr/`
  - [ ] `src/main/resources/`
  - [ ] `src/test/java/`
- [ ] Create `application.properties` file

### Spring Boot Configuration
- [ ] Create `ProcrApplication.java` with `@SpringBootApplication`
- [ ] Create `ProcrConfig.java` with `@Configuration`
- [ ] Configure logging (SLF4J with application.properties)
- [ ] Set port to 8080 (HTTP server)
- [ ] Configure YAML message handling
- [ ] Configure Jackson/SnakeYAML beans

---

## Phase 2: Domain Models

### Transaction Model
- [ ] Create `Transaction.java` POJO:
  - [ ] Fields: txn_id, stan, type, narration, quantity, rate, taxes, total, timestamp
  - [ ] Getters/setters
  - [ ] toString() method
  - [ ] Use for deserializing incoming YAML

### Response Model
- [ ] Create `Response.java` POJO:
  - [ ] Fields: response_type (APPROVE/REJECT), txn_id, status (SUCCESS/FAILED), message, timestamp
  - [ ] Getters/setters
  - [ ] toString() method
  - [ ] Use for serializing outgoing YAML

### Rejection Reason Enum
- [ ] Create `RejectionReason.java` enum:
  - [ ] INSUFFICIENT_FUNDS
  - [ ] CARD_CLOSED
  - [ ] NETWORK_DOWN
  - [ ] Each with descriptive message
  - [ ] Random selection (1% rejection scenarios)

---

## Phase 3: Business Logic

### Approval Decision Engine
- [ ] Create `ApprovalEngine.java`:
  - [ ] Method: `decideApproval(): boolean`
    - [ ] Generate random number (1-100)
    - [ ] Return true if <= 99 (APPROVE)
    - [ ] Return false if == 100 (REJECT)
  - [ ] Method: `selectRejectionReason(): RejectionReason`
    - [ ] Random selection from 3 reasons
    - [ ] Equal probability (33.3% each)
  - [ ] Add comprehensive logging:
    - [ ] Log generated random number
    - [ ] Log decision (APPROVE/REJECT)
    - [ ] Log rejection reason if applicable

### Response Delay Calculator
- [ ] Create `ResponseDelayCalculator.java`:
  - [ ] Method: `calculateDelay(config): long`
    - [ ] Formula: `sleep_ms = fixed_delay + random_value`
    - [ ] Parameters from config:
      - [ ] fixed_delay (default: 0 ms)
      - [ ] min_random (default: 0 ms)
      - [ ] max_random (default: 200 ms)
      - [ ] step (default: 100 ms)
    - [ ] Logic:
      1. Generate possible values: [min, min+step, min+2*step, ..., max]
      2. Randomly select one value from this list
      3. Add to fixed_delay
      4. Return total
  - [ ] Examples:
    - [ ] Example 1: Default (0, 0, 100, 200)
      - [ ] Possible: 0, 100, 200 ms (33.3% each)
    - [ ] Example 2: Custom (1000, 100, 100, 1000)
      - [ ] Possible: 1100-2000 ms in 100ms steps
  - [ ] Add comprehensive logging:
    - [ ] Log fixed_delay value
    - [ ] Log min/step/max values
    - [ ] Log calculated random_value
    - [ ] Log total sleep_ms calculated

### Transaction Processor
- [ ] Create `TransactionProcessor.java`:
  - [ ] Method: `process(Transaction): Response`
    - [ ] Step 1: Log "Processing transaction" (txn_id, type, narration, amount)
    - [ ] Step 2: Decide approval (use ApprovalEngine)
    - [ ] Step 3: Log decision ("Decided: APPROVE/REJECT")
    - [ ] Step 4: Calculate delay (use ResponseDelayCalculator)
    - [ ] Step 5: Log delay ("Calculated delay: XXX ms")
    - [ ] Step 6: Sleep for calculated duration
    - [ ] Step 7: Log sleep completion ("Sleep completed")
    - [ ] Step 8: Create Response object
    - [ ] Step 9: Log response ("Responding: APPROVE/REJECT")
    - [ ] Step 10: Return Response
  - [ ] Error handling:
    - [ ] Catch exceptions during processing
    - [ ] Log error with stack trace
    - [ ] Return REJECT response with error message

---

## Phase 4: REST Endpoints

### Health Endpoint
- [ ] Create `HealthController.java`:
  - [ ] Endpoint: `GET /health`
  - [ ] Returns: Health status in YAML format
  - [ ] Response format:
    ```yaml
    status: UP
    service: PROCR
    timestamp: 2024-10-23T12:34:56Z
    version: 1.0.0
    ```
  - [ ] Response type: `text/yaml`
  - [ ] Always returns HTTP 200

### Process Endpoint
- [ ] Create `ProcessController.java`:
  - [ ] Endpoint: `POST /process`
  - [ ] Content-Type: `text/yaml` (input)
  - [ ] Accept: `text/yaml` (output)
  - [ ] Request body: YAML-formatted Transaction
  - [ ] Response body: YAML-formatted Response
  - [ ] HTTP status: 200 OK on success
  - [ ] Implementation:
    - [ ] Parse YAML request body to Transaction
    - [ ] Call TransactionProcessor.process()
    - [ ] Serialize Response to YAML
    - [ ] Return YAML response with 200 status
  - [ ] Error handling:
    - [ ] Invalid YAML input: Log error, return HTTP 400
    - [ ] Missing required fields: Log error, return HTTP 400
    - [ ] Processing exception: Log error, return HTTP 500 with error message

---

## Phase 5: YAML Serialization/Deserialization

### YAML Parser
- [ ] Create `YamlParser.java`:
  - [ ] Method: `parseTransaction(String yamlContent): Transaction`
    - [ ] Use SnakeYAML Yaml instance
    - [ ] Parse YAML string to Map
    - [ ] Map fields to Transaction object
    - [ ] Handle missing fields with defaults or errors
    - [ ] Log parsed transaction details
  - [ ] Error handling:
    - [ ] Catch YAMLException
    - [ ] Log error with YAML content sample
    - [ ] Throw ParseException

### YAML Serializer
- [ ] Create `YamlSerializer.java`:
  - [ ] Method: `serializeResponse(Response): String`
    - [ ] Use SnakeYAML Yaml instance
    - [ ] Convert Response object to Map
    - [ ] Serialize to YAML string
    - [ ] Append "---" delimiter for multi-message streams
    - [ ] Log serialized YAML
  - [ ] Ensure field order for readability:
    - [ ] response_type first
    - [ ] txn_id second
    - [ ] status third
    - [ ] message fourth
    - [ ] timestamp fifth

### Content Negotiation
- [ ] Configure Spring to handle YAML:
  - [ ] Use HttpMessageConverter for text/yaml
  - [ ] Register SnakeYAML converter in WebConfig
  - [ ] Ensure @RequestBody and @ResponseBody work with YAML

---

## Phase 6: Logging & Monitoring

### Logging Implementation
- [ ] Use SLF4J throughout all classes
- [ ] Create logger for each class (private static final)
- [ ] Logging levels:
  - [ ] DEBUG: Detailed values (exact random numbers, delays)
  - [ ] INFO: State transitions (APPROVE/REJECT, sleep start/complete)
  - [ ] WARN: Unusual conditions (missing fields, defaults used)
  - [ ] ERROR: Failures (parse errors, processing exceptions)

### Key Log Points (Per Transaction)
- [ ] Request received:
  ```
  INFO: Received transaction: txn_id=TXN-123456, type=T1, quantity=50, total=1402.50
  ```
- [ ] Decision made:
  ```
  DEBUG: Generated random: 45
  INFO: Decided: APPROVE
  ```
- [ ] Delay calculated:
  ```
  DEBUG: fixed_delay=0, min_random=0, step=100, max_random=200
  DEBUG: Selected random_value=100
  DEBUG: Total sleep_ms=100
  INFO: Calculated delay: 100ms
  ```
- [ ] Sleep phase:
  ```
  INFO: Sleeping for 100ms
  DEBUG: Sleep started at HH:MM:SS.mmm
  DEBUG: Sleep completed at HH:MM:SS.mmm
  INFO: Sleep completed
  ```
- [ ] Response phase:
  ```
  INFO: Responding: APPROVE for TXN-123456
  DEBUG: Response JSON: {"response_type":"APPROVE",...}
  ```

### Log Configuration
- [ ] Add to `application.properties`:
  ```properties
  logging.level.root=INFO
  logging.level.com.gracefulshutdown.procr=DEBUG
  logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
  ```
- [ ] File logging (optional):
  - [ ] Log to `logs/procr.log`
  - [ ] Rolling file appender (10 MB per file, 10 files max)

---

## Phase 7: Configuration

### application.properties
- [ ] Create with configurable parameters:
  ```properties
  server.port=8080
  procr.response.delay.fixed=0
  procr.response.delay.min_random=0
  procr.response.delay.step=100
  procr.response.delay.max_random=200
  logging.level.root=INFO
  logging.level.com.gracefulshutdown.procr=DEBUG
  ```

### Configuration Class
- [ ] Create `ProcrProperties.java`:
  - [ ] Use `@ConfigurationProperties(prefix="procr")`
  - [ ] Nested class `ResponseDelay` for delay parameters
  - [ ] Fields:
    - [ ] fixed (fixed_delay)
    - [ ] minRandom (min_random)
    - [ ] step
    - [ ] maxRandom (max_random)
  - [ ] Getters/setters for all fields
  - [ ] Validation:
    - [ ] Ensure fixed >= 0
    - [ ] Ensure minRandom >= 0
    - [ ] Ensure step > 0
    - [ ] Ensure maxRandom >= minRandom

### Environment Variable Support (Optional)
- [ ] Allow override via environment variables:
  - [ ] `PROCR_RESPONSE_DELAY_FIXED=0`
  - [ ] `PROCR_RESPONSE_DELAY_MIN_RANDOM=0`
  - [ ] `PROCR_RESPONSE_DELAY_STEP=100`
  - [ ] `PROCR_RESPONSE_DELAY_MAX_RANDOM=200`

---

## Phase 8: Error Handling & Validation

### Input Validation
- [ ] Validate incoming Transaction:
  - [ ] Check required fields: txn_id, type, narration
  - [ ] Check numeric fields: quantity, rate, taxes, total
  - [ ] Check timestamp format (ISO 8601)
  - [ ] Log validation errors with field names
  - [ ] Return HTTP 400 if invalid

### Processing Errors
- [ ] Handle processing exceptions:
  - [ ] ApprovalEngine throws: Handle gracefully, log error
  - [ ] ResponseDelayCalculator throws: Return default delay, log warning
  - [ ] Sleep interrupted: Log warning, continue with response
  - [ ] All exceptions logged with full stack trace

### Response Errors
- [ ] Handle serialization errors:
  - [ ] YAML serialization fails: Log error, return HTTP 500
  - [ ] Response fields invalid: Log error, return HTTP 500

---

## Phase 9: Threading & Performance

### Thread Pool (Optional Enhancement)
- [ ] Consider ThreadPoolExecutor for parallel processing:
  - [ ] Core pool size: 2 (configurable)
  - [ ] Max pool size: 10 (configurable)
  - [ ] Queue capacity: 100
  - [ ] Each request processed in thread pool
  - [ ] Log thread information in logs

### Non-Blocking Sleep (Optional)
- [ ] Current: Thread.sleep() (blocking)
- [ ] Alternative: ScheduledExecutorService (async)
- [ ] For now, blocking sleep is acceptable

### Connection Pool (Optional)
- [ ] If using RestTemplate for outgoing calls
- [ ] Configure connection pool size
- [ ] Configure timeout values

---

## Phase 10: Spring Integration

### Spring Boot Components
- [ ] Create `@RestController` for endpoints
- [ ] Create `@Service` for business logic (processors, engines)
- [ ] Create `@Component` for utilities (YAML parser/serializer)
- [ ] Create `@Bean` for SnakeYAML Yaml instance
- [ ] Create `@Configuration` for HttpMessageConverters (YAML support)

### Lifecycle Management
- [ ] Use Spring's startup/shutdown hooks (optional):
  - [ ] Log application start
  - [ ] Log application stop
  - [ ] Verify all threads shutdown cleanly

---

## Phase 11: Testing

### Unit Tests
- [ ] Test ApprovalEngine:
  - [ ] Test decideApproval() with mocked random
  - [ ] Verify ~99% return true, ~1% return false
  - [ ] Test selectRejectionReason() with mocked random
  - [ ] Verify equal distribution of 3 reasons

- [ ] Test ResponseDelayCalculator:
  - [ ] Test with default config (0, 0, 100, 200)
    - [ ] Verify result is 0, 100, or 200
  - [ ] Test with custom config (1000, 100, 100, 1000)
    - [ ] Verify result is 1100-2000 in 100ms steps
  - [ ] Test with single-value config (fixed=500, others invalid)
    - [ ] Verify result is 500

- [ ] Test TransactionProcessor:
  - [ ] Test successful approval
  - [ ] Test successful rejection
  - [ ] Test with various transaction amounts
  - [ ] Test error handling

- [ ] Test YamlParser:
  - [ ] Test parsing valid YAML
  - [ ] Test parsing invalid YAML
  - [ ] Test handling missing fields
  - [ ] Test type conversions

- [ ] Test YamlSerializer:
  - [ ] Test serializing valid Response
  - [ ] Test field order in output
  - [ ] Test special characters handling

### Integration Tests (Manual with Adapter)
- [ ] Start PROCR server
- [ ] Send test YAML transaction via HTTP POST
- [ ] Verify response received in YAML format
- [ ] Verify response_type is APPROVE or REJECT
- [ ] Verify txn_id matches request
- [ ] Send 100 rapid requests
- [ ] Verify ~99 APPROVE, ~1 REJECT
- [ ] Verify response delays are applied (check via logs)
- [ ] Send invalid YAML
- [ ] Verify HTTP 400 response
- [ ] Verify logs show errors

### Load Tests (Optional)
- [ ] Send 1000 requests in succession
- [ ] Measure response times
- [ ] Verify all responses received
- [ ] Check memory usage
- [ ] Monitor CPU usage

---

## Phase 12: Documentation

### Code Documentation
- [ ] Add class-level JavaDoc comments
- [ ] Add method-level JavaDoc comments:
  - [ ] @param for parameters
  - [ ] @return for return values
  - [ ] @throws for exceptions
- [ ] Document configuration parameters
- [ ] Add README.md for PROCR:
  - [ ] Architecture overview
  - [ ] Build instructions
  - [ ] Run instructions
  - [ ] Configuration guide
  - [ ] API documentation
  - [ ] Examples

### API Documentation
- [ ] Document GET /health endpoint:
  - [ ] Request format
  - [ ] Response format (YAML)
  - [ ] Response codes
- [ ] Document POST /process endpoint:
  - [ ] Request format (YAML Transaction)
  - [ ] Response format (YAML Response)
  - [ ] Response codes (200, 400, 500)
  - [ ] Examples with YAML

### Configuration Guide
- [ ] Document all configuration parameters
- [ ] Explain approval logic (99%/1%)
- [ ] Explain response delay formula
- [ ] Provide example configurations:
  - [ ] Default (low delay)
  - [ ] Realistic (1-2 second delays)
  - [ ] Heavy load (5+ second delays)

---

## Phase 13: Build & Deployment

### Maven Build
- [ ] Execute: `mvn clean package`
- [ ] Verify: JAR file created in `target/`
- [ ] Verify: Size < 100MB
- [ ] Verify: No compilation errors or warnings

### Run & Verify
- [ ] Execute: `java -jar target/procr-*.jar`
- [ ] Verify: Server starts and listens on port 8080
- [ ] Verify: Logs show startup messages
- [ ] Verify: GET /health returns 200 with YAML
- [ ] Verify: Ready for requests

### Artifact
- [ ] Build successful JAR: `procr-transaction-processor-1.0.0.jar`
- [ ] Executable with all dependencies
- [ ] Ready for deployment

---

## Implementation Order (Recommended)

1. **Setup** (Phase 1-2): Maven project + domain models
2. **Business Logic** (Phase 3): Approval engine + delay calculator + processor
3. **REST Endpoints** (Phase 4): /health and /process endpoints
4. **Serialization** (Phase 5): YAML parser and serializer
5. **Integration** (Phase 6-7): Logging + configuration
6. **Error Handling** (Phase 8): Input validation + error responses
7. **Performance** (Phase 9-10): Threading + Spring integration
8. **Testing** (Phase 11): Unit + integration tests
9. **Finalization** (Phase 12-13): Documentation + build

---

## Dependencies to Add

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.0</version>
</dependency>

<!-- SnakeYAML (YAML support) -->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>

<!-- Jackson YAML -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.17.0</version>
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

- ✅ GET /health returns HTTP 200 with YAML status
- ✅ POST /process accepts YAML transaction
- ✅ Decision logic: ~99% APPROVE, ~1% REJECT
- ✅ Rejection reasons randomly selected
- ✅ Response delay applied based on formula
- ✅ Response format: YAML with all required fields
- ✅ Processing flow logged comprehensively:
  - ✅ Transaction received
  - ✅ Decision made
  - ✅ Delay calculated
  - ✅ Sleep started/completed
  - ✅ Response sent
- ✅ Error handling for invalid input (HTTP 400)
- ✅ Error handling for exceptions (HTTP 500)
- ✅ Configuration support for all parameters
- ✅ JAR builds successfully
- ✅ Runs without errors
- ✅ Integrates with Adapter (manual test)

---

## Key Processing Flow (Detailed)

```
Client sends YAML POST to /process
  ↓
Request received in ProcessController
  ↓
Parse YAML to Transaction (YamlParser)
  ↓
Validate Transaction (check required fields)
  ↓
Log: "Received transaction: txn_id=..., type=..., amount=..."
  ↓
Call TransactionProcessor.process(transaction)
  ├─ Generate random(1-100)
  ├─ Decide: true if <=99, false if ==100
  ├─ Log decision: "Decided: APPROVE/REJECT"
  ├─ If REJECT: select random rejection reason
  ├─ Calculate delay:
  │  ├─ Generate possible values [min, min+step, ..., max]
  │  ├─ Randomly select one
  │  ├─ Add to fixed_delay
  │  └─ Log: "Calculated delay: XXXms"
  ├─ Sleep for calculated duration
  ├─ Log: "Sleep completed"
  └─ Create Response object with decision
  ↓
Serialize Response to YAML (YamlSerializer)
  ↓
Return YAML response with HTTP 200
  ↓
Log: "Responding: APPROVE/REJECT for txn_id=..."
  ↓
Client receives YAML response
```

---

**Status**: ⏳ READY FOR IMPLEMENTATION

**Reference Documentation**: 
- `/data/code/gt/gracefulShutdown/procr/docs/req.html`
- `/data/code/gt/gracefulShutdown/procr/docs/implementation_guide.html`

**Last Updated**: October 23, 2024
