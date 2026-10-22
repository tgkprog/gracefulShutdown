# Getting Started - Graceful Shutdown Framework

## 🚀 30-Second Quick Start

```bash
cd /data/code/gt/gracefulShutdown

# Build all servers
./build-all.sh clean

# Start all servers
./start-all.sh

# Open dashboard
# http://localhost:5000/central/dashboard
```

That's it! All three servers are now running.

---

## 📊 What You're Running

Three microservices working together:

1. **Central Server** (Port 5000)
   - Real-time metrics dashboard
   - SQLite database for persistent storage
   - REST API endpoints

2. **PROCR Server** (Port 8080)
   - Transaction processor with approval logic
   - **Runtime sleep configuration** (NO RESTART NEEDED)
   - Responds to transaction requests

3. **Adapter Server** (Port 9090)
   - JSON/YAML converter
   - TCP/HTTP gateway
   - Bridges Central and PROCR

---

## 🎯 What Can You Do?

### View Real-Time Dashboard
```
http://localhost:5000/central/dashboard
```
- Live metrics (updated every 30 seconds)
- Historical data with 4 time buckets
- Status cards showing connection/session state
- Transaction approval rates with charts

### Change PROCR Response Delays (No Restart!)
In the dashboard, scroll to bottom:
1. **"Get From Server"** - Fetch current sleep settings
2. Modify: Fixed delay, random min/max, step
3. **"Post to Server"** - Apply immediately
4. Next transaction will use new settings!

### API Access
```bash
# Get PROCR sleep config
curl http://localhost:8080/config/sleep | jq

# Update PROCR sleep config
curl -X POST http://localhost:8080/config/sleep \
  -H "Content-Type: application/json" \
  -d '{
    "fixed_ms": 100,
    "random_min_ms": 50,
    "random_step_ms": 25,
    "random_max_ms": 200
  }' | jq

# Get Central status
curl http://localhost:5000/central/api/status | jq
```

---

## 📁 Key Files & Directories

```
gracefulShutdown/
├── build-all.sh          ← Build all servers at once
├── start-all.sh          ← Start all in background
├── kill.sh               ← Stop servers by name or pattern
│
├── central/
│   ├── build.sh          ← Build Central only
│   ├── run.sh            ← Run Central (port 5000)
│   └── target/           ← Compiled JAR
│
├── procr/
│   ├── build.sh          ← Build PROCR only
│   ├── run.sh            ← Run PROCR (port 8080)
│   └── target/           ← Compiled JAR
│
├── adapter/
│   ├── build.sh          ← Build Adapter only
│   ├── run.sh            ← Run Adapter (port 9090)
│   └── target/           ← Compiled JAR
│
└── docs/
    ├── BUILD_AND_RUN.md           ← Script usage guide
    ├── SLEEP_CONFIG_FEATURE.md    ← Sleep timing control
    ├── SCRIPTS_SUMMARY.md         ← Complete reference
    └── DASHBOARD_*.md             ← Dashboard guides
```

---

## 🛠️ Common Commands

### Build
```bash
./build-all.sh              # Build all servers
./build-all.sh clean        # Clean build (remove old artifacts)
cd central && ./build.sh    # Build Central only
```

### Start
```bash
./start-all.sh              # Start all in background
cd central && ./run.sh      # Run Central in foreground
cd procr && ./run.sh 9000   # Run PROCR on custom port 9000
```

### Stop
```bash
./kill.sh all               # Kill all servers
./kill.sh central           # Kill Central only
./kill.sh "java.*8080"      # Kill by pattern
```

### Monitor
```bash
# Watch all logs
tail -f /tmp/central.log &
tail -f /tmp/procr.log &
tail -f /tmp/adapter.log

# Check running servers
ps aux | grep java | grep -v grep

# Check ports
netstat -tlnp | grep -E "5000|8080|9090"
```

---

## 💡 Example: Test Different Response Times

### Scenario: "How does the system handle slow PROCR?"

1. **Start servers**
   ```bash
   ./start-all.sh
   ```

2. **Open dashboard**
   ```
   http://localhost:5000/central/dashboard
   ```

3. **Scroll to "PROCR Sleep Configuration"**

4. **Get current settings**
   - Click "Get From Server"
   - Fields show: Fixed: 0, Min: 0, Step: 100, Max: 200

5. **Make it slow**
   - Change Fixed to: 5000 (5 seconds)
   - Change Step to: 1000
   - Change Max to: 5000
   - Click "Post to Server"
   - Message shows success

