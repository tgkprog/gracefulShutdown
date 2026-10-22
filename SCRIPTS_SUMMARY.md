# Build and Run Scripts Summary

## Overview

Complete automation for building, starting, stopping, and managing all three microservices.

---

## File Structure

```
gracefulShutdown/
├── build-all.sh          # Build all servers sequentially
├── start-all.sh          # Start all servers in background
├── kill.sh               # Kill servers by pattern
├── central/
│   ├── build.sh          # Build Central only
│   └── run.sh            # Run Central (port 5000)
├── procr/
│   ├── build.sh          # Build PROCR only
│   └── run.sh            # Run PROCR (port 8080)
└── adapter/
    ├── build.sh          # Build Adapter only
    └── run.sh            # Run Adapter (port 9090)
```

---

## Master Scripts

### ./build-all.sh
Build all three servers sequentially.

**Usage:**
```bash
./build-all.sh              # Standard build
./build-all.sh clean        # Clean build (remove old artifacts)
```

**Output:**
- Central: 124 MB JAR
- PROCR: 20 MB JAR
- Adapter: 21 MB JAR
- Total time: ~30-40 seconds

---

### ./start-all.sh
Start all servers in background with logs.

**Usage:**
```bash
./start-all.sh              # Build and start all
./start-all.sh clean        # Clean build and start all
```

**What happens:**
1. Starts Central on port 5000
2. Starts PROCR on port 8080
3. Starts Adapter on port 9090
4. Redirects logs to `/tmp/*.log`
5. Saves PIDs to `/tmp/gracefulshutdown_pids.txt`

**Access:**
- Central Dashboard: `http://localhost:5000/central/dashboard`
- PROCR API: `http://localhost:8080/config/sleep`

**Monitor Logs:**
```bash
tail -f /tmp/central.log
tail -f /tmp/procr.log
tail -f /tmp/adapter.log
```

---

### ./kill.sh
Kill servers by name or pattern with graceful shutdown.

**Usage:**
```bash
./kill.sh all               # Kill all servers
./kill.sh central          # Kill Central only
./kill.sh procr            # Kill PROCR only
./kill.sh adapter          # Kill Adapter only
./kill.sh "java.*8080"     # Kill by regex pattern
./kill.sh "procr"          # Kill matching "procr"
```

**Behavior:**
1. Sends SIGTERM (graceful shutdown)
2. Waits 2 seconds
3. Sends SIGKILL if still running
4. Colored output (green=killed, yellow=not running)

---

## Individual Server Scripts

### Central Server

**Build:**
```bash
cd central && ./build.sh clean
```

**Run:**
```bash
cd central && ./run.sh          # Default port 5000
cd central && ./run.sh 6000     # Custom port 6000
```

**Endpoints:**
- Dashboard: `http://localhost:5000/central/dashboard`
- REST APIs: `/central/api/status`, `/central/api/history`, `/central/api/metrics`
- SQLite: `central-metrics.db`

---

### PROCR Server

**Build:**
```bash
cd procr && ./build.sh clean
```

**Run:**
```bash
cd procr && ./run.sh            # Default port 8080
cd procr && ./run.sh 9000       # Custom port 9000
```

**Endpoints:**
- Config API: `http://localhost:8080/config/sleep` (GET/POST)
- Process API: `http://localhost:8080/process` (POST)
- YAML format support

---

### Adapter Server

**Build:**
```bash
cd adapter && ./build.sh clean
```

**Run:**
```bash
cd adapter && ./run.sh           # Default port 9090
cd adapter && ./run.sh 7000      # Custom port 7000
```

**Features:**
- JSON/YAML conversion
- TCP/HTTP gateway
- Connects Central ↔ PROCR

---

## Common Workflows

### Fresh Start (Everything from Scratch)
```bash
# Kill any existing servers
./kill.sh all

# Clean build all
./build-all.sh clean

# Start all in background
./start-all.sh
```

### Quick Restart After Code Change
```bash
# Kill all
./kill.sh all

# Rebuild Central only (for example)
cd central && ./build.sh

# Start all
./start-all.sh
```

### Debug Mode (Foreground)
```bash
# Terminal 1
cd central && ./run.sh

# Terminal 2
cd procr && ./run.sh

# Terminal 3
cd adapter && ./run.sh

# Watch logs in real-time
# Ctrl+C to stop individual server
```

### Change Sleep Timings at Runtime
```bash
# 1. Start all servers
./start-all.sh

# 2. Open Central Dashboard
#    http://localhost:5000/central/dashboard

# 3. Scroll to "PROCR Sleep Configuration"

# 4. Click "Get From Server" to fetch current settings

# 5. Modify values (Fixed, Min, Max, Step)

# 6. Click "Post to Server" to apply immediately

# 7. No restart needed - takes effect on next transaction!
```

### Monitor Multiple Servers
```bash
# In one terminal, watch all logs
while true; do
  clear
  echo "=== CENTRAL ==="
  tail -3 /tmp/central.log
  echo ""
  echo "=== PROCR ==="
  tail -3 /tmp/procr.log
  echo ""
  echo "=== ADAPTER ==="
  tail -3 /tmp/adapter.log
  sleep 2
done
```

### Check Server Health
```bash
# Check if servers are running
ps aux | grep java | grep -v grep

# Check port availability
netstat -tlnp | grep -E "5000|8080|9090"

# Test endpoints
curl http://localhost:5000/central/api/status | jq
curl http://localhost:8080/config/sleep | jq
```

---

## Port Configuration

### Default Ports
- **Central**: 5000
- **PROCR**: 8080
- **Adapter**: 9090

