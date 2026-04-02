#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# Patterns aimed at accidental secret commits.
# Detect known key formats and suspicious literal assignments.
PATTERN='(BEGIN (RSA|OPENSSH|EC) PRIVATE KEY|aws_access_key_id[[:space:]]*=[[:space:]]*[A-Z0-9]{16,}|aws_secret_access_key[[:space:]]*=[[:space:]]*[A-Za-z0-9/+]{20,}|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{35}|xox[baprs]-[A-Za-z0-9-]{10,}|(password|secret|token|api[_-]?key)[[:space:]]*[:=][[:space:]]*[\"\047][^\"\047]{8,}[\"\047]|(TICKIFY_DB_PASSWORD|TICKIFY_SMTP_PASSWORD|TICKIFY_SMTP_USER)[[:space:]]*=[[:space:]]*[A-Za-z0-9_@.:-]{6,})'

EXCLUDES=(
  "./.git/*"
  "./build/*"
  "./dist/*"
  "./.cpqdevkit/*"
  "./nbproject/build-impl.xml"
  "./nbproject/ant-deploy.xml"
  "./deploy/oracle-free-tier/tickify.env.example"
  "./deploy/oracle-free-tier/pre-push-sensitive-scan.sh"
  "./docs/ORACLE_CLOUD_FREE_TIER_DEPLOYMENT.md"
  "./test/*"
)

find_args=(.)
for ex in "${EXCLUDES[@]}"; do
  find_args+=( -path "$ex" -prune -o )
done
find_args+=( -type f -print )

# shellcheck disable=SC2016
if find "${find_args[@]}" | xargs -r grep -nEI "$PATTERN" > /tmp/tickify_sensitive_scan.txt; then
  echo "Potential sensitive entries found:"
  cat /tmp/tickify_sensitive_scan.txt
  echo
  echo "Review and redact before pushing."
  exit 2
fi

echo "No high-risk sensitive patterns found in scanned files."
