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
tiene dos servicios activos, el login del panel con CSRF y el registro por tipo de documento.

## Pendiente

- API REST `/api/v1` con el token de Firebase (contrato en `docs/api-taxis-v1.yaml`).
- Guardar en S3 los documentos del registro (hoy el formulario los recibe pero no los guarda).
- Crear la cuenta de Firebase del taxista al aprobarlo.
- Despliegue en AWS.
