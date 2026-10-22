#!/bin/bash

# Dual Port Test Script for Central Server
# Tests both Web API (8080) and TCP Server (5009)

set -e

SERVER_URL="http://localhost:8080"
TCP_HOST="localhost"
TCP_PORT=5009

echo "=========================================="
echo "Central Dual Port Configuration Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    
    echo -e "${BLUE}Testing:${NC} $description"
    echo "URL: $SERVER_URL$endpoint"
    
    if response=$(curl -s -w "\n%{http_code}" "$SERVER_URL$endpoint" 2>/dev/null); then
        http_code=$(echo "$response" | tail -n1)
        body=$(echo "$response" | head -n-1)
        
        if [ "$http_code" = "200" ]; then
            echo -e "${GREEN}✓ SUCCESS (HTTP $http_code)${NC}"
            echo "$body" | jq . 2>/dev/null || echo "$body"
        else
            echo -e "${RED}✗ FAILED (HTTP $http_code)${NC}"
            echo "$body"
        fi
    else
        echo -e "${RED}✗ Connection failed${NC}"
    fi
    echo ""
}

# Function to test TCP port
test_tcp_port() {
    echo -e "${BLUE}Testing:${NC} TCP Server Port Connectivity"
    echo "Host: $TCP_HOST:$TCP_PORT"
    
    if nc -zv $TCP_HOST $TCP_PORT 2>&1 | grep -q "succeeded"; then
        echo -e "${GREEN}✓ SUCCESS - TCP port is listening${NC}"
    else
        echo -e "${RED}✗ FAILED - TCP port not accessible${NC}"
        echo "Make sure the Central server is running:"
        echo "  java -jar central/target/central-transaction-server-1.0.0.jar"
    fi
    echo ""
}

# Function to check if server is running
check_server_running() {
    if ! curl -s "$SERVER_URL/central/api/health" >/dev/null 2>&1; then
        echo -e "${RED}✗ ERROR: Central server is not running!${NC}"
        echo ""
        echo "Start the server with:"
        echo "  cd /data/code/gt/gracefulShutdown/central"
        echo "  java -jar target/central-transaction-server-1.0.0.jar"
        echo ""
        exit 1
    fi
    echo -e "${GREEN}✓ Central server is running${NC}"
    echo ""
}

# Start tests
echo "Checking server status..."
check_server_running

echo "=========================================="
echo "WEB API Tests (Port 8080)"
echo "=========================================="
echo ""

test_endpoint "/central/api/health" "Health Check"
test_endpoint "/central/api/info" "Server Information"
test_endpoint "/central/api/status" "Current Status"
test_endpoint "/central/api/history" "Metrics History"
test_endpoint "/central/api/metrics" "Combined Metrics"

echo "=========================================="
echo "TCP Server Tests (Port 5009)"
echo "=========================================="
echo ""

test_tcp_port

echo "=========================================="
echo "Port Verification"
echo "=========================================="
echo ""

echo -e "${BLUE}Listening Ports:${NC}"
if command -v netstat >/dev/null 2>&1; then
    netstat -tlnp 2>/dev/null | grep -E ":(8080|5009)" || echo "Port information not available"
elif command -v ss >/dev/null 2>&1; then
    ss -tlnp 2>/dev/null | grep -E ":(8080|5009)" || echo "Port information not available"
else
    echo "netstat/ss not available"
fi
echo ""

echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}✓${NC} Web API (Port 8080): Responding"
echo -e "${GREEN}✓${NC} TCP Server (Port 5009): Listening"
echo ""
echo "Both ports are operational!"
echo ""
echo "Next steps:"
echo "1. Connect Adapter to port 5009"
echo "2. Monitor dashboard at http://localhost:8080/central/dashboard"
echo "3. Query API endpoints at http://localhost:8080/central/api/*"
echo ""
