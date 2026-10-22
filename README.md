# Graceful Shutdown - Transaction Processing Framework

A distributed transaction processing system with three microservices for handling financial transactions with graceful shutdown capabilities.

## 🔄 System Architecture

```
CENTRAL (JSON)          ADAPTER (Converter)      PROCR (YAML)
   ┌─────────┐             ┌────────┐           ┌──────────┐
   │         │ JSON/TCP    │        │  YAML/HTTP│          │
   │ Sender  │────────────→│Convert │──────────→│ Decision │
   │ Threads │ (port 5000) │JSON→   │ (port 8080)│ Threads  │
   │         │             │  YAML  │           │          │
   └─────────┘             └────────┘           └──────────┘
        ▲                        ▲                    ↓
        │                        │ JSON/HTTP         │
        │        Cache           │ YAML responses    │
        └────────────────────────┴───────────────────┘
             Acknowledgments
```

**Message Flow**:
1. Central generates JSON transactions and sends via TCP
2. Adapter receives JSON, converts to YAML
3. Adapter POSTs YAML to PROCR /process endpoint
4. PROCR processes transaction, returns YAML response
5. Adapter converts YAML back to JSON
6. Adapter sends JSON acknowledgment back to Central
7. Central caches and acknowledges receipt

## 🏗️ Project Structure

```
gracefulShutdown/
├── central/                 # Transaction Generator & Sender
│   ├── docs/
│   │   ├── req.html        # Requirements specification
│   │   └── review.html     # Implementation review
│   ├── src/
│   ├── pom.xml
│   └── target/
│       └── central-transaction-server-1.0.0.jar
├── procr/                  # Transaction Processor
│   └── docs/
│       ├── req.html        # API & Processing requirements
│       └── implementation_guide.html
├── adapter/                # Adapter (planned)
├── README.md               # This file
├── IMPLEMENTATION_CHECKLIST.md
├── LICENSE
└── .gitignore
```

## 📦 Services Overview

### 1. Central Server ✅ (COMPLETE)

Generates and sends transactions to the Adapter via JSON/TCP protocol.

**Status**: Production-ready for integration testing  
**Version**: 1.0.0  
**Build**: 40 MB executable JAR  
**Technology**: Java 26 + Spring Boot 3.4.0

**Key Features**:
- 6-thread concurrent architecture (2 senders, 2 receivers, 1 TCP writer, 1 TCP reader)
- 4-state machine (DISCONNECTED, CONNECTED, ACTIVE, SESSION_END_REQUESTED)
- Rate-limited transaction generation (configurable)
- TTL-based cache with automatic cleanup (9 seconds default)
- JSON message format (converted by Adapter to YAML for PROCR)
- Thread-safe with no concurrent socket writes

**Protocol**: JSON over TCP (port 5000)  
**Adapter Role**: Receives Central's JSON, converts to YAML for PROCR

**Documentation**:
- [Requirements (req.html)](central/docs/req.html)
- [Implementation Review (review.html)](central/docs/review.html)

**Quick Start**:
```bash
cd central
mvn clean package -DskipTests
java -jar target/central-transaction-server-1.0.0.jar
```

Listens on port 5000 (TCP) for Adapter connections.

---

### 2. PROCR ⏳ (IN PROGRESS)

Processes transactions with approval/rejection logic and configurable response delays.

**Status**: Specification complete, ready for implementation  
**Technology**: Spring Boot + YAML  
**Endpoints**:
- `GET /health` - Health check (YAML response)
- `POST /process` - Process transaction (YAML input/output)

**Key Features**:
- 99% approval rate, 1% rejection rate (random)
- Random rejection reasons (Insufficient Funds, Card Closed, Network Down)
- Configurable response delay (fixed + random components)
- No state preservation between requests
- Full logging at each processing stage

**Documentation**:
- [Requirements (req.html)](procr/docs/req.html)
- [Implementation Guide (implementation_guide.html)](procr/docs/implementation_guide.html)

**Processing Flow**:
```
Request → Decide (APPROVE/REJECT)
        → Log decision
        → Calculate sleep duration
        → Sleep (with logging)
        → Send response
```

**Response Delay Formula** (clarified):
```
sleep_ms = fixed_delay + random_value

Where:
  fixed_delay: Base delay applied to every response (ms)
  random_value: Random value from [min_random, max_random] in steps of step (ms)

Example (Default):
  fixed=0, min_random=0, step=100, max_random=200
  → Possible sleep: 0, 100, or 200 ms (each with 33.3% probability)

Example (Custom):
  fixed=1000, min_random=100, step=100, max_random=1000
  → Possible sleep: 1100-2000 ms (1000 + {100-1000 in 100ms steps})
```

