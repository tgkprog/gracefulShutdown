#!/bin/bash

# H2 Database Server Start Script
# Starts H2 database in standalone server mode
# Database files stored in: ./db/

set -e

DB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_NAME="central-metrics"
DB_PORT=9092
DB_USER="sa"
DB_PASSWORD=""

echo "╔════════════════════════════════════════════════════════╗"
echo "║         H2 DATABASE SERVER - STANDALONE MODE           ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "📁 Database Directory: $DB_DIR"
echo "📊 Database Name: $DB_NAME"
echo "🔌 TCP Port: $DB_PORT"
echo "👤 Username: $DB_USER"
echo ""

# Check if H2 JAR exists
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21+."
    exit 1
fi

# Download H2 if not present
H2_JAR="$DB_DIR/h2-2.2.220.jar"
if [ ! -f "$H2_JAR" ]; then
    echo "📥 Downloading H2 database (2.2.220)..."
    cd "$DB_DIR"
    curl -s -o h2-2.2.220.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar
    if [ $? -ne 0 ]; then
        echo "❌ Failed to download H2. Please download manually:"
        echo "   https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar"
        exit 1
    fi
    echo "✅ H2 downloaded successfully"
fi

# Create database file if not exists
if [ ! -f "$DB_DIR/central-metrics.mv.db" ]; then
    echo "📝 Creating new database: $DB_NAME"
fi

echo ""
echo "🚀 Starting H2 Database Server..."
echo "───────────────────────────────────────────────────────"
echo ""
echo "Server is running on:"
echo "  TCP: localhost:$DB_PORT"
echo "  Database: $DB_NAME"
echo "  Files: $DB_DIR/"
echo ""
echo "Connection string:"
echo "  jdbc:h2:tcp://localhost:$DB_PORT/$DB_DIR/$DB_NAME"
echo ""
echo "To stop the server: Press Ctrl+C"
echo ""
echo "───────────────────────────────────────────────────────"
echo ""

# Start H2 server
cd "$DB_DIR"
java -cp "$H2_JAR" org.h2.tools.Server \
    -tcp \
    -tcpPort $DB_PORT \
    -baseDir "$DB_DIR" \
    -trace

