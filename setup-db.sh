#!/bin/bash

# Setup script to create and/or start local Postgres instance

set -e

case "$1" in
  start)
    echo "Starting Postgres instance..."
    docker compose up -d
    echo "Waiting for Postgres to be ready..."
    docker compose exec -T postgres pg_isready -U appuser -d appdb || sleep 2
    echo "Postgres is ready!"
    echo "Database: appdb"
    echo "User: appuser"
    echo "Password: apppass"
    echo "Port: 5432"
    ;;
  stop)
    echo "Stopping Postgres instance..."
    docker compose down
    echo "Postgres stopped."
    ;;
  restart)
    echo "Restarting Postgres instance..."
    docker compose restart
    echo "Postgres restarted."
    ;;
  reset)
    echo "Resetting Postgres instance (will delete all data)..."
    docker compose down -v
    docker compose up -d
    echo "Postgres reset complete."
    ;;
  logs)
    docker compose logs -f postgres
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|reset|logs}"
    echo ""
    echo "Commands:"
    echo "  start   - Start the Postgres container"
    echo "  stop    - Stop the Postgres container"
    echo "  restart - Restart the Postgres container"
    echo "  reset   - Stop and remove all data, then start fresh"
    echo "  logs    - Show Postgres logs"
    exit 1
    ;;
esac
