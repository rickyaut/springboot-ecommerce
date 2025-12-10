#!/bin/sh

# Start Vault in background
docker-entrypoint.sh vault server -dev -dev-root-token-id=myroot -dev-listen-address=0.0.0.0:8200 &

# Wait for Vault to be ready
sleep 5

# Set Vault environment
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=myroot

# Load secrets from .env file if it exists, otherwise use defaults
if [ -f /vault/.env ]; then
    . /vault/.env
    echo "Loading secrets from .env file"
else
    echo "No .env file found, using default values"
    ORDER_DB_USERNAME="order"
    ORDER_DB_PASSWORD="password"
    PAYMENT_DB_USERNAME="payment"
    PAYMENT_DB_PASSWORD="password"
    JWT_SECRET="demo-secret-key-for-local-development-only-replace-in-production"
    SLACK_WEBHOOK_URL=""
    REDIS_PASSWORD=""
fi

# Create secrets for order-service
vault kv put secret/order-service \
    spring.datasource.username="$ORDER_DB_USERNAME" \
    spring.datasource.password="$ORDER_DB_PASSWORD" \
    jwt.secret="$JWT_SECRET" \
    slack.webhook.url="$SLACK_WEBHOOK_URL"

# Create secrets for payment-service  
vault kv put secret/payment-service \
    spring.datasource.username="$PAYMENT_DB_USERNAME" \
    spring.datasource.password="$PAYMENT_DB_PASSWORD" \
    spring.data.redis.password="$REDIS_PASSWORD"
    
echo "Vault secrets initialized successfully"

# Keep Vault running in foreground
wait