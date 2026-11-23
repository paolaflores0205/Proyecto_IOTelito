# Costo total de la solución (OPEX) — IoTelito

Costo **mensual de operación** de los recursos que hoy usa el sistema desplegado, calculado con
precios de lista *on-demand* de la región **us-east-1 (N. Virginia)** y 730 horas al mes.

> **Supuestos.** Precios de lista de AWS y Firebase vigentes al redactar este documento
> (noviembre de 2026); pueden cambiar. Antes de entregar, recalcular en la
> [AWS Pricing Calculator](https://calculator.aws/) con estos mismos recursos.
> Tipo de cambio referencial: **US$ 1 = S/ 3.75**. Uso de una demostración académica:
> pocos usuarios, unas cientos de reservas y algunos viajes de taxi al mes.

## 1. Recursos y costo mensual

### Web de gestión de taxistas (cuenta AWS 1)

| Recurso | Configuración real | Cálculo | US$/mes |
|---|---|---|---|
| EC2 | `t3.micro` (2 vCPU, 1 GiB), Amazon Linux 2023, encendida 24/7 | 730 h × 0.0104 | 7.59 |
| EBS (disco de la EC2) | gp3, 8 GB | 8 × 0.08 | 0.64 |
| IP pública IPv4 (Elastic IP) | 1 dirección | 730 h × 0.005 | 3.65 |
| RDS MySQL | `db.t3.micro`, MySQL 8.4, una zona (sin Multi-AZ) | 730 h × 0.017 | 12.41 |
| Almacenamiento RDS | gp2, 20 GB | 20 × 0.115 | 2.30 |
| Respaldos RDS | 1 día de retención | gratis hasta el tamaño de la base | 0.00 |
| API Gateway (HTTP API) `iot-telito-taxiweb` | ~300 000 solicitudes/mes (la app consulta el viaje cada pocos segundos) | 0.3 M × 1.00 | 0.30 |
| S3 `iotelito-taxiweb-…` | ~0.3 GB (documentos de taxistas y versiones del .jar) | 0.3 × 0.023 | 0.01 |
| Systems Manager Parameter Store | 2 parámetros estándar | nivel estándar gratis | 0.00 |
| **Subtotal** | | | **26.90** |

### Fotos y notificaciones (cuenta AWS 2)

| Recurso | Configuración real | Cálculo | US$/mes |
|---|---|---|---|
| Lambda `iot-telito-imagenes-api` | Node.js, 256 MB, se llama al subir o ver fotos | dentro de la capa gratuita permanente (1 M solicitudes y 400 000 GB-s) | 0.00 |
| Lambda `iot-telito-notificaciones` | Node.js, 256 MB, **cada minuto** = 43 800 ejecuciones/mes (~11 000 GB-s) | dentro de la capa gratuita permanente; sin ella serían ~US$ 0.19 | 0.00 |
| EventBridge (regla programada) | `rate(1 minute)` | las reglas programadas no tienen costo | 0.00 |
| API Gateway (HTTP API) `iot-telito-imagenes` | ~10 000 solicitudes/mes | 0.01 M × 1.00 | 0.01 |
| S3 `iot-telito-imagenes` | fotos de perfil y de hoteles (< 1 GB) | < 1 × 0.023 | 0.02 |
| Secrets Manager | 1 secreto (cuenta de servicio de Firebase) | 1 × 0.40 + llamadas | 0.45 |
| CloudWatch Logs | registros de las Lambdas (< 1 GB) | dentro de los 5 GB gratuitos | 0.00 |
| **Subtotal** | | | **0.48** |

### Firebase (proyecto `iot-telito`)

| Servicio | Uso | Plan | US$/mes |
|---|---|---|---|
| Authentication (correo y contraseña) | < 50 000 usuarios activos al mes | Spark (gratuito) | 0.00 |
| Cloud Firestore | < 50 000 lecturas, 20 000 escrituras y 20 000 borrados al día; < 1 GiB | Spark (gratuito) | 0.00 |
| Cloud Messaging (FCM) | notificaciones push | gratuito | 0.00 |
| **Subtotal** | | | **0.00** |

> Las fotos se guardan en S3 y no en Firebase Storage, por lo que el proyecto no necesita el plan
> de pago Blaze. Confirmar el plan en la consola de Firebase (*Uso y facturación*).

### Transferencia de datos

Salida a internet menor a 100 GB/mes, cubierta por la capa gratuita de AWS: **US$ 0.00**.

## 2. Total

| Concepto | US$/mes | S/ /mes | US$/año |
|---|---|---|---|
| Web de taxistas (EC2 + RDS + API + S3) | 26.90 | 100.88 | 322.80 |
| Fotos y notificaciones (Lambda, S3, Secrets Manager) | 0.48 | 1.80 | 5.76 |
| Firebase | 0.00 | 0.00 | 0.00 |
| **Total OPEX** | **≈ 27.38** | **≈ 102.68** | **≈ 328.56** |

**Qué pesa más:** la base de datos RDS (≈ 54 %) y la EC2 con su IP pública (≈ 41 %). Todo lo
serverless (Lambda, API Gateway, Firebase) cuesta casi nada con este nivel de uso.

**En el curso:** los recursos están en **AWS Academy Learner Lab**, que se paga con los créditos
académicos de la cuenta del laboratorio. El costo real para el equipo es **S/ 0**.

## 3. Escenario con más usuarios (referencial)

Si el sistema pasara a producción con alta disponibilidad, los cambios más probables serían:

| Cambio | Efecto aproximado |
|---|---|
| RDS Multi-AZ (dos zonas) | duplica el costo de RDS: +US$ 14.7/mes |
| EC2 `t3.small` (2 GiB) para la web | +US$ 7.6/mes |
| Firebase Blaze si se supera la cuota gratuita (p. ej. 2 millones de lecturas extra al mes) | +US$ 1.20/mes (US$ 0.06 por cada 100 000 lecturas) |
| Compromiso de 1 año (Savings Plans / instancias reservadas) para EC2 y RDS | −30 % a −40 % en esos dos recursos |

## 4. Cómo reducir el costo

- Apagar la EC2 y el RDS fuera del horario de uso (en el laboratorio se apagan solos al terminar la sesión).
- Borrar de S3 los `.jar` antiguos de `deploy/` (ocupan la mayor parte del bucket).
- Bajar la frecuencia del emisor de avisos (de 1 a 2 minutos) si no se necesita inmediatez.
- Unir las dos cuentas AWS en una sola para administrar un único conjunto de recursos.

## Fuentes de precios

- EC2: <https://aws.amazon.com/ec2/pricing/on-demand/> · IPv4 pública: <https://aws.amazon.com/vpc/pricing/>
- RDS MySQL: <https://aws.amazon.com/rds/mysql/pricing/> · EBS: <https://aws.amazon.com/ebs/pricing/>
- API Gateway: <https://aws.amazon.com/api-gateway/pricing/> · Lambda: <https://aws.amazon.com/lambda/pricing/>
- S3: <https://aws.amazon.com/s3/pricing/> · Secrets Manager: <https://aws.amazon.com/secrets-manager/pricing/>
- Firebase: <https://firebase.google.com/pricing>