6. **Send a transaction**
   - Start session and send transactions
   - Watch logs show 5-10 second delays
   - See metrics update in dashboard
   - No server restart needed!

7. **Change it back**
   - Set Fixed to: 0
   - Click "Post to Server"
   - Delays immediately back to 0-200ms

---

## 🐛 Troubleshooting

### "Port already in use"
```bash
# Kill existing process
./kill.sh all

# Or find specific process
lsof -i :5000
kill <PID>
```

### "Build fails"
```bash
# Check Java version (need 25+)
java -version

# Clean build
./build-all.sh clean
```

### "Dashboard won't load"
```bash
# Check if Central is running
ps aux | grep central

# Check logs
tail -f /tmp/central.log

# Restart Central
./kill.sh central
cd central && ./run.sh
```

### "Config changes not applying"
```bash
# Check PROCR is running
curl http://localhost:8080/config/sleep

# Verify in dashboard
# Click "Get From Server" again
```

---

## 📚 Documentation

For detailed information:

| File | Contains |
|------|----------|
| `BUILD_AND_RUN.md` | Complete build & run guide (5 min read) |
| `SLEEP_CONFIG_FEATURE.md` | Sleep configuration details (10 min read) |
| `SCRIPTS_SUMMARY.md` | All script options & examples (15 min read) |
| `README_DASHBOARD.md` | Dashboard features & API (10 min read) |

---

## ⚡ Performance Notes

- **Build time**: 30-40 seconds (one-time)
- **Startup time**: 3-5 seconds
- **Dashboard refresh**: Every 30 seconds
- **Config change latency**: ~20ms (no restart!)
- **Memory usage**: ~1 GB total for all three

---

## 🔐 Security Notes

- CORS enabled for cross-origin requests (for development)
- All APIs on localhost by default
- No authentication (add to production)
- SQLite database local-only
- Graceful shutdown on SIGTERM

---

## 📖 Architecture Overview

```
User (Browser)
    ↓
Central Server (Port 5000)
    ├─ Dashboard UI
    ├─ REST APIs
    └─ SQLite Database
    
    ↓ (HTTP)
    
Adapter Server (Port 9090)
    ├─ JSON/YAML Converter
    └─ TCP/HTTP Gateway
    
    ↓ (HTTP)
    
PROCR Server (Port 8080)
    ├─ Transaction Processor
    ├─ Approval Engine
    ├─ Sleep Config API ← Runtime config changes here!
    └─ Response Delay Calculator
```

All three can run on the same machine (using different ports) or on separate machines.

---

## 🎓 Learning Paths

### New to the project?
1. Read: `GETTING_STARTED.md` (this file) - 5 min
2. Run: `./start-all.sh` - 30 seconds
3. Click: `http://localhost:5000/central/dashboard` - 2 min
4. Try: Change sleep settings in dashboard - 3 min
5. Read: `README_DASHBOARD.md` for more features - 10 min

### Want to modify code?
1. Read: Project structure - 10 min
2. Find: Which server to modify (central/procr/adapter)
3. Edit: Source code in `src/main/java`
4. Build: `cd <server> && ./build.sh clean`
5. Test: `./run.sh` or `./start-all.sh`

### Want to deploy?
1. Read: `SCRIPTS_SUMMARY.md` - 15 min
2. Prepare: Build artifacts are in `target/`
3. Deploy: Copy JARs to your server
4. Run: `java -jar central-transaction-server-1.0.0.jar`
5. Access: `http://<ip>:5000/central/dashboard`

---

## ✅ Verification Checklist

After starting servers, verify everything works:

- [ ] All three services start successfully
- [ ] Central dashboard loads: `http://localhost:5000/central/dashboard`
- [ ] Dashboard shows status cards with "No data" (expected initially)
- [ ] PROCR config API responds: `curl http://localhost:8080/config/sleep`
- [ ] Sleep config section visible in dashboard
- [ ] "Get From Server" button works
- [ ] Can update values and "Post to Server"
- [ ] Kill script works: `./kill.sh all`

---

## 🚀 Ready to Go!

Everything is built, tested, and ready to use.

**Next step**: Run `./start-all.sh` and open your browser! 🎉

---

## 📞 Quick Reference

```bash
# Start everything
./start-all.sh

# Dashboard
http://localhost:5000/central/dashboard

# Sleep config API
GET  http://localhost:8080/config/sleep
POST http://localhost:8080/config/sleep

# Stop everything
./kill.sh all

# See logs
tail -f /tmp/{central,procr,adapter}.log
```

Enjoy! 🚀
