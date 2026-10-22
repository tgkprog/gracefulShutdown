#!/bin/bash

# Master Start Script - Starts all servers in background with structured logging
# Usage: ./start-all.sh [clean]
# Options:
#   clean - Build before starting

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PIDS_FILE="/tmp/gracefulshutdown_pids.txt"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "════════════════════════════════════════════════════════════════════════════"
echo "Starting All Servers with Structured Logging"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""

# Build if requested
if [ "$1" = "clean" ]; then
    echo "Building all servers first..."
    "$BASE_DIR/build-all.sh" clean
    echo ""
fi

# Clear old PID file
> "$PIDS_FILE"

# Start Central (port 5000)
echo -e "${BLUE}▶ Starting Central Server (port 5000)${NC}"
cd "$BASE_DIR/central"
PORT=5000 ./run > /dev/null 2>&1 &
CENTRAL_PID=$!
echo $CENTRAL_PID >> "$PIDS_FILE"
echo -e "${GREEN}  PID: $CENTRAL_PID${NC}"
echo "  Logs: log/central/yyyy/MMM/dd/*.log"
echo ""

# Wait for Central to start
sleep 2

# Start PROCR (port 8080)
echo -e "${BLUE}▶ Starting PROCR Server (port 8080)${NC}"
cd "$BASE_DIR/procr"
PORT=8080 ./run > /dev/null 2>&1 &
PROCR_PID=$!
echo $PROCR_PID >> "$PIDS_FILE"
echo -e "${GREEN}  PID: $PROCR_PID${NC}"
echo "  Logs: log/procr/yyyy/MMM/dd/*.log"
echo ""

# Wait for PROCR to start
sleep 2

# Start Adapter (port 9090)
echo -e "${BLUE}▶ Starting Adapter Server (port 9090)${NC}"
cd "$BASE_DIR/adapter"
PORT=9090 ./run > /dev/null 2>&1 &
ADAPTER_PID=$!
echo $ADAPTER_PID >> "$PIDS_FILE"
echo -e "${GREEN}  PID: $ADAPTER_PID${NC}"
echo "  Logs: log/adapter/yyyy/MMM/dd/*.log"
echo ""

sleep 2

echo "════════════════════════════════════════════════════════════════════════════"
echo -e "${GREEN}✅ ALL SERVERS STARTED${NC}"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""
echo "📊 Dashboards:"
echo "  Central Dashboard: ${BLUE}http://localhost:5000/central/dashboard${NC}"
echo ""
echo "🔌 APIs:"
echo "  PROCR Sleep Config: ${BLUE}http://localhost:8080/config/sleep${NC}"
echo ""
echo "📝 Logs with rollover (5MB per file, plain .log format):"
echo "  Central: tail -f central/log/central/$(date +%Y/%b/%d)/*.log"
echo "  PROCR:   tail -f procr/log/procr/$(date +%Y/%b/%d)/*.log"
echo "  Adapter: tail -f adapter/log/adapter/$(date +%Y/%b/%d)/*.log"
echo ""
echo "🛑 To stop all: ./kill.sh all"
echo "📁 PIDs saved: $PIDS_FILE"
echo ""

