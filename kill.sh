#!/bin/bash

# Process Kill Utility
# Usage: ./kill.sh [all|central|procr|adapter|pattern]
# Examples:
#   ./kill.sh all              - Kill all servers
#   ./kill.sh central          - Kill Central server only
#   ./kill.sh procr            - Kill PROCR server only
#   ./kill.sh adapter          - Kill Adapter server only
#   ./kill.sh "java.*8080"     - Kill by pattern (regex)

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function kill_by_pattern() {
    local pattern="$1"
    local server_name="$2"
    
    echo ""
    echo "🔍 Looking for: $server_name ($pattern)"
    
    # Find PID by searching java processes
    local pid=$(ps aux | grep -E "$pattern" | grep -v grep | awk '{print $2}' | head -1)
    
    if [ -z "$pid" ]; then
        echo "${YELLOW}⊘ Not running${NC}"
        return 0
    fi
    
    echo "Found PID: $pid"
    echo "Process: $(ps -p $pid -o cmd= | cut -c1-80)"
    
    echo "${YELLOW}Killing PID $pid...${NC}"
    kill -TERM "$pid"
    
    # Wait for graceful shutdown
    sleep 2
    
    # Check if process is still running
    if ps -p "$pid" > /dev/null 2>&1; then
        echo "${RED}Process still running, forcing kill...${NC}"
        kill -9 "$pid"
        sleep 1
    fi
    
    if ! ps -p "$pid" > /dev/null 2>&1; then
        echo "${GREEN}✓ Killed successfully${NC}"
    else
        echo "${RED}✗ Failed to kill${NC}"
        return 1
    fi
}

function kill_all() {
    echo "════════════════════════════════════════════════════════════════"
    echo "Killing All Servers"
    echo "════════════════════════════════════════════════════════════════"
    
    local errors=0
    
    kill_by_pattern "central.*5000" "Central (port 5000)" || errors=$((errors+1))
    kill_by_pattern "procr.*8080" "PROCR (port 8080)" || errors=$((errors+1))
    kill_by_pattern "adapter.*9090" "Adapter (port 9090)" || errors=$((errors+1))
    
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    if [ $errors -eq 0 ]; then
        echo "${GREEN}✅ All servers stopped${NC}"
    else
        echo "${YELLOW}⚠ Some servers may not have been running${NC}"
    fi
    echo "════════════════════════════════════════════════════════════════"
}

function kill_central() {
    echo "════════════════════════════════════════════════════════════════"
    echo "Killing Central Server"
    echo "════════════════════════════════════════════════════════════════"
    kill_by_pattern "central.*5000" "Central (port 5000)"
    echo ""
}

function kill_procr() {
    echo "════════════════════════════════════════════════════════════════"
    echo "Killing PROCR Server"
    echo "════════════════════════════════════════════════════════════════"
    kill_by_pattern "procr.*8080" "PROCR (port 8080)"
    echo ""
}

function kill_adapter() {
    echo "════════════════════════════════════════════════════════════════"
    echo "Killing Adapter Server"
    echo "════════════════════════════════════════════════════════════════"
    
    # First try by port pattern
    local pid=$(ps aux | grep -E "adapter.*9090" | grep -v grep | awk '{print $2}' | head -1)
    
    # If not found, try by JAR name
    if [ -z "$pid" ]; then
        pid=$(ps aux | grep -E "java.*adapter-service.*\.jar" | grep -v grep | awk '{print $2}' | head -1)
        if [ -n "$pid" ]; then
            echo "Found by JAR name: adapter-service"
        fi
    fi
    
    if [ -z "$pid" ]; then
        echo "${YELLOW}⊘ Adapter not running${NC}"
        return 0
    fi
    
    echo "Found PID: $pid"
    echo "Process: $(ps -p $pid -o cmd= | cut -c1-80)"
    
    echo "${YELLOW}Killing PID $pid...${NC}"
    kill -TERM "$pid"
    
    sleep 2
    
    if ps -p "$pid" > /dev/null 2>&1; then
        echo "${RED}Process still running, forcing kill...${NC}"
        kill -9 "$pid"
        sleep 1
    fi
    
    if ! ps -p "$pid" > /dev/null 2>&1; then
        echo "${GREEN}✓ Killed successfully${NC}"
    else
        echo "${RED}✗ Failed to kill${NC}"
        return 1
    fi
}

function kill_pattern() {
    local pattern="$1"
    echo "════════════════════════════════════════════════════════════════"
    echo "Killing by pattern: $pattern"
    echo "════════════════════════════════════════════════════════════════"
    
    local count=$(ps aux | grep -E "$pattern" | grep -v grep | wc -l)
    
    if [ "$count" -eq 0 ]; then
        echo "${YELLOW}No processes found matching: $pattern${NC}"
        echo ""
        return 0
    fi
    
    echo "${YELLOW}Found $count process(es)${NC}"
    ps aux | grep -E "$pattern" | grep -v grep
    echo ""
    
    while read -r line; do
        local pid=$(echo "$line" | awk '{print $2}')
        if [ ! -z "$pid" ]; then
            echo "${YELLOW}Killing PID $pid...${NC}"
            kill -TERM "$pid" 2>/dev/null || true
        fi
    done < <(ps aux | grep -E "$pattern" | grep -v grep)
    
    sleep 2
    
    # Force kill any remaining
    while read -r line; do
        local pid=$(echo "$line" | awk '{print $2}')
        if [ ! -z "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
            kill -9 "$pid" 2>/dev/null || true
        fi
    done < <(ps aux | grep -E "$pattern" | grep -v grep)
    
    echo "${GREEN}✓ Done${NC}"
    echo ""
}

# Main
if [ "$#" -eq 0 ]; then
    echo "Usage: $0 [all|central|procr|adapter|pattern]"
    echo ""
    echo "Examples:"
    echo "  $0 all              # Kill all servers"
    echo "  $0 central          # Kill Central only"
    echo "  $0 procr            # Kill PROCR only"
    echo "  $0 adapter          # Kill Adapter only"
    echo "  $0 'java.*8080'     # Kill by pattern"
    echo ""
    exit 0
fi

case "$1" in
    all)
        kill_all
        ;;
    central)
        kill_central
        ;;
    procr)
        kill_procr
        ;;
    adapter)
        kill_adapter
        ;;
    *)
        kill_pattern "$1"
        ;;
esac
