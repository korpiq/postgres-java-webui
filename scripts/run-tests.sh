#!/bin/bash

# Script to run Cucumber BDD tests

set -e

WORKSPACE_DIR="$PWD"

echo "========================================="
echo "Running Cucumber BDD Tests"
echo "========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Testcontainers requires Docker."
    echo "Please start Docker and try again."
    exit 1
fi

# Check if JWT keys exist
if [ ! -f "$WORKSPACE_DIR/.secrets/jwt_key" ]; then
    echo "Warning: JWT keys not found in .secrets/"
    echo "Generating JWT keys..."
    ./scripts/generate-jwt-keys.sh
    echo ""
fi

# Run the tests
echo "Running tests..."
./gradlew test --info

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ All tests passed!"
    echo "========================================="
    echo ""
    echo "Test reports available at:"
    echo "  - HTML: build/reports/cucumber/cucumber.html"
    echo "  - JSON: build/reports/cucumber/cucumber.json"
    echo "  - JUnit: build/reports/tests/test/index.html"
else
    echo ""
    echo "========================================="
    echo "❌ Tests failed!"
    echo "========================================="
    echo ""
    echo "Check the test reports for details:"
    echo "  - build/reports/tests/test/index.html"
    exit 1
fi