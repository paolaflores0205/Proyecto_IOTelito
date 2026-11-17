# Sistema web de gestión de taxistas - IoTelito

Spring Boot 3.5 (Java 17 o superior) con MySQL 8. El taxista se autorregistra aquí y el
superadmin lo aprueba desde el panel. La app móvil usa Firestore para todo lo demás y
consultará lo del taxi por la API REST del contrato `docs/api-taxis-v1.yaml` (rama `main`).

## Base de datos

En MySQL Workbench, ejecutar en este orden:

1. `db/00-crear-base.sql`: crea la base `iotelito`.
2. `db/schema.sql` (DDL) borra y recrea las tablas de la web. Después aplica
   `db/01-chat-y-avisos-taxi.sql` para agregar las tablas de chat y avisos.
3. `db/data.sql` (DML) carga datos de prueba (superadmin, 10 taxistas, documentos, pedidos,
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
| `TAXI_DOCS_BUCKET` | Bucket privado para los adjuntos del registro | sin configurar |
| `TAXI_DOCS_REGION` | Región del bucket | `us-east-1` |

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

## Archivos del registro de taxistas

En **Registro de taxistas → Documentación**, adjunta las fotos del conductor y del
vehículo, el documento de identidad, la licencia, el SOAT y la tarjeta de propiedad.
Las fotos aceptan JPG y PNG hasta 2 MB. Los documentos también aceptan PDF hasta 5 MB.
El total de los seis archivos admite hasta 8 MB.

La web guarda los objetos en S3 bajo `taxistaDocs/` y la base guarda su clave y
metadatos. En **Superadmin → Taxistas → Detalle** o **Evaluar solicitud**, selecciona
el nombre de un archivo. Se abre usando un enlace de S3 que vence en cinco minutos.
La API devuelve únicamente las dos fotos en los campos `fotoUrl` que ya usaba.

Para activar la carga en EC2, configura `TAXI_DOCS_BUCKET` y `TAXI_DOCS_REGION` en
`/etc/taxiweb.env`. Usa un bucket privado y el rol de la instancia con
`s3:PutObject`, `s3:GetObject` y `s3:DeleteObject` sobre `taxistaDocs/*`. Este último
permiso permite deshacer las subidas de un registro que falle.

Para probar el recorrido con S3 real desde una base H2 de prueba, configura
`TAXI_DOCS_BUCKET` con un bucket privado de pruebas y ejecuta
`./mvnw "-Dtest=RegistroAdjuntosTest#registraLosSeisAdjuntosYElSuperadminPuedeAbrirlos" test`.
Ese comando utiliza las credenciales de AWS del entorno y deja seis archivos ficticios
en el bucket de pruebas.

## Chat y avisos de taxi

En una instalación existente, aplica solo `db/01-chat-y-avisos-taxi.sql` sobre
`iotelito_taxis` con una cuenta con permiso para crear tablas. Este script agrega tablas
y se puede volver a ejecutar. Hazlo antes de instalar el JAR nuevo.

El servidor envía los avisos con la cuenta de servicio indicada en
`IOTELITO_FIREBASE_CREDENCIALES`. La cuenta necesita el permiso
`cloudmessaging.messages.create` en el proyecto del que proviene el APK.
La cola reintenta los envíos no confirmados y descarta ofertas ya tomadas o vencidas.
Firebase Messaging HTTP v1 debe estar habilitado en el proyecto.

## API REST para la app

`/api/v1`, con las operaciones del contrato `docs/api-taxis-v1.yaml` en el proyecto Android.

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
- Probar la API con un token real de Firebase desde la app.
