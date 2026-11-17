# Sistema web de gestión de taxistas - IoTelito

Spring Boot 3.5 (Java 17 o superior) con MySQL 8. El taxista se autorregistra aquí y el
superadmin lo aprueba desde el panel. La app móvil usa Firestore para todo lo demás y
consultará lo del taxi por la API REST del contrato `docs/api-taxis-v1.yaml` (rama `main`).

## Base de datos

En MySQL Workbench, ejecutar en este orden:

1. `db/00-crear-base.sql`: crea la base `iotelito`.
2. `db/schema.sql` (DDL): 8 tablas, solo lo del taxi. Borra y recrea las tablas.
3. `db/data.sql` (DML): datos de prueba (superadmin, 10 taxistas, documentos, pedidos,
   servicios, ubicaciones y calificaciones).

Hoteles, reservas, checkout y chat no están aquí: viven en Firestore. Por eso `reserva_id`,
`hotel_id` y `cliente_uid` guardan ids de Firestore sin llave foránea.

La web arranca con `ddl-auto=validate`: si las entidades y `schema.sql` no coinciden, no
inicia. Cualquier cambio de tablas se hace primero en `schema.sql`.

## Ejecutar

Las credenciales van en variables de entorno, nunca en el repositorio:

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `DB_URL` | JDBC de MySQL | `jdbc:mysql://localhost:3306/iotelito?...` |
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | vacía |
| `IOTELITO_ADMIN_PASSWORD` | Contraseña del superadmin en producción (12 caracteres o más) | no se usa |
| `FIREBASE_PROJECT_ID` | Proyecto de Firebase cuyos tokens acepta la API | `iot-telito` |

```bash
./mvnw spring-boot:run
```

Luego abrir http://localhost:8080. Superadmin de desarrollo de `data.sql`:
`admin@iotelito.com` / `admin123`. Al desplegar, definir `IOTELITO_ADMIN_PASSWORD` para
reemplazarla.

## Pruebas

```bash
./mvnw test
```

Usan H2 en modo MySQL con los mismos `schema.sql` y `data.sql`, y comprueban: que las
entidades coinciden con el DDL, que un pedido lo gana un solo taxista, que un taxista no
tiene dos servicios activos, el login del panel con CSRF, el registro por tipo de documento
y la API completa (del pedido a la calificación, con cada rechazo por rol y estado) con un
Firestore en memoria y tokens simulados.

## API REST para la app

`/api/v1`, con las 17 operaciones del contrato `docs/api-taxis-v1.yaml` (rama `main`).

- La app envía `Authorization: Bearer <token de Firebase>`. La web verifica la firma con las
  claves públicas de Google, el emisor y la audiencia del proyecto (`FIREBASE_PROJECT_ID`,
  por defecto `iot-telito`). No guarda ninguna clave de Firebase.
- El rol sale de `users/{uid}` en Firestore. La web lo lee por REST con el mismo token del
  usuario, así que se aplican las reglas publicadas. Igual con la reserva y el hotel al pedir
  un taxi: el hotel, la dirección y la ciudad nunca se toman de lo que envía la app.
- Todo error responde `{"codigo": "...", "mensaje": "..."}` con los códigos del contrato.

### Cómo entra un taxista a la app

1. Se registra en la web y el superadmin lo aprueba.
2. Se le crea su cuenta de Firebase con el **mismo correo** y su `users/{uid}` con
   `role: TAXISTA` y `active: true` (hoy a mano, en la consola de Firebase).
3. La primera vez que abre la app, `GET /api/v1/taxistas/me` enlaza esa cuenta con su ficha
   por el correo y guarda el `firebase_uid`.

## Despliegue en AWS (Learner Lab)

Desplegado el 16/11/2026 en la misma EC2 y el mismo RDS de la web anterior, **sin tocarla**:

| Pieza | Valor |
| --- | --- |
| URL pública (HTTPS) | `https://bkmx72ue4k.execute-api.us-east-1.amazonaws.com` (API Gateway HTTP) |
| Servidor | EC2 `servidor-proyecto-web`, servicio `taxiweb` en el puerto 8081 |
| Base | RDS `iotelito-db`, base `iotelito_taxis`, usuario `taxiweb` (la base `iotelito` sigue intacta) |
| Contraseña del superadmin | Parameter Store `/iotelito/taxiweb/admin-password` (SecureString) |
| Artefactos | `s3://iotelito-taxiweb-102343456181/deploy/` (privado) |

La web anterior sigue en el puerto 8080 y se enciende a mano como antes. La nueva arranca sola
con el lab (`systemctl status taxiweb`). Si hace falta memoria: `sudo systemctl stop taxiweb`.

Para actualizar la versión nueva:

1. `./mvnw -DskipTests package` y subir `target/taxiweb-0.0.1-SNAPSHOT.jar` como
   `s3://iotelito-taxiweb-102343456181/deploy/taxiweb.jar`.
2. En el servidor (Session Manager): `sudo aws s3 cp s3://iotelito-taxiweb-102343456181/deploy/taxiweb.jar /opt/taxiweb/taxiweb.jar && sudo systemctl restart taxiweb`.

Instalación desde cero: `deploy/instalar-junto-a-web-existente.sh` (pide la contraseña de admin de
RDS, crea la base y el usuario, aplica `schema.sql` y `data.sql` e instala el servicio).
`deploy/instalar-servidor.sh` es la alternativa para una EC2 Ubuntu nueva con MySQL local.

## Pendiente

- Probar en real la creación automática de cuentas al aprobar (activa desde el 16/11),
  y deshabilitar la cuenta de Firebase al desactivar un taxista (hoy la API ya le niega el acceso).
- Guardar en S3 los documentos del registro (hoy el formulario los recibe pero no los guarda) y
  devolver las fotos en `fotoUrl` (hoy `null`).
- Probar la API con un token real de Firebase desde la app.