**Response Delay Configuration**:
```properties
procr.response.delay.fixed=0           # Base delay (ms)
procr.response.delay.min_random=0      # Min random (ms)
procr.response.delay.step=100          # Step increment (ms)
procr.response.delay.max_random=200    # Max random (ms)
```

---

### 3. Adapter ⏳ (PLANNED)

Bridges Central (JSON) and PROCR (YAML) by converting between formats.

**Status**: Specification in progress, ready for implementation soon  
**Technology**: Spring Boot (planned)  
**Protocols**:
- Input: JSON over TCP (port 5000, from Central)
- Output: YAML over HTTP (port 8080, to PROCR)

**Key Responsibilities**:
- Listen on TCP port 5000 for Central connections
- Receive SESSION_START/END and JSON transactions
- Convert JSON transactions to YAML format
- POST transactions to PROCR `/process` endpoint
- Receive YAML responses from PROCR
- Convert YAML responses back to JSON
- Send JSON acknowledgments back to Central
- Maintain connection state with both services

**Documentation**:
- Specification coming soon (adapter/docs/req.html)

---

## 🚀 Quick Start

### Build All Services
```bash
# Central (only service with build currently)
cd central
mvn clean package -DskipTests
```

### Run Central
```bash
java -jar central/target/central-transaction-server-1.0.0.jar
```

Expected output:
```
TCP Server listening on port 5000
Waiting for client connection...
```

### Integration Testing (when Adapter is ready)
1. Start Central
2. Start Adapter
3. Verify transactions flow (4 per 10-second window by default)
4. Verify acknowledgments are processed
5. Send SESSION_END and verify sender stops

---

## 📋 Documentation

### Central Server
- **Requirements**: [central/docs/req.html](central/docs/req.html) - Complete specification with threading architecture
- **Implementation Review**: [central/docs/review.html](central/docs/review.html) - Code review with Q&A
- **Checklist**: [IMPLEMENTATION_CHECKLIST.md](IMPLEMENTATION_CHECKLIST.md)

### PROCR Service
- **Requirements**: [procr/docs/req.html](procr/docs/req.html) - API contract and processing logic (with clarified "Wait before respond")
- **Implementation Guide**: [procr/docs/implementation_guide.html](procr/docs/implementation_guide.html) - Detailed examples and walkthroughs

---

## 🔧 Configuration

### Central Server (application.properties)
```properties
server.port=5000
txn.sender.pool.size=2
txn.receiver.pool.size=2
txn.rate.interval=10000          # Rate window (ms)
txn.rate.count=4                 # Txns per window
txn.timeout=9000                 # Cache TTL (ms)
```

### PROCR Service (application.properties)
```properties
server.port=8080
procr.response.delay.fixed=0
procr.response.delay.min_random=0
procr.response.delay.step=100
procr.response.delay.max_random=200
```

---

## 📊 Architecture

### Central Server - 6-Thread Model

| Component | Type | Count | Purpose |
|-----------|------|-------|---------|
| Sender Threads | Pool | 2 | Generate rate-limited transactions |
| Receiver Threads | Pool | 2 | Process acknowledgments |
| TCP Writer | Single | 1 | Synchronized sends (no corruption) |
| TCP Reader | Single | 1 | Parse JSON and route messages |

### PROCR - Processing Flow

```
Receive Request (YAML from Adapter)
    ↓
Generate Random (1-100)
    ↓
Decision: APPROVE (1-99) or REJECT (100)
    ↓
Log decision
    ↓
Calculate sleep = fixed_delay + random_value
    ↓
Sleep for duration
    ↓
Log completion
    ↓
Send Response (YAML)
```

### Adapter - Conversion Flow

```
Receive JSON from Central
    ↓
Parse JSON transaction
    ↓
Convert to YAML format
    ↓
POST to PROCR /process endpoint
    ↓
Receive YAML response
    ↓
Convert back to JSON
    ↓
Send JSON acknowledgment to Central
```

---

## 🔄 Message Protocol

### Central ↔ Adapter (JSON over TCP, port 5000)

#### Transaction (Central → Adapter)
```json
{
  "txn_id": "TXN-123456",
  "stan": "123456",
  "type": "T1",
  "narration": "Payment received",
  "quantity": 50,
  "rate": 25.50,
  "taxes": 127.50,
  "total": 1402.50,
  "timestamp": "2024-10-23T12:34:56Z"
}
```

#### Session Messages (Central → Adapter)
```json
{"message_type": "SESSION_START"}
```
```json
{"message_type": "SESSION_END"}
```

#### Acknowledgment (Adapter → Central in JSON)
```json
{
  "response_type": "APPROVE",
  "txn_id": "TXN-123456",
  "status": "SUCCESS",
  "message": "Transaction approved successfully",
  "timestamp": "2024-10-23T12:34:56Z"
}
```

