#!/usr/bin/env bash
set -euo pipefail

# Provisions an Oracle Cloud VM (in your OCI profile) and deploys Tickify automatically.
# Prereqs on local machine: oci cli, ssh, rsync
# Prereqs in OCI network: subnet route + security rules allow 22 and 80 ingress.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

OCI_PROFILE="${OCI_PROFILE:-DEFAULT}"
OCI_CONFIG_FILE="${OCI_CONFIG_FILE:-$HOME/.oci/config}"
COMPARTMENT_ID="${COMPARTMENT_ID:-}"
SUBNET_ID="${SUBNET_ID:-}"
VM_DISPLAY_NAME="${VM_DISPLAY_NAME:-tickify-free-$(date +%Y%m%d%H%M%S)}"
SHAPE="${SHAPE:-VM.Standard.E2.1.Micro}"
IMAGE_OS="${IMAGE_OS:-Canonical Ubuntu}"
IMAGE_OS_VERSION="${IMAGE_OS_VERSION:-22.04}"
IMAGE_ID="${IMAGE_ID:-}"
REMOTE_USER="${REMOTE_USER:-ubuntu}"
SSH_PUBLIC_KEY_PATH="${SSH_PUBLIC_KEY_PATH:-$HOME/.ssh/id_rsa.pub}"
SSH_PRIVATE_KEY_PATH="${SSH_PRIVATE_KEY_PATH:-$HOME/.ssh/id_rsa}"
APP_BASE_URL="${APP_BASE_URL:-}"

# Required app secrets
TICKIFY_DB_USER="${TICKIFY_DB_USER:-}"
TICKIFY_DB_PASSWORD="${TICKIFY_DB_PASSWORD:-}"
TICKIFY_DB_HOST="${TICKIFY_DB_HOST:-127.0.0.1}"
TICKIFY_DB_PORT="${TICKIFY_DB_PORT:-1527}"
TICKIFY_DB_NAME="${TICKIFY_DB_NAME:-tickifyDB}"

# Optional SMTP
TICKIFY_SMTP_HOST="${TICKIFY_SMTP_HOST:-}"
TICKIFY_SMTP_PORT="${TICKIFY_SMTP_PORT:-587}"
TICKIFY_SMTP_USER="${TICKIFY_SMTP_USER:-}"
TICKIFY_SMTP_PASSWORD="${TICKIFY_SMTP_PASSWORD:-}"
TICKIFY_SMTP_FROM="${TICKIFY_SMTP_FROM:-no-reply@tickify.ac.za}"
TICKIFY_SMTP_STARTTLS="${TICKIFY_SMTP_STARTTLS:-true}"
TICKIFY_SMTP_SSL="${TICKIFY_SMTP_SSL:-false}"
TICKIFY_LOGO_URL="${TICKIFY_LOGO_URL:-}"

require() {
  local name="$1"
  local value="$2"
  if [ -z "$value" ]; then
    echo "Missing required variable: $name" >&2
    exit 1
  fi
}

check_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing required command: $1" >&2; exit 1; }
}

check_cmd oci
check_cmd ssh
check_cmd rsync

require COMPARTMENT_ID "$COMPARTMENT_ID"
require SUBNET_ID "$SUBNET_ID"
require TICKIFY_DB_USER "$TICKIFY_DB_USER"
require TICKIFY_DB_PASSWORD "$TICKIFY_DB_PASSWORD"

if [ ! -f "$SSH_PUBLIC_KEY_PATH" ]; then
  echo "SSH public key not found at $SSH_PUBLIC_KEY_PATH" >&2
  exit 1
fi
if [ ! -f "$SSH_PRIVATE_KEY_PATH" ]; then
  echo "SSH private key not found at $SSH_PRIVATE_KEY_PATH" >&2
  exit 1
fi

OCI_BASE=(oci --profile "$OCI_PROFILE" --config-file "$OCI_CONFIG_FILE")

echo "Resolving availability domain..."
AD="$(${OCI_BASE[@]} iam availability-domain list \
  --compartment-id "$COMPARTMENT_ID" \
  --query 'data[0].name' --raw-output)"

if [ -z "$AD" ] || [ "$AD" = "null" ]; then
  echo "Unable to resolve availability domain" >&2
  exit 1
fi

if [ -z "$IMAGE_ID" ]; then
  echo "Resolving latest Ubuntu image for shape $SHAPE ..."
  IMAGE_ID="$(${OCI_BASE[@]} compute image list \
    --compartment-id "$COMPARTMENT_ID" \
    --operating-system "$IMAGE_OS" \
    --operating-system-version "$IMAGE_OS_VERSION" \
    --shape "$SHAPE" \
    --sort-by TIMECREATED \
    --sort-order DESC \
    --query 'data[0].id' --raw-output)"
fi

if [ -z "$IMAGE_ID" ] || [ "$IMAGE_ID" = "null" ]; then
  echo "Unable to resolve image id. Set IMAGE_ID explicitly." >&2
  exit 1
