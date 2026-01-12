#!/bin/bash

# Script to stop the backend Java application

WORKSPACE_DIR="$(pwd)"
PID_FILE="$WORKSPACE_DIR/backend.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    echo "Stopping backend (PID: $PID)..."
    kill "$PID" || true
    rm "$PID_FILE"
    echo "Backend stopped."
else
    echo "Backend PID file not found. It might not be running."
fi
