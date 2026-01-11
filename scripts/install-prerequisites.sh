#!/bin/bash

# Script to install prerequisites for postgres-java-webui project on Ubuntu Linux
# Installs: Docker Engine, Docker Compose plugin, Java (OpenJDK), Node.js, and npm

set -e

echo "==============================================="
echo "Installing prerequisites for postgres-java-webui"
echo "==============================================="
echo ""

# Check if running as root
if [ "$EUID" -eq 0 ]; then
   echo "Please do not run this script as root. Use your regular user account."
   echo "The script will prompt for sudo password when needed."
   exit 1
fi

# Update package index
echo "Updating package index..."
sudo apt-get update

# Install dependencies
echo "Installing basic dependencies..."
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Install Docker
echo ""
echo "Installing Docker..."
if command -v docker &> /dev/null; then
    echo "Docker is already installed: $(docker --version)"
else
    # Add Docker's official GPG key
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Set up Docker repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker Engine and Docker Compose plugin
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Add current user to docker group
    sudo usermod -aG docker $USER

    echo "Docker installed successfully: $(docker --version)"
    echo "Docker Compose installed successfully: $(docker compose version)"
    echo ""
    echo "NOTE: You need to log out and log back in (or run 'newgrp docker') for docker group membership to take effect."
fi

# Install Java (OpenJDK)
echo ""
echo "Installing Java..."
if command -v java &> /dev/null; then
    echo "Java is already installed: $(java -version 2>&1 | head -n 1)"
else
    sudo apt-get install -y openjdk-25-jdk
    echo "Java installed successfully: $(java -version 2>&1 | head -n 1)"
fi

# Install Node.js and npm
echo ""
echo "Installing Node.js and npm..."
if command -v node &> /dev/null; then
    echo "Node.js is already installed: $(node --version)"
    echo "npm is already installed: $(npm --version)"
else
    # Install Node.js 25.x
    curl -fsSL https://deb.nodesource.com/setup_25.x | sudo -E bash -
    sudo apt-get install -y nodejs
    echo "Node.js installed successfully: $(node --version)"
    echo "npm installed successfully: $(npm --version)"
fi

echo ""
echo "==============================================="
echo "Installation complete!"
echo "==============================================="
echo ""
echo "Installed versions:"
docker --version 2>/dev/null || echo "Docker: Not available in current session"
docker compose version 2>/dev/null || echo "Docker Compose: Not available in current session"
java -version 2>&1 | head -n 1
node --version
npm --version
echo ""
echo "IMPORTANT: If Docker was just installed, you must:"
echo "  1. Log out and log back in, OR"
echo "  2. Run: newgrp docker"
echo ""
echo "After that, you can start the database with: ./setup-db.sh start"
