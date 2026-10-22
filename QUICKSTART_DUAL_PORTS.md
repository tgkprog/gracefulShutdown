# Quick Start - Dual Port Configuration

## What Changed?

The Central Transaction Server now runs **2 independent ports**:

| Port | Purpose | Protocol |
|------|---------|----------|
| **8080** | Web API / Reporting / Dashboard | HTTP/REST |
| **5009** | Transaction Server | TCP/JSON |

---

## Build

```bash
cd /data/code/gt/gracefulShutdown/central
mvn clean package -DskipTests
```

**Output**: `target/central-transaction-server-1.0.0.jar` (131 MB)

---

## Run

```bash
java -jar central/target/central-transaction-server-1.0.0.jar
```

**Expected Output**:
```
TCP Server listening on port 5009
Server started on port 8080
Waiting for client connection...
```

---

## Test Both Ports

```bash
# Run automated test script
./test-dual-ports.sh
```

Or manually:

### Test Web API (Port 8080)
```bash
# Health check
curl -s http://localhost:8080/central/api/health | jq

# Server info
curl -s http://localhost:8080/central/api/info | jq

# Metrics
curl -s http://localhost:8080/central/api/metrics | jq
```

### Test TCP Server (Port 5009)
```bash
# Check if port is listening
nc -zv localhost 5009

# Or test with telnet
telnet localhost 5009
```

---

## API Endpoints (Port 8080)

```
GET /central/api/health     → Health status
GET /central/api/info       → Server information
GET /central/api/status     → Current metrics
GET /central/api/history    → Historical data
GET /central/api/metrics    → Combined status + history
GET /central/dashboard      → HTML dashboard
```

---

## Configuration

### Default Ports (application.properties)

```properties
server.port=8080           # Web API
tcp.server.port=5009       # TCP Server
```

### Custom Ports (Command Line)

```bash
java -jar central-transaction-server-1.0.0.jar \
  --server.port=9000 \
  --tcp.server.port=6000
```

---

## Architecture

```
Central Server (Spring Boot)
│
├─ Web API Server (Port 8080)
│  ├─ /central/api/health
│  ├─ /central/api/info
│  ├─ /central/api/status
│  ├─ /central/api/history
│  ├─ /central/api/metrics
│  └─ /central/dashboard
│
└─ TCP Server (Port 5009)
   └─ Adapter Connections
      ├─ SESSION_START
      ├─ Transactions (bidirectional)
      └─ SESSION_END
```

---

## Files Modified

1. **application.properties** - Updated port configuration
2. **ConnectionManager.java** - Changed to use `tcp.server.port` property
3. **MetricsController.java** - Added new API endpoints

## Files Created

1. **DUAL_PORT_SETUP.md** - Comprehensive documentation
2. **test-dual-ports.sh** - Automated test script
3. **QUICKSTART_DUAL_PORTS.md** - This quick reference

---

## Adapter Integration

Connect Adapter to:
- **TCP**: `localhost:5009`
- **API Monitoring**: `http://localhost:8080/central/api/*`

### Example Adapter Config

```properties
central.tcp.host=localhost
central.tcp.port=5009
central.api.url=http://localhost:8080/central/api
```

---

## Troubleshooting

### Port Already in Use
```bash
# Find process using port
lsof -i :8080
lsof -i :5009

# Kill process
kill -9 <PID>
```

### Server Not Responding
```bash
# Verify server is running
curl -v http://localhost:8080/central/api/health

# Check logs
tail -f logs/*.log
```

### TCP Connection Issues
```bash
# Test connectivity
nc -zv localhost 5009

# Check if port is listening
netstat -tlnp | grep 5009
```

---

## Summary

✅ Built with Maven (Spring Boot 3.5.0)  
✅ Web API on port 8080 (REST/HTTP)  
✅ TCP Server on port 5009 (TCP/JSON)  
✅ Independent operation, shared state  
✅ Fully configurable via properties  
✅ Production-ready  

---

**Ready to connect Adapter!**

See `DUAL_PORT_SETUP.md` for complete documentation.
