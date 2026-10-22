# Dual Port Configuration - Central Server

## Overview

The Central Transaction Server now supports **two independent ports** for different communication protocols:

| Port | Protocol | Purpose | Usage |
|------|----------|---------|-------|
| **8080** | HTTP/REST | Web API, Reporting & Information Service | Dashboard, metrics, health checks, API clients |
| **5009** | TCP/JSON | Transaction Protocol | Direct TCP connections from Adapter |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│         Central Transaction Server (Spring Boot)     │
│                                                       │
│  ┌────────────────────────────────────────────────┐ │
│  │         WEB API SERVER (Port 8080)             │ │
│  │                                                │ │
│  │  • REST Endpoints: /central/api/*              │ │
│  │  • Metrics & Status API                        │ │
│  │  • Health Check Endpoint                       │ │
│  │  • HTML Dashboard                              │ │
│  │  • Information Service                         │ │
│  └────────────────────────────────────────────────┘ │
│                                                       │
│  ┌────────────────────────────────────────────────┐ │
│  │         TCP SERVER (Port 5009)                 │ │
│  │                                                │ │
│  │  • JSON/TCP Protocol                           │ │
│  │  • Adapter Connection                          │ │
│  │  • Transaction Exchange                        │ │
│  │  • Acknowledgment Processing                   │ │
│  └────────────────────────────────────────────────┘ │
│                                                       │
│  ┌────────────────────────────────────────────────┐ │
│  │      Shared Components (Thread Pools)          │ │
│  │                                                │ │
│  │  • Sender Pool (2 threads)                     │ │
│  │  • Receiver Pool (2 threads)                   │ │
│  │  • Transaction Cache                          │ │
│  │  • Metrics Repository                         │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## Port Configuration

### application.properties

```properties
# Web API Server (Port 8080)
server.port=8080
server.servlet.context-path=/

# TCP Server (Port 5009)
tcp.server.port=5009
```

### Environment Override

You can override ports using environment variables:

```bash
# Override web port
java -jar central-transaction-server-1.0.0.jar --server.port=9000

# Override TCP port
java -jar central-transaction-server-1.0.0.jar --tcp.server.port=6000

# Both ports
java -jar central-transaction-server-1.0.0.jar \
  --server.port=9000 \
  --tcp.server.port=6000
```

---

## API Endpoints (Web API - Port 8080)

### 1. Health Check
```
GET /central/api/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": 1698066000000,
  "service": "Central Transaction Server",
  "web_port": 8080,
  "tcp_port": 5009
}
```

### 2. Server Information
```
GET /central/api/info
```

**Response:**
```json
{
  "service": "Central Transaction Server",
  "version": "1.0.0",
  "ports": {
    "web_api_port": 8080,
    "tcp_server_port": 5009
  },
  "description": "Transaction generator and sender with web API for reporting",
  "endpoints": {
    "health": "/central/api/health",
    "status": "/central/api/status",
    "metrics": "/central/api/metrics",
    "history": "/central/api/history",
    "info": "/central/api/info",
    "dashboard": "/central/dashboard"
  }
}
```

### 3. Current Status
```
GET /central/api/status
```

**Response:**
```json
{
  "adapter_connected": true,
  "session_active": true,
  "transactions_sent": 42,
  "transactions_received": 42,
  "transactions_approved": 41,
  "transactions_rejected": 1,
  "avg_response_time_ms": 125,
  "peak_response_time_ms": 250
}
```

### 4. Metrics History
```
GET /central/api/history
```

**Response:**
```json
[
  {
    "timestamp": "2024-10-23T12:30:00Z",
    "minutes_connected": 10,
    "minutes_session_active": 10,
    "transactions_sent": 4,
    "transactions_approved": 4,
    "transactions_rejected": 0
  }
]
```

### 5. Combined Metrics
```
GET /central/api/metrics
```

**Response:**
```json
{
  "status": { ... },
  "history": [ ... ],
  "timestamp": 1698066000000
}
```

### 6. Dashboard
```
GET /central/dashboard
```

HTML dashboard with real-time metrics and charts.

---

## TCP Server Details (Port 5009)

The TCP server handles direct connections from the Adapter service:

### Connection Protocol

1. **Client Connection** → Server accepts on port 5009
2. **SESSION_START Message** → Server initializes session
3. **Transaction Exchange** → Bidirectional JSON messages
4. **SESSION_END Message** → Clean disconnect

### Message Format

**Transaction (Central → Adapter):**
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

**Acknowledgment (Adapter → Central):**
```json
{
  "response_type": "APPROVE",
  "txn_id": "TXN-123456",
  "status": "SUCCESS",
  "message": "Transaction approved successfully",
  "timestamp": "2024-10-23T12:34:56Z"
}
```

---

## Running the Server

### Build
```bash
cd central
mvn clean package -DskipTests
```

### Run with Default Ports
```bash
java -jar target/central-transaction-server-1.0.0.jar
```

Expected output:
```
TCP Server listening on port 5009
Server started on port 8080
Waiting for client connection...
```

### Run with Custom Ports
```bash
java -jar target/central-transaction-server-1.0.0.jar \
  --server.port=8080 \
  --tcp.server.port=5009
```

---

## Testing the Dual-Port Setup

### Test Web API (Port 8080)

```bash
# Health check
curl -s http://localhost:8080/central/api/health | jq

# Server info
curl -s http://localhost:8080/central/api/info | jq

# Status
curl -s http://localhost:8080/central/api/status | jq

# Metrics
curl -s http://localhost:8080/central/api/metrics | jq
```

### Test TCP Server (Port 5009)

```bash
# Simple TCP connection test
nc -zv localhost 5009

# Or using telnet
telnet localhost 5009
```

### Port Verification

```bash
# Check which ports are listening
netstat -tlnp | grep java

# Or using lsof
lsof -i -P -n | grep LISTEN
```

Expected output:
```
java      12345  user   50u  IPv4   123456  0t0  TCP  127.0.0.1:8080 (LISTEN)
java      12345  user   51u  IPv4   123457  0t0  TCP  127.0.0.1:5009 (LISTEN)
```

---

## Configuration Details

### Web API Server (Spring Boot Embedded Tomcat)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | HTTP server port for REST API |
| `server.servlet.context-path` | `/` | Root context path |
| `server.tomcat.threads.max` | 200 | Max thread pool size |

### TCP Server (Custom Implementation)

| Property | Default | Description |
|----------|---------|-------------|
| `tcp.server.port` | 5009 | TCP server port for transactions |
| `txn.sender.pool.size` | 2 | Number of sender threads |
| `txn.receiver.pool.size` | 2 | Number of receiver threads |
| `txn.rate.interval` | 10000 | Rate window (ms) |
| `txn.rate.count` | 4 | Transactions per window |
| `txn.timeout.seconds` | 9 | Cache TTL (seconds) |

---

## Service Startup Flow

```
1. Spring Boot Application Start
   └─ Initialize Spring Context

2. Metrics Service
   └─ Load historical metrics from database

3. Session Manager
   └─ Initialize session state

4. Thread Pools
   ├─ Sender Thread Pool (2 threads)
   ├─ Receiver Thread Pool (2 threads)
   └─ Ready for transactions

5. Connection Manager (TCP Server)
   └─ Start TcpServerHandler listening on port 5009
   └─ Wait for Adapter connection

6. Spring Web Server
   └─ Start embedded Tomcat on port 8080
   └─ REST API endpoints ready

7. Initialization Complete
   └─ System ready for Adapter connection
```

---

## Monitoring Both Ports

### Real-time Dashboard
```
http://localhost:8080/central/dashboard
```

### API Monitoring Script
```bash
#!/bin/bash
while true; do
  echo "=== Status at $(date) ==="
  curl -s http://localhost:8080/central/api/status | jq .
  echo ""
  sleep 5
done
```

### TCP Connection Monitoring
```bash
# Watch for TCP connections on port 5009
watch 'netstat -tn | grep 5009'
```

---

## Troubleshooting

### Port Already in Use

If you get "Address already in use" error:

```bash
# Find process using the port
lsof -i :8080
lsof -i :5009

# Kill the process
kill -9 <PID>

# Or use a different port
java -jar central-transaction-server-1.0.0.jar \
  --server.port=9000 \
  --tcp.server.port=6000
```

### No TCP Connection

1. Verify TCP server is listening
   ```bash
   netstat -tlnp | grep 5009
   ```

2. Test connectivity
   ```bash
   nc -zv localhost 5009
   ```

3. Check logs for errors
   ```bash
   tail -f app.log.dir../app.log
   ```

### API Not Responding

1. Verify web server is running
   ```bash
   curl -v http://localhost:8080/central/api/health
   ```

2. Check port configuration
   ```bash
   netstat -tlnp | grep 8080
   ```

3. Review application logs for startup errors

---

## Integration with Adapter

The Adapter service connects to the TCP port (5009) and monitors the Web API (8080):

```
Adapter Service
    │
    ├─→ TCP Connection to Central:5009 (Transactions)
    │
    └─→ HTTP GET to Central:8080/api/* (Monitoring)
```

### Adapter Configuration Example

```properties
# Connect to Central TCP server
central.tcp.host=localhost
central.tcp.port=5009

# Monitor Central API
central.api.url=http://localhost:8080/central/api
```

---

## Production Considerations

### Network Security

- Expose Web API (8080) through proxy/load balancer
- Restrict TCP server (5009) to internal network only
- Use firewall rules to limit access

### Port Selection

- **Development**: 8080 (web), 5009 (tcp)
- **Staging**: 8080 (web), 5009 (tcp)
- **Production**: Use environment variables to configure

### Scaling

Both ports run independently within the same JVM process:
- No inter-process communication overhead
- Shared database and cache
- Single deployment unit

---

## Summary

✅ **Web API Port 8080**: Reporting, health checks, dashboards  
✅ **TCP Port 5009**: Transaction protocol, Adapter communication  
✅ **Independent Operation**: Each port serves distinct purpose  
✅ **Shared State**: Both ports access same metrics and cache  
✅ **Production Ready**: Configurable, secure, monitoring-friendly  

---

**Version**: 1.0.0  
**Last Updated**: October 23, 2026
