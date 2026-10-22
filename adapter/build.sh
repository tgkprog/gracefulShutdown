#!/bin/bash

# Adapter Server Build Script
# Usage: ./build.sh [clean]
# Options:
#   clean - Perform clean build (removes target directory)

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "════════════════════════════════════════════════════════════════"
echo "Building Adapter Server"
echo "════════════════════════════════════════════════════════════════"
echo ""

if [ "$1" = "clean" ]; then
    echo "🧹 Cleaning previous build..."
    mvn clean
    echo ""
fi

echo "📦 Building with Maven..."
mvn package -DskipTests

if [ -f "target/adapter-json-yaml-converter-1.0.0.jar" ]; then
    JAR_SIZE=$(du -h "target/adapter-json-yaml-converter-1.0.0.jar" | cut -f1)
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "✅ BUILD SUCCESSFUL"
    echo "════════════════════════════════════════════════════════════════"
    echo "JAR: target/adapter-json-yaml-converter-1.0.0.jar ($JAR_SIZE)"
    echo ""
    echo "To start: ./run.sh"
    echo ""
else
    echo "❌ BUILD FAILED - JAR not found"
    exit 1
fi
