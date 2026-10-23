# H2 Standalone Database - Quick Setup Guide

## 3-Step Setup

### Step 1: Download & Start H2 Database Server
```bash
cd /data/code/gt/gracefulShutdown/db
./start-db.sh
```

This will:
- Auto-download H2 JAR (2.2.220)
- Start standalone server on port 9092
- Store database in `db/` folder
- Show success message when ready

**Keep this terminal open!**

### Step 2: In New Terminal - Rebuild Central
```bash
cd /data/code/gt/gracefulShutdown/central
mvn clean package -DskipTests
```

### Step 3: Start Central Server
```bash
java -jar central/target/central-transaction-server-1.0.0.jar
```

You should see:
```
TCP Server listening on port 5009
Tomcat started on port 8080
```

---

## Test It

### Terminal 4: Verify Everything Works
```bash
# Test Web API
curl http://localhost:8080/central/api/health | jq

# Expected output:
# {
#   "status": "UP",
#   "tcp_port": 5009,
#   "web_port": 8080
# }

# Test metrics
curl http://localhost:8080/central/api/metrics | jq
```

---

## Architecture

```
Terminal 1: H2 Server (Port 9092)
    ↓
    └─ Listens on TCP:9092
    └─ Manages db/central-metrics.mv.db

Terminal 2: Central Server (Port 8080/5009)
    ↓
    ├─ TCP Server on 5009 (Adapter connects here)
    ├─ Web API on 8080 (Reporting & monitoring)
    └─ Connects to H2 via tcp://localhost:9092
```

---

## Database Files Location

All database files are in: `/data/code/gt/gracefulShutdown/db/`

```
db/
├── start-db.sh                  # Start script (executable)
├── h2-2.2.220.jar              # H2 database (auto-downloaded)
├── central-metrics.mv.db       # Main database file
└── central-metrics.trace.db    # Transaction log
```

---

## Configuration

### application.properties
```properties
# H2 Standalone Server
spring.datasource.url=jdbc:h2:tcp://localhost:9092/./db/central-metrics;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

### Connection String Explained
```
jdbc:h2:tcp://localhost:9092/./db/central-metrics

  ↓           ↓              ↓    ↓
  ├─ jdbc     → Java database connectivity
  ├─ h2       → H2 database protocol
  ├─ tcp      → Network connection (server mode)
  ├─ localhost:9092 → H2 server address & port
  └─ ./db/central-metrics → Database file path
```

---

## Stopping Services

### Stop Central Server
```
Press Ctrl+C in the Central terminal
```

### Stop H2 Database
```
Press Ctrl+C in the H2 terminal
```

Data is persisted, so just restart to resume!

---

## Key Advantages Over Embedded

| Feature | Embedded H2 | H2 Server Mode |
|---------|-------------|----------------|
| Setup | Just run jar | Run db server + jar |
| Data persistence | ✓ | ✓ |
| Folder isolation | ✗ | ✓ (db/ folder) |
| Multiple instances | ✗ | ✓ |
| Easy backup | ✗ | ✓ (one folder) |
| Network access | ✗ | ✓ |
| Shared across services | ✗ | ✓ |

---

## Backup & Restore

### Backup (While Running)
```bash
cd /data/code/gt/gracefulShutdown
tar -czf db-backup-$(date +%Y%m%d).tar.gz db/central-metrics.mv.db
```

### Restore
```bash
# Stop H2 first: Ctrl+C in H2 terminal
cd /data/code/gt/gracefulShutdown
tar -xzf db-backup-20261023.tar.gz
# Restart H2: ./db/start-db.sh
```

### Fresh Start (Clear Database)
```bash
# Stop H2 first: Ctrl+C
cd /data/code/gt/gracefulShutdown/db
rm -f central-metrics.mv.db central-metrics.trace.db
# Restart H2: ./start-db.sh
```

---

## Troubleshooting

### Port Already in Use
```bash
lsof -i :9092
kill -9 <PID>
# Then: cd db && ./start-db.sh
```

### H2 Not Starting
```bash
# Check Java is installed
java -version

# Check internet (for auto-download)
curl -I https://repo1.maven.org/maven2/

# Manual download if needed
cd db
curl -o h2-2.2.220.jar \
  https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar
./start-db.sh
```

### Central Can't Connect to H2
```bash
# Make sure H2 is running first
# Terminal 1: cd db && ./start-db.sh

# Wait 5 seconds for H2 to start
# Then start Central in Terminal 2
```

### Database Locked
```bash
# Multiple H2 instances running
ps aux | grep h2
kill -9 <PID>

# Restart H2
cd db && ./start-db.sh
```

---

## Next Steps

1. ✅ Create `db/` folder (done)
2. ✅ Create `start-db.sh` script (done)
3. ✅ Update configuration (done)
4. ✅ Remove old database files (done)
5. 🔄 **Run: `cd db && ./start-db.sh`**
6. 🔄 **Run: `java -jar central/target/central-transaction-server-1.0.0.jar`**
7. 🔄 **Test: `curl http://localhost:8080/central/api/health | jq`**

---

**For detailed info**: See `DB_STANDALONE_SETUP.md`

