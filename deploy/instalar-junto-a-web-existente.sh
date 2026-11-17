#!/bin/bash
# Instala la web nueva de taxistas JUNTO a la de Mariana, sin tocar lo suyo:
# su base iotelito, sus .jar en /home/ec2-user, el puerto 8080 y la contrasena de admin quedan igual.
set -euo pipefail
REGION=us-east-1
BUCKET=iotelito-taxiweb-102343456181
RDS=iotelito-db.cktph5qnfls0.us-east-1.rds.amazonaws.com

echo "== Swap de 1 GB (913 MB de RAM no alcanzan para dos webs Java)"
if ! swapon --show --noheadings | grep -q /swapfile; then
  [ -f /swapfile ] || { dd if=/dev/zero of=/swapfile bs=1M count=1024 status=none; chmod 600 /swapfile; mkswap /swapfile >/dev/null; }
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
swapon --show --noheadings

echo "== Archivos desde S3"
mkdir -p /opt/taxiweb/db
aws s3 cp "s3://$BUCKET/deploy/taxiweb.jar" /opt/taxiweb/taxiweb.jar --region "$REGION" --only-show-errors
aws s3 cp "s3://$BUCKET/deploy/db/" /opt/taxiweb/db/ --recursive --region "$REGION" --only-show-errors
ls -la /opt/taxiweb /opt/taxiweb/db | awk '{print $5, $9}'

ADMIN_WEB=$(aws ssm get-parameter --region "$REGION" --name /iotelito/taxiweb/admin-password --with-decryption --query Parameter.Value --output text)
[ "${#ADMIN_WEB}" -ge 12 ] || { echo "ERROR: no pude leer la contrasena del superadmin"; exit 1; }

echo "== Conexion a RDS con admin"
SSL="--ssl"
[ -f /home/ec2-user/global-bundle.pem ] && SSL="--ssl-ca=/home/ec2-user/global-bundle.pem --ssl-verify-server-cert"
M="mysql -h $RDS -P 3306 -u admin $SSL"
for intento in 1 2 3; do
  read -rsp "Contrasena de admin de RDS (no se muestra al escribir): " MYSQL_PWD </dev/tty; echo
  export MYSQL_PWD
  if $M -N -e "SELECT 1" >/dev/null 2>&1; then echo "Contrasena correcta"; break; fi
  echo "RDS rechazo la contrasena (intento $intento de 3)"; unset MYSQL_PWD
  [ "$intento" = 3 ] && { echo "No se cambio nada. Vuelve a correr el instalador cuando tengas la contrasena."; exit 1; }
done
$M -N -e "SELECT CONCAT('Conectado a MySQL ', VERSION())"
echo "Tablas de la base de Mariana (no se tocan): $($M -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='iotelito'")"

echo "== Base nueva iotelito_taxis y usuario taxiweb"
DB_PW=$(openssl rand -hex 16)
$M -e "CREATE DATABASE IF NOT EXISTS iotelito_taxis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'taxiweb'@'%' IDENTIFIED BY '$DB_PW' REQUIRE SSL;
ALTER USER 'taxiweb'@'%' IDENTIFIED BY '$DB_PW' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE ON iotelito_taxis.* TO 'taxiweb'@'%';"
$M iotelito_taxis < /opt/taxiweb/db/schema.sql
$M iotelito_taxis < /opt/taxiweb/db/data.sql
$M -N -e "SELECT CONCAT(table_name, ': ', table_rows) FROM information_schema.tables WHERE table_schema='iotelito_taxis' ORDER BY table_name"
echo "Tablas de la base de Mariana despues: $($M -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='iotelito'")"
unset MYSQL_PWD

echo "== Servicio taxiweb en el puerto 8081"
id taxiweb >/dev/null 2>&1 || useradd --system --home /opt/taxiweb --shell /sbin/nologin taxiweb
chown -R taxiweb:taxiweb /opt/taxiweb
( umask 077
cat > /etc/taxiweb.env <<EOF
SERVER_PORT=8081
DB_URL=jdbc:mysql://$RDS:3306/iotelito_taxis?sslMode=REQUIRED&serverTimezone=America/Lima
DB_USER=taxiweb
DB_PASSWORD=$DB_PW
IOTELITO_ADMIN_PASSWORD=$ADMIN_WEB
FIREBASE_PROJECT_ID=iot-telito
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_THYMELEAF_CACHE=true
EOF
)
chmod 600 /etc/taxiweb.env
unset DB_PW ADMIN_WEB

cat > /etc/systemd/system/taxiweb.service <<'EOF'
[Unit]
Description=IoTelito - web y API de taxistas (version nueva, puerto 8081)
After=network-online.target
Wants=network-online.target

[Service]
User=taxiweb
EnvironmentFile=/etc/taxiweb.env
ExecStart=/usr/bin/java -Xms96m -Xmx256m -XX:MaxMetaspaceSize=160m -Duser.timezone=America/Lima -jar /opt/taxiweb/taxiweb.jar
Restart=on-failure
RestartSec=15

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable taxiweb >/dev/null 2>&1
systemctl restart taxiweb

echo "== Esperando a que arranque"
for i in $(seq 1 50); do
  if curl -sf http://localhost:8081/api/v1/health >/dev/null; then echo "OK en ~$((i*3)) s: $(curl -s http://localhost:8081/api/v1/health)"; break; fi
  sleep 3
done
systemctl is-active taxiweb || true
journalctl -u taxiweb --no-pager -n 200 | grep -E 'Started TaxiwebApplication|ERROR|Schema-validation|Caused by' | tail -8 | cut -c1-300
free -m | head -2
