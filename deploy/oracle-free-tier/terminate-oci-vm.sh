#!/usr/bin/env bash
set -euo pipefail

# Terminates an OCI instance created for Tickify deployment.

OCI_PROFILE="${OCI_PROFILE:-DEFAULT}"
OCI_CONFIG_FILE="${OCI_CONFIG_FILE:-$HOME/.oci/config}"
INSTANCE_ID="${INSTANCE_ID:-}"

if [ -z "$INSTANCE_ID" ]; then
  echo "Missing INSTANCE_ID" >&2
  exit 1
fi

oci --profile "$OCI_PROFILE" --config-file "$OCI_CONFIG_FILE" \
  compute instance terminate \
  --instance-id "$INSTANCE_ID" \
  --preserve-boot-volume false \
  --force \
  --wait-for-state TERMINATED

echo "Instance terminated: $INSTANCE_ID"
