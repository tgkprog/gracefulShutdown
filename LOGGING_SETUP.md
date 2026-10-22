# Structured Logging & Run Scripts

**Implementation complete**: Individual run scripts with structured date-based logging for all servers.

## Overview

Each server (Central, PROCR, Adapter) now has:
1. **Individual run file** (no .sh extension) - Bash script to launch the server
2. **Logback configuration** - XML-based logging with automatic rollover
3. **Structured log paths** - Date-organized directories: `../log/<component>/yyyy/MMM/dd/`
4. **5MB file rollover** - Automatic log file rollover when reaching 5MB
5. **Plain .log format** - No compression, easy to tail and view

## Run Files

### Location & Usage

```bash
central/run      # Start Central Server (port 5000)
procr/run        # Start PROCR Server (port 8080)
adapter/run      # Start Adapter Server (port 9090)
```

### Examples

```bash
# Start Central Server with default port (5000)
cd central && ./run

# Start Central Server with custom port
cd central && PORT=9999 ./run

# Start PROCR Server with default port (8080)
cd procr && ./run

# Start in background and redirect to another terminal
cd adapter && nohup ./run &
```

## Log Directory Structure

```
gracefulShutdown/
├── log/
│   ├── central/
│   │   ├── 2026/
│   │   │   ├── Oct/
│   │   │   │   ├── 23/
│   │   │   │   │   ├── central-20261023-022900-1.log
│   │   │   │   │   ├── central-20261023-045612-1.log
│   │   │   │   │   └── central-20261023-051234-2.log
│   │   │   │   └── 24/
│   │   │   │       └── central-20261024-000001-1.log
│   │   │   └── Nov/
│   │   │       └── ...
│   │
│   ├── procr/
│   │   ├── 2026/
│   │   │   ├── Oct/
│   │   │   │   └── 23/
│   │   │   │       ├── procr-20261023-022924-1.log (5MB)
│   │   │   │       ├── procr-20261023-022924-2.log (2.1MB)
│   │   │   │       └── procr-20261023-051400-1.log
│   │
│   └── adapter/
│       ├── 2026/
│       │   ├── Oct/
│       │   │   └── 23/
│       │   │       ├── adapter-20261023-023001-1.log
│       │   │       └── adapter-20261023-045300-1.log
```

**Naming Convention:**
- `<component>-yyyyMMdd-HHmmss-i.log`
- Example: `procr-20261023-022924-1.log` = PROCR server, Oct 23 2026, 02:29:24, sequence 1

## Log Rollover Behavior

### When Rollover Occurs:
1. **Size-based**: When a log file reaches **5MB**
2. **Time-based**: When the date changes (daily rollover at midnight)

### Rollover Action:
- Old file is archived with sequence number: `procr-20261023-022924-1.log`
- New file created with same timestamp: `procr-20261023-022924-2.log`
- When date changes: `procr-20261024-000001-1.log` in new date directory

### Archive Retention:
- Keep last **30 days** of logs
- Total size cap: **1GB** across all logs
- Files older than 30 days auto-delete

## Logback Configuration

Each server includes `src/main/resources/logback.xml`:

### Key Features:
```xml
<!-- Rolling File Appender with Date-based Directory Structure -->
<fileNamePattern>
  ${LOG_DIR}/%d{yyyy/MMM/dd}/central-%d{yyyyMMdd-HHmmss}-%i.log
</fileNamePattern>

<!-- Roll on file size -->
<maxFileSize>5MB</maxFileSize>

<!-- Retention policies -->
<maxHistory>30</maxHistory>           <!-- Keep 30 days -->
<totalSizeCap>1GB</totalSizeCap>      <!-- Cap at 1GB -->
```

### Log Levels:
- **Root**: INFO (production logs)
- **Application packages**: DEBUG (detailed diagnostics)
- **Spring Framework**: INFO (framework logs)

### Appenders:
1. **CONSOLE** - Real-time output to terminal
2. **FILE** - Rolling file appender with date structure
3. **ASYNC_FILE** - Non-blocking async write (better performance)

## Master Start Script

Updated `start-all.sh` now uses the new run files:

```bash
./start-all.sh           # Start all servers
./start-all.sh clean     # Clean build + start all servers
```

Output shows structured log paths:
```
✅ ALL SERVERS STARTED
📝 Logs with rollover (5MB per file, plain .log format):
  Central: tail -f central/log/central/2026/Oct/23/*.log
  PROCR:   tail -f procr/log/procr/2026/Oct/23/*.log
  Adapter: tail -f adapter/log/adapter/2026/Oct/23/*.log
```

## Viewing Logs

### Real-time tail (latest today's logs):
```bash
tail -f central/log/central/2026/Oct/23/*.log
tail -f procr/log/procr/2026/Oct/23/*.log
tail -f adapter/log/adapter/2026/Oct/23/*.log
```

### View specific file:
```bash
cat central/log/central/2026/Oct/23/central-20261023-022900-1.log
```

### Count total logs by date:
```bash
find central/log -name "*.log" -type f | xargs wc -l | tail -1
```

### Archive old logs (manual):
```bash
# Move logs older than 7 days to archive
find central/log -name "*.log" -mtime +7 -exec mv {} central/logs-archive/ \;
```

## Kill/Restart Handling

The existing `kill.sh` script works with the new run files:

```bash
./kill.sh central     # Gracefully kill Central (SIGTERM then SIGKILL)
./kill.sh procr       # Gracefully kill PROCR
./kill.sh adapter     # Gracefully kill Adapter
./kill.sh all         # Kill all servers
```

When killed, the next `./run` invocation creates new log files in today's date directory.

## Environment Variables

### Override port during runtime:
```bash
PORT=9999 ./run       # PROCR uses port 9999 instead of 8080
```

### Custom log directory (not typical):
Would require modifying `logback.xml`, but default paths work for most use cases.

## Deployment Checklist

- [x] Individual run files created (central/run, procr/run, adapter/run)
- [x] Logback XML configuration in each server
- [x] Date-based directory structure (yyyy/MMM/dd)
- [x] 5MB file rollover implemented
- [x] Plain .log format (no compression)
- [x] 30-day retention policy
- [x] 1GB total size cap
- [x] Console + File appenders
- [x] Async file writing for performance
- [x] start-all.sh updated to use new run files
- [x] kill.sh compatibility verified

## Performance Notes

- **Async appenders** queue log writes (512 item buffer) → better throughput
- **Logback XML** is parsed once at startup
- **Rolling** happens in-process, no external tools needed
- **Date formatting** uses efficient thread-safe formatters

## Troubleshooting

### Logs not appearing:
```bash
# Check if run script has execute permission
ls -la central/run  # Should show: -rwxrwxr-x

# Check JAR contains logback.xml
jar tf central/target/central-transaction-server-1.0.0.jar | grep logback.xml

# Force logback debug output (edit logback.xml):
<configuration debug="true">
```

### Permission denied on log directory:
```bash
# Ensure log parent directory is writable
mkdir -p log/central && chmod 775 log/central
```

### Old logs not auto-deleting:
```bash
# Manual cleanup (keep logs with modification time < 7 days old):
find log -type f -name "*.log" -mtime +30 -delete
```

## Next Steps

- Monitor rollover behavior in production
- Integrate metrics collection hooks from business logic
- Implement coordinated restart on rollover (if needed)
- Set up centralized log aggregation (ELK/Splunk)
