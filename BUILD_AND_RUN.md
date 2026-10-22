# Build and Run Scripts

This directory contains shell scripts to build and run all three microservices.

## Quick Start

### Build All Servers
```bash
./build-all.sh              # Build all servers
./build-all.sh clean        # Clean build (remove old artifacts)
```

### Start All Servers (background)
```bash
./start-all.sh              # Start all servers in background
./start-all.sh clean        # Clean build and start
```

### Stop All Servers
```bash
./kill.sh all               # Kill all servers gracefully
```

---

## Individual Server Scripts

### Central Server (Port 5000)
```bash
cd central

# Build
./build.sh                  # Build
./build.sh clean           # Clean build

# Run
./run.sh                   # Start on default port 5000
./run.sh 5001             # Start on custom port

# Dashboard
# http://localhost:5000/central/dashboard
```

### PROCR Server (Port 8080)
```bash
cd procr

# Build
./build.sh                  # Build
./build.sh clean           # Clean build

# Run
./run.sh                   # Start on default port 8080
./run.sh 8081             # Start on custom port

# APIs
# http://localhost:8080/config/sleep (GET/POST)
# http://localhost:8080/process (POST)
```

### Adapter Server (Port 9090)
```bash
cd adapter

# Build
./build.sh                  # Build
./build.sh clean           # Clean build

# Run
./run.sh                   # Start on default port 9090
./run.sh 9091             # Start on custom port
```

---

## Process Management

### Kill Specific Server
```bash
./kill.sh central          # Kill Central only
./kill.sh procr            # Kill PROCR only
./kill.sh adapter          # Kill Adapter only
```

### Kill by Pattern
```bash
./kill.sh "java.*8080"     # Kill any Java process on port 8080
./kill.sh "procr"          # Kill any process matching "procr"
```

### View Running Processes
```bash
ps aux | grep java
```

---

## Monitoring

### View Logs (when started with start-all.sh)
```bash
tail -f /tmp/central.log    # Central logs
tail -f /tmp/procr.log      # PROCR logs
tail -f /tmp/adapter.log    # Adapter logs
```

### Check Port Availability
```bash
netstat -tlnp | grep -E "5000|8080|9090"
```

### View Running Server PIDs
```bash
cat /tmp/gracefulshutdown_pids.txt
```

---

## Examples

### Scenario 1: Full Clean Start
```bash
# Kill any running instances
./kill.sh all

# Clean build
./build-all.sh clean

# Start all servers
./start-all.sh

# Open dashboard
# http://localhost:5000/central/dashboard
```

### Scenario 2: Rebuild Central Only
```bash
./kill.sh central
cd central
./build.sh clean
./run.sh
```

### Scenario 3: Start Servers on Custom Ports
```bash
cd central && ./run.sh 6000 &
cd procr && ./run.sh 9000 &
cd adapter && ./run.sh 7000 &
```

### Scenario 4: Kill PROCR and Restart
```bash
./kill.sh procr
cd procr
./run.sh
```

---

## Troubleshooting

### Port Already in Use
```bash
# Find what's using the port
lsof -i :5000           # Central
lsof -i :8080           # PROCR
lsof -i :9090           # Adapter

# Kill the process
./kill.sh central       # or use kill.sh for pattern-based kill
```

### Build Fails
```bash
# Clean build
./build-all.sh clean

# Check Java version
java -version

# Check Maven
mvn -version
```

### Cannot Connect to Server
```bash
# Check if server is running
ps aux | grep java

# Check logs
tail -f /tmp/central.log
tail -f /tmp/procr.log
tail -f /tmp/adapter.log

# Try starting with foreground to see errors
cd central && ./run.sh
```

---

## Script Details

### build.sh (each server)
- Checks for Maven
- Compiles Java source
- Creates JAR file
- Shows build status

### run.sh (each server)
- Validates JAR exists
- Accepts optional port argument
- Shows connection info
- Runs Java process in foreground

### kill.sh
- Supports killing individual servers or all
- Supports pattern matching for flexibility
- Graceful SIGTERM, then forced SIGKILL if needed
- Color-coded output

### build-all.sh
- Builds each server sequentially
- Supports clean flag
- Shows progress for each build

### start-all.sh
- Starts all servers in background
- Saves PIDs for later reference
- Redirects logs to /tmp/*.log
- Supports clean build before start

---

## Default Ports
- **Central**: 5000
- **PROCR**: 8080
- **Adapter**: 9090

These can be overridden by passing a port number to ./run.sh

---

## Environment Requirements
- Java 25+
- Maven 3.8+
- Linux/macOS/WSL (bash required)
- Ports 5000, 8080, 9090 available (or use custom ports)

---

## Useful Commands

```bash
# View all Java processes
jps -l

# Kill by port
kill $(lsof -t -i:5000)

# Check if port is listening
nc -zv localhost 5000

# Get verbose startup
cd central && java -jar target/central-transaction-server-1.0.0.jar -Dlogging.level.root=DEBUG

# Monitor resource usage
watch 'ps aux | grep java'
```
