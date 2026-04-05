#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <domain> [email]"
  echo "Example: $0 app.example.com admin@example.com"
  exit 1
fi

DOMAIN="$1"
EMAIL="${2:-}"
NGINX_SITE="/etc/nginx/sites-available/tickify"

if [[ ! -f "$NGINX_SITE" ]]; then
  echo "Nginx site config not found at $NGINX_SITE"
  exit 1
fi

echo "Installing Certbot prerequisites..."
sudo apt-get update -y >/dev/null
sudo apt-get install -y certbot python3-certbot-nginx >/dev/null

echo "Updating Nginx server_name to $DOMAIN..."
sudo sed -i "s/^\s*server_name\s\+.*;/    server_name $DOMAIN;/" "$NGINX_SITE"

echo "Validating and reloading Nginx..."
sudo nginx -t
sudo systemctl reload nginx

echo "Requesting Let's Encrypt certificate for $DOMAIN..."
if [[ -n "$EMAIL" ]]; then
  sudo certbot --nginx -d "$DOMAIN" --agree-tos -m "$EMAIL" --non-interactive --redirect
else
  sudo certbot --nginx -d "$DOMAIN" --agree-tos --register-unsafely-without-email --non-interactive --redirect
fi

echo "Final Nginx validation..."
sudo nginx -t
sudo systemctl reload nginx

echo "Done. Active certificates:"
sudo certbot certificates
