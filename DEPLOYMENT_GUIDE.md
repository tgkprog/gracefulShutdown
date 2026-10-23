# Dual Port Deployment Guide

## Quick Start (Development)

```bash
# 1. Build
cd central
mvn clean package -DskipTests

# 2. Run
java -jar target/central-transaction-server-1.0.0.jar

# 3. Test
curl http://localhost:8080/central/api/health | jq
nc -zv localhost 5009
```

## Configuration

### Web API Port (Default: 8080)
```properties
server.port=8080
```

### TCP Server Port (Default: 5009)
```properties
tcp.server.port=5009
```

### Custom Ports
```bash
java -jar central-transaction-server-1.0.0.jar \
  --server.port=9000 \
  --tcp.server.port=6000
```

## Architecture

```
┌────────────────────────────────────────┐
│  Central Transaction Server (Java)     │
├────────────────────────────────────────┤
│                                        │
│  Port 8080 (HTTP/REST)                 │
│  ├─ /central/api/health                │
│  ├─ /central/api/info                  │
│  ├─ /central/api/status                │
│  ├─ /central/api/metrics               │
│  └─ /central/dashboard                 │
│                                        │
│  Port 5009 (TCP/JSON)                  │
│  └─ Adapter Connections                │
│                                        │
└────────────────────────────────────────┘
```

## API Endpoints

### Health Check
```bash
curl http://localhost:8080/central/api/health
```

### Server Information
```bash
curl http://localhost:8080/central/api/info | jq
```

### Current Status
```bash
curl http://localhost:8080/central/api/status | jq
```

### Metrics & History
```bash
curl http://localhost:8080/central/api/metrics | jq
```

### Dashboard
```
http://localhost:8080/central/dashboard
```

## Integration with Adapter

The Adapter service should:

1. **Connect to TCP Server**
   ```
   Host: localhost
   Port: 5009
   Protocol: JSON over TCP
   ```

2. **Monitor Web API**
   ```
   URL: http://localhost:8080/central/api
   Methods: GET
   Format: JSON
   ```

3. **Send Messages**
   ```json
   {"message_type": "SESSION_START"}
   {"txn_id": "TXN-123456", "amount": 100, ...}
   {"message_type": "SESSION_END"}
   ```

4. **Receive Responses**
   ```json
   {"response_type": "APPROVE", "txn_id": "TXN-123456", ...}
   ```

## Production Deployment

### Docker
```dockerfile
FROM openjdk:26-slim
COPY central/target/central-transaction-server-1.0.0.jar app.jar
EXPOSE 8080 5009
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes
```yaml
apiVersion: v1
kind: Service
metadata:
  name: central-server
spec:
  ports:
  - name: web-api
    port: 8080
    targetPort: 8080
  - name: tcp-server
    port: 5009
    targetPort: 5009
  selector:
    app: central
```

### Environment Variables
```bash
SERVER_PORT=8080
TCP_SERVER_PORT=5009
TXN_RATE_INTERVAL=10000
TXN_RATE_COUNT=4
```

## Monitoring

### Health Status
```bash
while true; do
  curl -s http://localhost:8080/central/api/health | jq .status
  sleep 5
done
```

### Port Verification
```bash
netstat -tlnp | grep java
```

### Connection Monitoring
```bash
watch 'netstat -tn | grep -E ":(8080|5009)"'
```

### Transaction Metrics
```bash
curl -s http://localhost:8080/central/api/metrics | jq '.status'
```

## Troubleshooting

### Port Already in Use
```bash
# Find process
lsof -i :8080
lsof -i :5009

# Kill process
kill -9 <PID>

# Or use different ports
java -jar app.jar --server.port=9000 --tcp.server.port=6000
```

### Connection Refused
```bash
# Verify server is running
curl -v http://localhost:8080/central/api/health

# Check logs
tail -f logs/*.log

# Test TCP connectivity
nc -zv localhost 5009
```

### Slow Response
```bash
# Check system resources
free -h
top

# Check database
# Look for H2 file: central-metrics.mv.db
ls -lh central-metrics.*

# Clear cache if needed
# Stop server and remove cache database
```

## Performance Tuning

### Thread Pool Size
```properties
txn.sender.pool.size=2
txn.receiver.pool.size=2
```

### Transaction Rate
```properties
txn.rate.interval=10000    # ms per window
txn.rate.count=4           # transactions per window
```

### Cache Timeout
```properties
txn.timeout.seconds=9
```

### Tomcat Settings
```properties
server.tomcat.threads.max=200
server.tomcat.accept-count=100
```

## Security Considerations

1. **API Port (8080)**
   - Expose through reverse proxy
   - Use firewall rules
   - Consider authentication/authorization

2. **TCP Server (5009)**
   - Restrict to internal network only
   - Use network segmentation
   - Implement firewall rules

3. **Database**
   - H2 database stored locally
   - Use encryption for sensitive data
   - Regular backups recommended

## Verification Checklist

- [ ] Build completes successfully
- [ ] Server starts without errors
- [ ] Port 8080 is listening
- [ ] Port 5009 is listening
- [ ] /central/api/health responds
- [ ] /central/api/info responds
- [ ] Adapter can connect to 5009
- [ ] Metrics API shows data
- [ ] Dashboard displays content

## Support & Debugging

### Enable Debug Logging
```bash
java -jar app.jar \
  --logging.level.root=DEBUG \
  --logging.level.com.gracefulshutdown.central=TRACE
```

### Check Component Status
```bash
# Verify components are initialized
grep -i "started\|listening\|initialized" logs/app.log
```

### Test Full Flow
```bash
# 1. Health check
curl http://localhost:8080/central/api/health

# 2. Server info
curl http://localhost:8080/central/api/info

# 3. Current metrics
curl http://localhost:8080/central/api/metrics

# 4. TCP connectivity
nc -zv localhost 5009
```

---

**Ready for:** ✓ Development  
**Ready for:** ✓ Staging  
**Ready for:** ✓ Production  

See IMPLEMENTATION_SUMMARY.md for complete details.
