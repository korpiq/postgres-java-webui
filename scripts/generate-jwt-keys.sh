#!/bin/bash

# Script to generate RSA public/private key pair for JWT signing
# Uses ssh-keygen to generate keys

set -e

WORKSPACE_DIR="$PWD"
KEYS_DIR="$WORKSPACE_DIR/.secrets"
PRIVATE_KEY="$KEYS_DIR/jwt_key"
PUBLIC_KEY="$KEYS_DIR/jwt_key.pub"
CONFIG_FILE="$WORKSPACE_DIR/src/main/resources/application.properties"

echo "Generating JWT signing keys..."

# Create keys directory if it doesn't exist
mkdir -p "$KEYS_DIR"

# Generate RSA key pair using ssh-keygen
# -t rsa: key type
# -b 2048: key size
# -f: output file
# -N "": no passphrase
# -C: comment
ssh-keygen -t rsa -b 2048 -f "$PRIVATE_KEY" -N "" -C "pogrejab-jwt-signing-key"

echo "Keys generated successfully:"
echo "  Private key: $PRIVATE_KEY"
echo "  Public key: $PUBLIC_KEY"

# Convert SSH public key to PEM format for easier use in Java
ssh-keygen -f "$PUBLIC_KEY" -e -m pem > "$KEYS_DIR/jwt_public_key.pem"
echo "  Public key (PEM): $KEYS_DIR/jwt_public_key.pem"

# Convert private key to PKCS8 format for Java compatibility
ssh-keygen -p -f "$PRIVATE_KEY" -m pem -N ""
echo "  Private key converted to PEM format"

# Update application.properties with key paths
echo ""
echo "Updating $CONFIG_FILE with key paths..."

# Check if jwt.privateKey already exists in config
if grep -q "jwt.privateKey" "$CONFIG_FILE"; then
    echo "JWT key configuration already exists in application.properties"
else
    # Append JWT configuration
    cat >> "$CONFIG_FILE" << EOF

# JWT Configuration
jwt.privateKey=$PRIVATE_KEY
jwt.publicKey=$KEYS_DIR/jwt_public_key.pem
jwt.expirationMs=3600000
EOF
    echo "JWT configuration added to application.properties"
fi

echo ""
echo "Key generation complete!"
echo ""
echo "IMPORTANT: The keys directory is excluded from version control."
echo "Make sure to back up your keys securely."
