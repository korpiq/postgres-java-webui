#!/bin/bash

# Script to start the backend Java application

set -e

WORKSPACE_DIR="$(pwd)"
PID_FILE="$WORKSPACE_DIR/backend.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null; then
        echo "Backend is already running (PID: $PID)"
        exit 0
    else
        rm "$PID_FILE"
    fi
fi

echo "Building backend..."
./gradlew build -x test

echo "Starting backend..."
# Use .secrets directory for keys as seen in scripts/run-tests.sh
JWT_PRIVATE_KEY="$WORKSPACE_DIR/.secrets/jwt_key"
JWT_PUBLIC_KEY="$WORKSPACE_DIR/.secrets/jwt_public_key.pem"

# Ensure keys exist
if [ ! -f "$JWT_PRIVATE_KEY" ]; then
    echo "JWT keys not found. Generating..."
    ./scripts/generate-jwt-keys.sh
fi

CLASSPATH=$(./gradlew -q -I scripts/init-classpath.gradle printClasspath)

java -DJWT_PRIVATE_KEY="$JWT_PRIVATE_KEY" \
     -DJWT_PUBLIC_KEY="$JWT_PUBLIC_KEY" \
     -cp "$CLASSPATH" \
     fi.iki.korpiq.pogrejab.App > backend.log 2>&1 &

PID=$!
echo $PID > "$PID_FILE"

echo "Backend started with PID $PID. Logs are in backend.log"

# Wait for backend to be ready
echo "Waiting for backend to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0
until curl -s http://localhost:8080/health | grep -q "OK"; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo "Backend failed to start"
        exit 1
    fi
    sleep 1
done

echo "Backend is ready!"
