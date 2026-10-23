# Standalone H2 Database Setup Guide

## Overview

The Central Transaction Server now uses **H2 Database in Standalone Server Mode**:

- **Mode**: TCP Server (independent process)
- **Port**: 9092 (default)
- **Location**: `/data/code/gt/gracefulShutdown/db/`
- **Database Name**: central-metrics
- **Data Files**: Stored in `db/` folder

## Architecture

```
┌──────────────────────────────────────┐
│   Central Server (Port 8080/5009)    │
│                                      │
│  Connects to H2 via TCP:9092         │
└──────────────────────────────────────┘
              │
              │ jdbc:h2:tcp://localhost:9092/./db/central-metrics
              │
┌──────────────────────────────────────┐
│   H2 Database Server (Port 9092)     │
│                                      │
│  • Standalone TCP Server             │
│  • Database files in db/ folder      │
│  • Persistent storage                │
└──────────────────────────────────────┘
```

## Quick Start

### 1. Start Database Server
```bash
cd /data/code/gt/gracefulShutdown/db
./start-db.sh
```

**Expected Output:**
```
H2 DATABASE SERVER - STANDALONE MODE

📁 Database Directory: /data/code/gt/gracefulShutdown/db
📊 Database Name: central-metrics
🔌 TCP Port: 9092
👤 Username: sa

🚀 Starting H2 Database Server...
───────────────────────────────────────
Server is running on:
  TCP: localhost:9092
  Database: central-metrics
  Files: /data/code/gt/gracefulShutdown/db/

To stop the server: Press Ctrl+C
```

Keep this terminal open and open a new terminal for the next step.

### 2. Start Central Server
```bash
cd /data/code/gt/gracefulShutdown/central
java -jar target/central-transaction-server-1.0.0.jar
```

The Central Server will:
- Connect to H2 database on port 9092
- Create tables if they don't exist
- Store all metrics in the db folder

### 3. Test the Connection
```bash
# In a third terminal
curl http://localhost:8080/central/api/health | jq
```

## Configuration

### application.properties
```properties
# H2 Standalone Server Connection
spring.datasource.url=jdbc:h2:tcp://localhost:9092/./db/central-metrics;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
```

### Connection String
```
jdbc:h2:tcp://localhost:9092/./db/central-metrics
```

### Parameters Explained
- `tcp://localhost:9092` - Connect to H2 server on port 9092
- `./db/central-metrics` - Database file location relative to H2 base directory
- `DB_CLOSE_DELAY=-1` - Don't close DB connections immediately
- `DB_CLOSE_ON_EXIT=FALSE` - Keep DB open when application exits

## Database Files

All database files are stored in: `/data/code/gt/gracefulShutdown/db/`

### Files Created
```
db/
├── start-db.sh                      # Start script
├── h2-2.2.220.jar                   # H2 JAR (auto-downloaded)
├── central-metrics.mv.db            # Main database file
├── central-metrics.trace.db         # Trace log file (optional)
└── .h2.server.properties            # H2 server config
```

## Running Multiple Instances

If you need to run multiple Central servers with separate databases:

### Instance 1 - Main Database
```bash
# Terminal 1: Start DB
cd /data/code/gt/gracefulShutdown/db
./start-db.sh

# Terminal 2: Start Central
java -jar central/target/central-transaction-server-1.0.0.jar --server.port=8080 --tcp.server.port=5009
```

### Instance 2 - Separate Database
```bash
# Create separate database directory
mkdir db-instance2
cd db-instance2
cp ../db/start-db.sh .

# Edit start-db.sh to use different port (9093)
# Change: DB_PORT=9092 → DB_PORT=9093
# Then start: ./start-db.sh

# In another terminal
java -jar central/target/central-transaction-server-1.0.0.jar \
  --server.port=8081 \
  --tcp.server.port=5010 \
  --spring.datasource.url=jdbc:h2:tcp://localhost:9093/./db-instance2/central-metrics
```

## Managing the Database

### Backup Database
```bash
cd /data/code/gt/gracefulShutdown/db
tar -czf central-metrics-backup-$(date +%Y%m%d_%H%M%S).tar.gz central-metrics.mv.db
```

### Restore Database
```bash
cd /data/code/gt/gracefulShutdown/db
# Stop H2 server first (Ctrl+C)
tar -xzf central-metrics-backup-TIMESTAMP.tar.gz
# Restart H2 server
```

### Clear Database
```bash
cd /data/code/gt/gracefulShutdown/db
# Stop H2 server first (Ctrl+C)
rm -f central-metrics.mv.db central-metrics.trace.db
# Restart H2 server (will create fresh DB)
```

### Check Database Size
```bash
ls -lh /data/code/gt/gracefulShutdown/db/central-metrics.mv.db
```

## Advanced Configuration

### Custom Database Port
Edit `db/start-db.sh` and change:
```bash
DB_PORT=9092  # Change to desired port
```

### Custom Database Name
Edit `db/start-db.sh`:
```bash
DB_NAME="my-database"
```

