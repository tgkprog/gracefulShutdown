# Quick Start Guide

## Build All Servers

```bash
cd /data/code/gt/gracefulShutdown
./build-all.sh
```

Expected output:
```
✅ ALL SERVERS BUILT SUCCESSFULLY
JAR: central/target/central-transaction-server-1.0.0.jar (126M)
JAR: procr/target/procr-transaction-processor-1.0.0.jar (21M)
JAR: adapter/target/adapter-json-yaml-converter-1.0.0.jar (21M)
```

## Run Servers (Open 3 Terminals)

### Terminal 1 - Central Server
```bash
cd central && ./run
```
- Port: 5000
- Logs: `./log/central/yyyy/mmm/dd/`
- Database: H2 (./central-metrics)

### Terminal 2 - PROCR Server
```bash
cd procr && ./run
```
- Port: 6000
- Logs: `./log/procr/yyyy/mmm/dd/`

### Terminal 3 - Adapter Server
```bash
cd adapter && ./run
```
- Port: 7000
- Logs: `./log/adapter/yyyy/mmm/dd/`

## Environment Setup

### Java Version
```bash
java -version
# Expected: OpenJDK 26.0.1
```

### Verify Compilation
```bash
javap -verbose central/target/classes/com/gracefulshutdown/central/CentralServerApplication.class | grep "major version"
# Expected: major version: 65 (Java 21)
```

## Configuration

### Runtime JVM Options
Can be customized via environment variables:

```bash
# Custom port
PORT=8000 ./run

# Custom log directory
LOG_DIR=/tmp/logs ./run

# Custom Java options
JAVA_OPTS="-Xmx512m" ./run
```

## Endpoints

### Central Server (http://localhost:5000)
- Status Dashboard: `/central/dashboard`
- API: `/central/api/*`

### PROCR Server (http://localhost:6000)
- API: `/procr/api/*`

### Adapter Server (http://localhost:7000)
- API: `/adapter/api/*`

## Troubleshooting

### "Address already in use"
Port is taken by another process. Use custom port:
```bash
PORT=5500 ./run
```

### "Cannot find database driver"
Rebuild the project:
```bash
./build-all.sh
```

### Check Java Compatibility
```bash
# Verify compilation target
javap -verbose <classfile> | grep "major version"

# Java 21 = version 65 ✓
# Java 26 = version 70 ✗ (not supported by Spring Boot 3.5.0)
```

## File Structure

```
gracefulShutdown/
├── central/              # Transaction Server
│   ├── run              # Run script (no args needed)
│   ├── pom.xml          # Maven config (Java 21 target)
│   ├── log/             # Log files (auto-created)
│   └── central-metrics* # H2 database (auto-created)
├── procr/               # Transaction Processor
│   ├── run              # Run script
│   ├── pom.xml
│   └── log/
├── adapter/             # JSON/YAML Converter
│   ├── run              # Run script
│   ├── pom.xml
│   └── log/
├── build-all.sh         # Build script
└── JAVA_VERSION_SETUP_COMPLETE.md  # Full documentation
```

## Database

### Central Server (H2)
- Location: `central/central-metrics.db`
- Type: Embedded relational database
- Configuration: `application.properties`

Metrics tables:
- Metrics (transaction history)
- Indexes on timestamp, status, type

## Logging

### Format
```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL classname - message
```

### Rollover Policy
- 5MB per file
- Date-based directory structure: `log/<component>/yyyy/MMM/dd/`
- 30-day retention
- 1GB max capacity

### Enable Debug Logging
Edit `application.properties`:
```properties
logging.level.com.gracefulshutdown.central=DEBUG
logging.level.com.gracefulshutdown.procr=DEBUG
logging.level.com.gracefulshutdown.adapter=DEBUG
```

## Performance

### Defaults (Can Be Tuned)
- Central sender threads: 2
- Central receiver threads: 2
- Transaction timeout: 9 seconds
- Queue capacity: 100

Configure via:
- `central/src/main/resources/application.properties`
- `procr/src/main/resources/application.properties`
- `adapter/src/main/resources/adapter-config.yaml`

## Shutdown

### Graceful Shutdown
Send SIGTERM signal:
```bash
kill -TERM <pid>
```

Server will:
1. Stop accepting new connections
2. Flush pending transactions
3. Close database connections
4. Exit cleanly

### Force Shutdown
```bash
kill -9 <pid>
```

## Development

### Rebuild Single Server
```bash
cd central && mvn clean package
```

### Run with Debug Logging
```bash
cd central && \
  JAVA_OPTS="-Xmx1g -Xms512m" \
  ./run
```

### Check Dependencies
```bash
mvn dependency:tree | grep -i h2
```

## System Requirements

- Java: 26.0.1 (minimum 26.0.0)
- Memory: Minimum 512MB, Recommended 1GB per server
- Disk: 100MB for binaries, 500MB for logs
- Network: TCP ports 5000, 6000, 7000 available

## Next Steps

1. ✅ Build: `./build-all.sh`
2. ✅ Verify: `java -version`
3. ✅ Run: Open 3 terminals and start servers
4. ✅ Monitor: Check logs in `log/` directories

---

**Java Version**: 26.0.1  
**Spring Boot**: 3.5.0  
**Compilation Target**: Java 21 v65  
**Status**: Production Ready ✅