fi

SSH_KEY_CONTENT="$(cat "$SSH_PUBLIC_KEY_PATH")"
METADATA_JSON=$(printf '{"ssh_authorized_keys":"%s"}' "${SSH_KEY_CONTENT//\"/\\\"}")

echo "Launching instance $VM_DISPLAY_NAME ..."
INSTANCE_ID="$(${OCI_BASE[@]} compute instance launch \
  --compartment-id "$COMPARTMENT_ID" \
  --availability-domain "$AD" \
  --display-name "$VM_DISPLAY_NAME" \
  --shape "$SHAPE" \
  --subnet-id "$SUBNET_ID" \
  --assign-public-ip true \
  --image-id "$IMAGE_ID" \
  --metadata "$METADATA_JSON" \
  --wait-for-state RUNNING \
  --query 'data.id' --raw-output)"

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "null" ]; then
  echo "Failed to launch instance" >&2
  exit 1
fi

echo "Instance created: $INSTANCE_ID"

VNIC_ID="$(${OCI_BASE[@]} compute instance list-vnics \
  --instance-id "$INSTANCE_ID" \
  --query 'data[0].id' --raw-output)"
PUBLIC_IP="$(${OCI_BASE[@]} network vnic get \
  --vnic-id "$VNIC_ID" \
  --query 'data."public-ip"' --raw-output)"

if [ -z "$PUBLIC_IP" ] || [ "$PUBLIC_IP" = "null" ]; then
  echo "Failed to resolve public IP" >&2
  exit 1
fi

if [ -z "$APP_BASE_URL" ]; then
  APP_BASE_URL="http://$PUBLIC_IP"
fi

echo "Waiting for SSH on $PUBLIC_IP ..."
for _ in $(seq 1 60); do
  if ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    -o ConnectTimeout=5 -i "$SSH_PRIVATE_KEY_PATH" "$REMOTE_USER@$PUBLIC_IP" 'echo ok' >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

echo "Copying project to VM ..."
rsync -az --delete \
  --exclude '.git' \
  --exclude 'build' \
  --exclude 'dist' \
  -e "ssh -i $SSH_PRIVATE_KEY_PATH -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" \
  "$PROJECT_ROOT/" "$REMOTE_USER@$PUBLIC_IP:/home/$REMOTE_USER/Tickify-SWP-Web-App_Copy/"

echo "Running setup and deployment on VM ..."
ssh -tt -i "$SSH_PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null "$REMOTE_USER@$PUBLIC_IP" <<EOF
set -euo pipefail
cd /home/$REMOTE_USER/Tickify-SWP-Web-App_Copy
chmod +x deploy/oracle-free-tier/setup-vm.sh deploy/oracle-free-tier/deploy-app.sh
./deploy/oracle-free-tier/setup-vm.sh

sudo tee /opt/tickify/config/tickify.env >/dev/null <<ENVVARS
JAVA_OPTS="-Xms256m -Xmx1024m -Djava.awt.headless=true -Dtickify.app.baseUrl=$APP_BASE_URL"
TICKIFY_DB_USER=$TICKIFY_DB_USER
TICKIFY_DB_PASSWORD=$TICKIFY_DB_PASSWORD
TICKIFY_DB_HOST=$TICKIFY_DB_HOST
TICKIFY_DB_PORT=$TICKIFY_DB_PORT
TICKIFY_DB_NAME=$TICKIFY_DB_NAME
TICKIFY_SMTP_HOST=$TICKIFY_SMTP_HOST
TICKIFY_SMTP_PORT=$TICKIFY_SMTP_PORT
TICKIFY_SMTP_USER=$TICKIFY_SMTP_USER
TICKIFY_SMTP_PASSWORD=$TICKIFY_SMTP_PASSWORD
TICKIFY_SMTP_FROM=$TICKIFY_SMTP_FROM
TICKIFY_SMTP_STARTTLS=$TICKIFY_SMTP_STARTTLS
TICKIFY_SMTP_SSL=$TICKIFY_SMTP_SSL
TICKIFY_LOGO_URL=$TICKIFY_LOGO_URL
ENVVARS

sudo chown tickify:tickify /opt/tickify/config/tickify.env
sudo chmod 640 /opt/tickify/config/tickify.env

./deploy/oracle-free-tier/deploy-app.sh
sudo systemctl status tickify --no-pager
EOF

cat <<OUT

Provision + deploy complete.
Instance ID: $INSTANCE_ID
Public IP:   $PUBLIC_IP
App URL:     $APP_BASE_URL/

To terminate later:
OCI_PROFILE="$OCI_PROFILE" OCI_CONFIG_FILE="$OCI_CONFIG_FILE" \
INSTANCE_ID="$INSTANCE_ID" ./deploy/oracle-free-tier/terminate-oci-vm.sh
OUT