### Adapter ↔ PROCR (YAML over HTTP, port 8080)

#### Transaction (Adapter → PROCR POST /process)
```yaml
txn_id: TXN-123456
stan: "123456"
type: T1
narration: "Payment received"
quantity: 50
rate: 25.50
taxes: 127.50
total: 1402.50
timestamp: 2024-10-23T12:34:56Z
---
```

#### Response (PROCR → Adapter HTTP 200)
```yaml
response_type: APPROVE
txn_id: TXN-123456
status: SUCCESS
message: Transaction approved successfully
timestamp: 2024-10-23T12:34:56Z
---
```

### Session Management
```json
{"message_type": "SESSION_START"}
```
```json
{"message_type": "SESSION_END"}
```

### Transactions (Central → PROCR)
```yaml
txn_id: TXN-123456
stan: "123456"
type: T1
narration: "Payment received"
quantity: 50
rate: 25.50
taxes: 127.50
total: 1402.50
---
```

### Acknowledgments (PROCR → Central)
```yaml
response_type: APPROVE
txn_id: TXN-123456
status: SUCCESS
message: Transaction approved successfully
timestamp: 2024-10-23T12:34:56Z
---
```

---

## ✅ Implementation Status

| Component | Status | Build | Test |
|-----------|--------|-------|------|
| Central | ✅ Complete | ✅ 40 MB JAR | ⏳ Deferred (with Adapter) |
| PROCR | 📝 Spec Complete | ⏳ Ready | ⏳ Deferred |
| Adapter | 📋 Planned | ⏳ Planned | ⏳ Planned |

---

## 🎯 Next Steps

1. **Implement PROCR Service**
   - Create Spring Boot application
   - Implement /health endpoint
   - Implement /process endpoint with approval logic
   - Add configurable response delay (using clarified formula)
   - Full logging at each processing stage

2. **Implement Adapter Service**
   - Connect to Central on port 5000
   - Send SESSION_START message
   - Receive and acknowledge transactions
   - Send SESSION_END when done

3. **Integration Testing**
   - Verify message flow (Central → Adapter)
   - Verify acknowledgment processing
   - Verify state transitions
   - Verify graceful shutdown
   - Load testing with multiple transactions

---

## 📝 Files & Directories

### Documentation
- `README.md` - Project overview (this file)
- `IMPLEMENTATION_CHECKLIST.md` - Central implementation checklist
- `LICENSE` - MIT License
- `.gitignore` - Git ignore rules

### Central Service
- `central/docs/req.html` - Requirements specification
- `central/docs/review.html` - Implementation review
- `central/pom.xml` - Maven configuration
- `central/src/` - 24 Java classes
- `central/target/central-transaction-server-1.0.0.jar` - Executable JAR

### PROCR Service
- `procr/docs/req.html` - API specification
- `procr/docs/implementation_guide.html` - Implementation guide with examples

---

## 🏆 Key Achievements

✅ Central service fully implemented (24 Java classes)  
✅ Thread-safe concurrent architecture (6 threads)  
✅ YAML-based message protocol  
✅ State machine with atomic transitions  
✅ Rate-limited transaction generation  
✅ TTL-based cache management  
✅ PROCR "Wait before respond" section clarified with examples  
✅ Comprehensive documentation  
✅ Ready for Adapter integration  

---

## ❓ Documentation Clarifications

### Central Server - Questions & Answers

**Q1: Connection without SESSION_START → No txn sent?**  
**A**: ✅ YES - Senders check state, sleep 100ms, retry. No transactions generated.

**Q2: SESSION_START → SESSION_END → Listen but no new txn?**  
**A**: ✅ YES - Senders break on SESSION_END_REQUESTED, receivers continue processing ACKs.

### PROCR - Response Delay Clarified

The confusing "Wait before respond" section has been clarified:

**Before** ❌ (Unclear): "Wait before respond, there will be a central time in milli to sleep (fixed) plus..."

**After** ✅ (Clear):
- **Formula**: `sleep_ms = fixed_delay + random_value`
- **Parameters**: Each defined with units (milliseconds) and purpose
- **Examples**: Two complete walkthroughs with calculations
- **Configuration**: All parameters configurable via application.properties
- **Logging**: Requirements documented with sample output

See [procr/docs/implementation_guide.html](procr/docs/implementation_guide.html) for full details.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

When implementing new services or features:

1. Follow the existing code structure and naming conventions
2. Add comprehensive logging at each processing stage
3. Make configuration parameters externally configurable
4. Document all APIs and state transitions
5. Include implementation checklists in documentation

---

**Status**: 🚀 Ready for Adapter Integration & Testing  
**Last Updated**: October 23, 2024

