#!/bin/bash

# Master Build Script - Builds all servers
# Usage: ./build-all.sh [clean]

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "════════════════════════════════════════════════════════════════════════════"
echo "Building All Servers"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""

CLEAN_FLAG=""
if [ "$1" = "clean" ]; then
    CLEAN_FLAG="clean"
fi

# Build Central
echo "┌─────────────────────────────────────────────────────────────────────────┐"
echo "│ CENTRAL SERVER                                                          │"
echo "└─────────────────────────────────────────────────────────────────────────┘"
cd "$BASE_DIR/central"
./build.sh $CLEAN_FLAG
echo ""

# Build PROCR
echo "┌─────────────────────────────────────────────────────────────────────────┐"
echo "│ PROCR SERVER                                                            │"
echo "└─────────────────────────────────────────────────────────────────────────┘"
cd "$BASE_DIR/procr"
./build.sh $CLEAN_FLAG
echo ""

# Build Adapter
echo "┌─────────────────────────────────────────────────────────────────────────┐"
echo "│ ADAPTER SERVER                                                          │"
echo "└─────────────────────────────────────────────────────────────────────────┘"
cd "$BASE_DIR/adapter"
./build.sh $CLEAN_FLAG
echo ""

echo "════════════════════════════════════════════════════════════════════════════"
echo "✅ ALL SERVERS BUILT SUCCESSFULLY"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""
echo "To start all servers, open 3 terminals and run:"
echo ""
echo "  Terminal 1: cd central && ./run.sh"
echo "  Terminal 2: cd procr && ./run.sh"
echo "  Terminal 3: cd adapter && ./run.sh"
echo ""
echo "Or use the master start script (not yet implemented)"
echo ""