Then update `application.properties`:
```properties
spring.datasource.url=jdbc:h2:tcp://localhost:9092/./db/my-database;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

### Enable H2 Web Console
Update `application.properties`:
```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Then access: `http://localhost:8080/h2-console`

**Connection Details:**
```
JDBC URL: jdbc:h2:tcp://localhost:9092/./db/central-metrics
User Name: sa
Password: (leave empty)
```

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 9092
lsof -i :9092

# Kill the process
kill -9 <PID>

# Or use different port
# Edit start-db.sh: DB_PORT=9093
```

### Database Connection Refused
```bash
# Make sure H2 server is running
# Terminal 1: cd db && ./start-db.sh

# Then start Central in Terminal 2
java -jar central/target/central-transaction-server-1.0.0.jar
```

### Database File Locked
```bash
# Another H2 instance might be using it
# Check: lsof | grep central-metrics

# Solution: Stop all H2 instances, then restart
```

### Permission Denied
```bash
# Make sure start-db.sh is executable
chmod +x /data/code/gt/gracefulShutdown/db/start-db.sh

# Make sure db folder is writable
chmod 755 /data/code/gt/gracefulShutdown/db
```

### H2 JAR Not Found
The script auto-downloads H2 from Maven Central. If download fails:

```bash
cd /data/code/gt/gracefulShutdown/db

# Manual download
curl -o h2-2.2.220.jar \
  https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar

# Then run start-db.sh
./start-db.sh
```

## Production Considerations

### Security
1. **Database Credentials**
   - Current: Username `sa`, No password
   - Production: Add strong password
   ```bash
   # In start-db.sh
   java ... -user sa -password YourStrongPassword123
   
   # In application.properties
   spring.datasource.password=YourStrongPassword123
   ```

2. **Network Access**
   - Restrict port 9092 to localhost only
   - Use firewall to block external access
   ```bash
   iptables -A INPUT -p tcp --dport 9092 -s localhost -j ACCEPT
   iptables -A INPUT -p tcp --dport 9092 -j DROP
   ```

3. **Database Files**
   - Restrict permissions on db folder
   ```bash
   chmod 700 /data/code/gt/gracefulShutdown/db
   chmod 600 /data/code/gt/gracefulShutdown/db/central-metrics.mv.db
   ```

### Monitoring
```bash
# Monitor database file size
watch 'ls -lh /data/code/gt/gracefulShutdown/db/central-metrics.mv.db'

# Monitor database connections
lsof -p $(pgrep -f "java.*central")
```

### Backup Strategy
```bash
# Daily backup
0 2 * * * /data/code/gt/gracefulShutdown/backup-db.sh
```

Create `/data/code/gt/gracefulShutdown/backup-db.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/data/code/gt/gracefulShutdown/backups"
mkdir -p "$BACKUP_DIR"
tar -czf "$BACKUP_DIR/central-metrics-$(date +%Y%m%d_%H%M%S).tar.gz" \
  /data/code/gt/gracefulShutdown/db/central-metrics.mv.db
```

## Performance Tuning

### Database Pool Size
Update `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Query Optimization
```properties
# Cache query results
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## Docker Support

### Dockerfile
```dockerfile
FROM openjdk:26-slim

# Install curl for H2 download
RUN apt-get update && apt-get install -y curl

# Create app directory
WORKDIR /app

# Copy database setup
COPY db/start-db.sh /app/db/
RUN chmod +x /app/db/start-db.sh

# Copy central JAR
COPY central/target/central-transaction-server-1.0.0.jar /app/

# Expose ports
EXPOSE 8080 5009 9092

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/central/api/health || exit 1

# Start command
CMD ["/bin/bash", "-c", \
  "cd /app/db && ./start-db.sh & \
   sleep 5 && \
   cd /app && \
   java -jar central-transaction-server-1.0.0.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  h2-database:
    image: openjdk:26-slim
    volumes:
      - ./db:/app/db
    ports:
      - "9092:9092"
    command: "bash /app/db/start-db.sh"

  central-server:
    build: .
    ports:
      - "8080:8080"
      - "5009:5009"
    depends_on:
      - h2-database
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:tcp://h2-database:9092/./db/central-metrics;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

## Verification Steps

1. **Database Server Running**
   ```bash
   # Should see H2 startup messages
   tail -f /path/to/console.log
   ```

2. **Central Server Connected**
   ```bash
   # Should show database connection success in logs
   grep -i "database\|h2" central.log
   ```

3. **API Responding**
   ```bash
   curl http://localhost:8080/central/api/health | jq
   ```

4. **Database Files Exist**
   ```bash
   ls -lh /data/code/gt/gracefulShutdown/db/
   ```

## Summary

✅ **H2 Database in Standalone Mode**
✅ **Centralized db/ folder**
✅ **Automatic startup script**
✅ **Persistent data storage**
✅ **Easy backup/restore**
✅ **Production-ready configuration**
✅ **Docker support included**

---

**Next Steps:**
1. Run: `cd db && ./start-db.sh`
2. In new terminal: `java -jar central/target/central-transaction-server-1.0.0.jar`
3. Test: `curl http://localhost:8080/central/api/health | jq`

**For detailed troubleshooting**: See section above
