#!/usr/bin/env bash
set -euo pipefail

# Installs runtime/build dependencies and prepares Tickify directories on Oracle Cloud Free Tier VM.

APP_USER="tickify"
APP_HOME="/opt/tickify"
LIB_DIR="$APP_HOME/lib"

sudo apt-get update
sudo apt-get install -y openjdk-11-jdk ant nginx curl unzip rsync ca-certificates

HOST_NAME="$(hostname)"
if ! grep -qE "[[:space:]]$HOST_NAME(\\s|$)" /etc/hosts; then
  echo "127.0.1.1 $HOST_NAME" | sudo tee -a /etc/hosts >/dev/null
fi

if ! id -u "$APP_USER" >/dev/null 2>&1; then
  sudo useradd -r -m -d "$APP_HOME" -s /bin/bash "$APP_USER"
fi

sudo mkdir -p "$APP_HOME"/{app,bin,config,logs,src} "$LIB_DIR"
sudo chown -R "$APP_USER":"$APP_USER" "$APP_HOME"

sudo -u "$APP_USER" bash <<'EOF'
set -euo pipefail
LIB_DIR="/opt/tickify/lib"

if [ ! -f "$LIB_DIR/payara-micro.jar" ]; then
  curl -fL -o "$LIB_DIR/payara-micro.jar" "https://repo1.maven.org/maven2/fish/payara/extras/payara-micro/5.2022.5/payara-micro-5.2022.5.jar"
fi

if [ ! -f "$LIB_DIR/derbyclient.jar" ]; then
  curl -fL -o "$LIB_DIR/derbyclient.jar" "https://repo1.maven.org/maven2/org/apache/derby/derbyclient/10.14.2.0/derbyclient-10.14.2.0.jar"
fi

if ! jar tf "$LIB_DIR/derbyclient.jar" 2>/dev/null | grep -q 'org/apache/derby/jdbc/ClientDriver.class'; then
  curl -fL -o "$LIB_DIR/derbyclient.jar" "https://repo1.maven.org/maven2/org/apache/derby/derbyclient/10.14.2.0/derbyclient-10.14.2.0.jar"
fi

if [ ! -f "$LIB_DIR/derby.jar" ]; then
  curl -fL -o "$LIB_DIR/derby.jar" "https://repo1.maven.org/maven2/org/apache/derby/derby/10.14.2.0/derby-10.14.2.0.jar"
fi

if ! jar tf "$LIB_DIR/derby.jar" 2>/dev/null | grep -q 'org/apache/derby/jdbc/EmbeddedDriver.class'; then
  curl -fL -o "$LIB_DIR/derby.jar" "https://repo1.maven.org/maven2/org/apache/derby/derby/10.14.2.0/derby-10.14.2.0.jar"
fi

if [ ! -f "$LIB_DIR/mysql-connector-j.jar" ]; then
  curl -fL -o "$LIB_DIR/mysql-connector-j.jar" "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
fi

if [ ! -f "$LIB_DIR/javaee-api.jar" ]; then
  curl -fL -o "$LIB_DIR/javaee-api.jar" "https://repo1.maven.org/maven2/javax/javaee-api/8.0.1/javaee-api-8.0.1.jar"
fi

if [ ! -f "$LIB_DIR/org-netbeans-modules-java-j2seproject-copylibstask.jar" ]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  curl -fL -o "$TMP_DIR/netbeans-bin.zip" "https://archive.apache.org/dist/netbeans/netbeans/22/netbeans-22-bin.zip"
  unzip -j "$TMP_DIR/netbeans-bin.zip" "netbeans/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar" -d "$LIB_DIR"
  rm -rf "$TMP_DIR"
  trap - EXIT
fi
EOF

echo "VM setup complete. Next: run deploy/oracle-free-tier/deploy-app.sh"