### Using Custom Ports
```bash
# Run each on different port
cd central && ./run.sh 6000 &
cd procr && ./run.sh 9000 &
cd adapter && ./run.sh 7000 &

# Kill specific port
./kill.sh "java.*6000"

# Access on new port
curl http://localhost:6000/central/api/status
curl http://localhost:9000/config/sleep
```

---

## Troubleshooting

### Build Fails
```bash
# Check Java version
java -version    # Should be 25+

# Check Maven
mvn -version     # Should be 3.8+

# Clean build
./build-all.sh clean

# Check pom.xml
cat central/pom.xml | grep -A5 "<properties>"
```

### Port Already in Use
```bash
# Find what's using the port
lsof -i :5000
lsof -i :8080
lsof -i :9090

# Kill by PID
kill <PID>

# Or use our script
./kill.sh "java.*5000"
```

### Server Won't Start
```bash
# Check logs
tail -f /tmp/central.log
tail -f /tmp/procr.log
tail -f /tmp/adapter.log

# Run in foreground to see errors
cd central && ./run.sh

# Check database lock (Central)
rm central-metrics.db  # Remove and restart
```

### Cannot Connect to Server
```bash
# Verify it's running
ps aux | grep java

# Test connection
nc -zv localhost 5000    # Should succeed
curl http://localhost:5000/central/dashboard

# Check firewall
sudo ufw status          # On Linux
```

---

## Performance Tips

### Minimize JAR Rebuilds
```bash
# If only changing YAML/properties, don't rebuild
# Just restart: ./kill.sh procr && cd procr && ./run.sh

# Only rebuild if Java code changed
./build.sh
```

### Monitor Resource Usage
```bash
# Watch CPU/memory
watch 'ps aux | grep java | grep -v grep | awk "{print \$2, \$3, \$4, \$11}"'

# Or use JVM tools
jps -l      # List Java processes
jstat -gc <PID>  # GC statistics
```

### Parallel Builds
```bash
# Build in parallel (advanced)
cd central && ./build.sh &
cd procr && ./build.sh &
cd adapter && ./build.sh &
wait

# Or just use build-all.sh which is optimized
./build-all.sh
```

---

## Logging

### Redirect Logs
```bash
# Start with custom log location
java -jar central/target/*.jar > /var/log/central.log 2>&1 &

# Or use our scripts (already does this)
./start-all.sh
```

### Set Log Level
```bash
# Debug mode
java -jar central/target/*.jar -Dlogging.level.root=DEBUG

# Or via properties
echo "logging.level.root=DEBUG" >> central/src/main/resources/application.properties
./build.sh
```

### View Real-Time Logs
```bash
# Central
tail -f /tmp/central.log | grep -i "error\|warn\|info"

# All servers
tmux new-session -d -s logs
tmux split-window -h -t logs
tmux split-window -v -t logs
tmux send-keys -t logs.0 'tail -f /tmp/central.log' Enter
tmux send-keys -t logs.1 'tail -f /tmp/procr.log' Enter
tmux send-keys -t logs.2 'tail -f /tmp/adapter.log' Enter
tmux attach -t logs
```

---

## Advanced Usage

### Run on Different Machines
```bash
# Machine 1 (Central)
central$ ./run.sh 5000

# Machine 2 (PROCR)
procr$ ./run.sh 8080

# Machine 3 (Adapter)
adapter$ ./run.sh 9090

# Update Central to point to remote PROCR
# (requires code changes to adapter config)
```

### Load Testing
```bash
# Start with aggressive delays
./start-all.sh

# Set PROCR to slow responses
curl -X POST http://localhost:8080/config/sleep \
  -H "Content-Type: application/json" \
  -d '{"fixed_ms":5000, "random_min_ms":0, "random_step_ms":1000, "random_max_ms":5000}'

# Generate load (requires load test tool)
# ab, wrk, hey, or custom test script
```

### CI/CD Integration
```bash
# In your CI pipeline
./build-all.sh clean || exit 1

# Run tests
# ./test-all.sh (if implemented)

# Package for deployment
tar -czf gracefulshutdown.tar.gz central/target/*.jar procr/target/*.jar adapter/target/*.jar
```

---

## Script Contents

All scripts are:
- ✅ POSIX-compliant bash
- ✅ Color-coded output
- ✅ Error handling
- ✅ Platform-agnostic (Linux/macOS/WSL)
- ✅ Self-documenting (run with no args to see help)

View any script:
```bash
cat ./build-all.sh
cat central/build.sh
cat ./kill.sh
```

---

## Summary Table

| Script | Purpose | Usage |
|--------|---------|-------|
| `build-all.sh` | Build all servers | `./build-all.sh [clean]` |
| `start-all.sh` | Start all in background | `./start-all.sh [clean]` |
| `kill.sh` | Kill servers | `./kill.sh [all\|central\|procr\|adapter\|pattern]` |
| `central/build.sh` | Build Central | `./build.sh [clean]` |
| `central/run.sh` | Run Central | `./run.sh [port]` |
| `procr/build.sh` | Build PROCR | `./build.sh [clean]` |
| `procr/run.sh` | Run PROCR | `./run.sh [port]` |
| `adapter/build.sh` | Build Adapter | `./build.sh [clean]` |
| `adapter/run.sh` | Run Adapter | `./run.sh [port]` |

---

## Next Steps

1. **Build**: `./build-all.sh clean`
2. **Start**: `./start-all.sh`
3. **Dashboard**: Open `http://localhost:5000/central/dashboard`
4. **Configure**: Update PROCR sleep timings in dashboard
5. **Monitor**: Watch logs in `/tmp/*.log`
6. **Stop**: `./kill.sh all`

All ready to go! 🚀
