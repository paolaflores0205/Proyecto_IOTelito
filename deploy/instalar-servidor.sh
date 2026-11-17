#!/bin/bash
# IoTelito - instalación de la web de taxistas en una EC2 Ubuntu 24.04 (user-data).
# Se ejecuta una sola vez, en el primer arranque. Registro: /var/log/taxiweb-instalacion.log
#
# Antes de usarlo se reemplazan:
#   __BUCKET__   bucket con el .jar y los scripts de db/ (prefijo deploy/taxiweb/)
#   __REGION__   región de AWS (us-east-1)
# La contraseña del superadmin se lee de SSM Parameter Store (/iotelito/taxiweb/admin-password,
# SecureString). La de MySQL se genera aquí y solo queda en /etc/taxiweb.env (root, 600).
set -euo pipefail
exec > /var/log/taxiweb-instalacion.log 2>&1
echo "Inicio: $(date -u)"

BUCKET="__BUCKET__"
REGION="__REGION__"
export DEBIAN_FRONTEND=noninteractive

# 1 GB de RAM no alcanza para MySQL y Java juntos sin un poco de swap.
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

apt-get update
apt-get install -y openjdk-21-jre-headless mysql-server unzip curl openssl

# AWS CLI v2 (Ubuntu 24.04 no la trae) para bajar el .jar y leer el parámetro.
if ! command -v aws >/dev/null; then
    curl -sSL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o /tmp/awscliv2.zip
    unzip -q /tmp/awscliv2.zip -d /tmp
    /tmp/aws/install
fi

# MySQL solo escucha en la propia máquina y con poca memoria.
cat > /etc/mysql/mysql.conf.d/zz-iotelito.cnf <<'EOF'
[mysqld]
bind-address = 127.0.0.1
performance_schema = OFF
innodb_buffer_pool_size = 128M
max_connections = 40
EOF
systemctl restart mysql

ADMIN_PASSWORD=$(aws ssm get-parameter --region "$REGION" --name /iotelito/taxiweb/admin-password \
    --with-decryption --query Parameter.Value --output text)
if [ "${#ADMIN_PASSWORD}" -lt 12 ]; then
    echo "ERROR: no se pudo leer la contraseña del superadmin de SSM; la web no se inicia."
    exit 1
fi

mkdir -p /opt/taxiweb/db
aws s3 cp "s3://$BUCKET/deploy/taxiweb/taxiweb.jar" /opt/taxiweb/taxiweb.jar --region "$REGION"
aws s3 cp "s3://$BUCKET/deploy/taxiweb/db/" /opt/taxiweb/db/ --recursive --region "$REGION"

# Base y usuario propio de la web (no root). Los scripts son los mismos del entregable.
DB_PASSWORD=$(openssl rand -hex 16)
mysql < /opt/taxiweb/db/00-crear-base.sql
mysql -e "CREATE USER IF NOT EXISTS 'taxiweb'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
          ALTER USER 'taxiweb'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
          GRANT SELECT, INSERT, UPDATE, DELETE ON iotelito.* TO 'taxiweb'@'localhost';
          FLUSH PRIVILEGES;"
mysql iotelito < /opt/taxiweb/db/schema.sql
mysql iotelito < /opt/taxiweb/db/01-chat-y-avisos-taxi.sql
mysql iotelito < /opt/taxiweb/db/data.sql

id taxiweb >/dev/null 2>&1 || useradd --system --home /opt/taxiweb --shell /usr/sbin/nologin taxiweb
chown -R taxiweb:taxiweb /opt/taxiweb

umask 077
cat > /etc/taxiweb.env <<EOF
DB_URL=jdbc:mysql://localhost:3306/iotelito?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
DB_USER=taxiweb
DB_PASSWORD=${DB_PASSWORD}
IOTELITO_ADMIN_PASSWORD=${ADMIN_PASSWORD}
FIREBASE_PROJECT_ID=iot-telito
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_THYMELEAF_CACHE=true
EOF
chmod 600 /etc/taxiweb.env
unset ADMIN_PASSWORD DB_PASSWORD

cat > /etc/systemd/system/taxiweb.service <<'EOF'
[Unit]
Description=IoTelito - web y API de gestion de taxistas
After=network-online.target mysql.service
Wants=network-online.target
Requires=mysql.service

[Service]
User=taxiweb
EnvironmentFile=/etc/taxiweb.env
ExecStart=/usr/bin/java -Xms128m -Xmx384m -Duser.timezone=America/Lima -jar /opt/taxiweb/taxiweb.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now taxiweb
echo "Fin: $(date -u)"
