#!/usr/bin/env bash
set -euo pipefail

# Builds Tickify WAR and deploys it as a systemd service backed by Payara Micro.
# Run from project root.

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_USER="tickify"
APP_HOME="/opt/tickify"
APP_SRC="$APP_HOME/src/Tickify-SWP-Web-App_Copy"
APP_WAR="$APP_HOME/app/Tickify-SWP-Web-App.war"
NGINX_ENABLED="${NGINX_ENABLED:-false}"

sudo mkdir -p "$APP_HOME/src"
sudo rsync -a --delete \
  --exclude '.git' \
  --exclude 'build' \
  --exclude 'dist' \
  "$PROJECT_ROOT/" "$APP_SRC/"

sudo chown -R "$APP_USER":"$APP_USER" "$APP_HOME/src"

sudo -u "$APP_USER" bash <<'EOF'
set -euo pipefail
APP_SRC="/opt/tickify/src/Tickify-SWP-Web-App_Copy"
PROPS="$APP_SRC/nbproject/project.properties"
PRIVATE_PROPS="$APP_SRC/nbproject/private/private.properties"

# Patch developer-local jar references to VM-local libs.
sed -i "s|^file.reference.derbyclient.jar=.*|file.reference.derbyclient.jar=/opt/tickify/lib/derbyclient.jar|" "$PROPS"
sed -i "s|^file.reference.mysql-connector-j-9.6.0.jar=.*|file.reference.mysql-connector-j-9.6.0.jar=/opt/tickify/lib/mysql-connector-j.jar|" "$PROPS"

if [ -f "$PRIVATE_PROPS" ]; then
  sed -i "s|^file.reference.derbyclient.jar=.*|file.reference.derbyclient.jar=/opt/tickify/lib/derbyclient.jar|" "$PRIVATE_PROPS"
  sed -i "s|^file.reference.mysql-connector-j-9.6.0.jar=.*|file.reference.mysql-connector-j-9.6.0.jar=/opt/tickify/lib/mysql-connector-j.jar|" "$PRIVATE_PROPS"
fi

cd "$APP_SRC"
ant -q \
  -Dlibs.CopyLibs.classpath=/opt/tickify/lib/org-netbeans-modules-java-j2seproject-copylibstask.jar \
  -Djavac.classpath=/opt/tickify/lib/mysql-connector-j.jar:/opt/tickify/lib/derbyclient.jar:/opt/tickify/lib/derby.jar:/opt/tickify/lib/javaee-api.jar \
  clean dist
cp -f dist/Tickify-SWP-Web-App.war /opt/tickify/app/Tickify-SWP-Web-App.war

# Ensure embedded Derby driver is present inside WAR for runtime fallback mode.
TMP_WAR_DIR="$(mktemp -d)"
mkdir -p "$TMP_WAR_DIR/WEB-INF/lib"
cp -f /opt/tickify/lib/derby.jar "$TMP_WAR_DIR/WEB-INF/lib/derby.jar"
(cd "$TMP_WAR_DIR" && jar uf /opt/tickify/app/Tickify-SWP-Web-App.war WEB-INF/lib/derby.jar)
rm -rf "$TMP_WAR_DIR"
EOF

sudo cp "$PROJECT_ROOT/deploy/oracle-free-tier/tickify.service" /etc/systemd/system/tickify.service
sudo cp "$PROJECT_ROOT/deploy/oracle-free-tier/payara-postboot.asadmin" "$APP_HOME/config/payara-postboot.asadmin"
if [ ! -f "$APP_HOME/config/tickify.env" ]; then
  sudo cp "$PROJECT_ROOT/deploy/oracle-free-tier/tickify.env.example" "$APP_HOME/config/tickify.env"
fi
sudo chown "$APP_USER":"$APP_USER" "$APP_HOME/config/tickify.env"
sudo chown "$APP_USER":"$APP_USER" "$APP_HOME/config/payara-postboot.asadmin"
sudo chmod 640 "$APP_HOME/config/tickify.env"
sudo chmod 640 "$APP_HOME/config/payara-postboot.asadmin"

sudo systemctl daemon-reload
sudo systemctl enable tickify
sudo systemctl restart tickify

if [ "$NGINX_ENABLED" = "true" ]; then
  sudo cp "$PROJECT_ROOT/deploy/oracle-free-tier/nginx-tickify.conf" /etc/nginx/sites-available/tickify
  sudo ln -sf /etc/nginx/sites-available/tickify /etc/nginx/sites-enabled/tickify
  sudo rm -f /etc/nginx/sites-enabled/default
  sudo nginx -t
  sudo systemctl enable nginx
  sudo systemctl restart nginx
else
  sudo systemctl stop nginx >/dev/null 2>&1 || true
  sudo systemctl disable nginx >/dev/null 2>&1 || true
fi

if [ "$NGINX_ENABLED" = "true" ]; then
  echo "Deploy complete. App should be reachable at http://<vm-public-ip>/"
else
  echo "Deploy complete. App should be reachable at http://<vm-public-ip>:8080/Tickify-SWP-Web-App/"
fi
