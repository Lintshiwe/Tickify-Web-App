#!/usr/bin/env bash
set -euo pipefail

# Deploy Tickify to an existing Ubuntu EC2 VM over SSH.
# Usage:
#   EC2_HOST=ec2-xx-xx-xx-xx.region.compute.amazonaws.com \
#   EC2_USER=ubuntu \
#   SSH_KEY_PATH=$HOME/Downloads/SladeTickify.pem \
#   ./deploy/aws-ec2/deploy-to-ec2.sh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EC2_HOST="${EC2_HOST:-}"
EC2_USER="${EC2_USER:-ubuntu}"
SSH_KEY_PATH="${SSH_KEY_PATH:-$HOME/Downloads/SladeTickify.pem}"
REMOTE_DIR="${REMOTE_DIR:-/home/$EC2_USER/Tickify-SWP-Web-App_Copy}"

# Default to direct app mode on port 8080 for smaller VMs.
NGINX_ENABLED="${NGINX_ENABLED:-false}"

if [ "$NGINX_ENABLED" = "true" ]; then
  APP_BASE_URL="http://$EC2_HOST"
else
  APP_BASE_URL="http://$EC2_HOST:8080/Tickify-SWP-Web-App"
fi
LOGO_URL="$APP_BASE_URL/assets/tickify-logo.svg"

if [ -z "$EC2_HOST" ]; then
  echo "Missing EC2_HOST" >&2
  exit 1
fi

if [ ! -f "$SSH_KEY_PATH" ]; then
  echo "SSH key not found: $SSH_KEY_PATH" >&2
  exit 1
fi

SSH_OPTS=(
  -o BatchMode=yes
  -o ConnectTimeout=90
  -o ConnectionAttempts=6
  -o ServerAliveInterval=15
  -o ServerAliveCountMax=12
  -o StrictHostKeyChecking=no
  -o UserKnownHostsFile=/dev/null
  -i "$SSH_KEY_PATH"
)

echo "Syncing project to $EC2_USER@$EC2_HOST ..."
rsync -az --delete \
  --exclude '.git' \
  --exclude 'build' \
  --exclude 'dist' \
  -e "ssh ${SSH_OPTS[*]}" \
  "$PROJECT_ROOT/" "$EC2_USER@$EC2_HOST:$REMOTE_DIR/"

echo "Running setup + deploy on EC2 ..."
ssh "${SSH_OPTS[@]}" "$EC2_USER@$EC2_HOST" \
  "set -euo pipefail; cd '$REMOTE_DIR'; chmod +x deploy/oracle-free-tier/setup-vm.sh deploy/oracle-free-tier/deploy-app.sh; ./deploy/oracle-free-tier/setup-vm.sh; NGINX_ENABLED='$NGINX_ENABLED' ./deploy/oracle-free-tier/deploy-app.sh; \
  sudo mkdir -p /opt/tickify/config; sudo touch /opt/tickify/config/tickify.env; \
  if sudo grep -q '^TICKIFY_APP_BASE_URL=' /opt/tickify/config/tickify.env; then \
    sudo sed -i \"s|^TICKIFY_APP_BASE_URL=.*|TICKIFY_APP_BASE_URL=$APP_BASE_URL|\" /opt/tickify/config/tickify.env; \
  else \
    echo \"TICKIFY_APP_BASE_URL=$APP_BASE_URL\" | sudo tee -a /opt/tickify/config/tickify.env >/dev/null; \
  fi; \
  if sudo grep -q '^TICKIFY_LOGO_URL=' /opt/tickify/config/tickify.env; then \
    sudo sed -i \"s|^TICKIFY_LOGO_URL=.*|TICKIFY_LOGO_URL=$LOGO_URL|\" /opt/tickify/config/tickify.env; \
  else \
    echo \"TICKIFY_LOGO_URL=$LOGO_URL\" | sudo tee -a /opt/tickify/config/tickify.env >/dev/null; \
  fi; \
  sudo chown tickify:tickify /opt/tickify/config/tickify.env; sudo chmod 640 /opt/tickify/config/tickify.env; sudo systemctl restart tickify"

echo "Checking service health ..."
ssh "${SSH_OPTS[@]}" "$EC2_USER@$EC2_HOST" \
  "set -euo pipefail; echo tickify:\$(sudo systemctl is-active tickify || true); if [ '$NGINX_ENABLED' = 'true' ]; then echo nginx:\$(sudo systemctl is-active nginx || true); curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/; else curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/Tickify-SWP-Web-App/; fi"

if [ "$NGINX_ENABLED" = "true" ]; then
  echo "Done. Open: http://$EC2_HOST/"
else
  echo "Done. Open: http://$EC2_HOST:8080/Tickify-SWP-Web-App/"
fi
