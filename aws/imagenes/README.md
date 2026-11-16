# Fotos del cliente con AWS

Bucket configurado manualmente por el usuario: `iot-telito-imagenes`, región `us-east-1`, privado.
Función: `iot-telito-imagenes-api`, Node.js, rol existente `LabRole`.
Variables: `BUCKET_NAME=iot-telito-imagenes`, `FIREBASE_PROJECT_ID=iot-telito`.

## Estado y alcance

Esta carpeta prepara la API de fotos de perfil. No despliega recursos ni modifica AWS/Firebase.
La app Android ya está adaptada localmente a esta API; requiere publicar manualmente el ajuste
revisado de Firestore para aceptar referencias S3 propias. Ver PERFIL_CLIENTE_AWS.md en la raíz
del proyecto. No anunciar que las fotos funcionan antes de la prueba real desde la app.

El mismo bucket puede alojar hotelPhotos más adelante, pero esta API solo permite profilePhotos
de clientes activos. Las rutas de hoteles no están implementadas y no se conceden permisos a
administradores de hotel mediante estas rutas.

## Despliegue manual

1. En Lambda > función > Código > Cargar desde > Archivo .zip, subir `iot-telito-imagenes-api.zip`.
   Confirmar el guardado/carga. El ZIP contiene index.mjs, package.json, package-lock.json y node_modules
   en su raíz, sin una carpeta contenedora. No contiene credenciales.
2. Comprobar que el controlador sea `index.handler`.
3. Configuración > Configuración general > Editar: timeout 15 segundos y memoria 256 MB.
   No habilitar VPC, URL de función ni opciones de capacidad especiales.
4. Crear un evento de prueba con el JSON siguiente y ejecutarlo:

```json
{"rawPath":"/health","requestContext":{"http":{"method":"GET"}},"headers":{}}
```

Esperado: statusCode 200 y body `{"ok":true}`. Solo comprueba configuración e importación;
no acredita permisos S3 ni acceso a Firebase.

5. Probar el evento siguiente, sin añadir credenciales:

```json
{"rawPath":"/profile-photos/upload-url","requestContext":{"http":{"method":"POST"}},"headers":{},"body":"{\"contentType\":\"image/jpeg\",\"sizeBytes\":100}"}
```

Esperado: statusCode 401. Es el rechazo correcto por falta de sesión, no un fallo de despliegue.

6. Después crear HTTP API en API Gateway, integración con esta Lambda, etapa `$default`:
   GET /health, POST /profile-photos/upload-url, POST /profile-photos/read-url.
   La Lambda verifica el Bearer token en cada operación protegida. No configurar un autorizador
   IAM que exija claves AWS en el APK. Mantener bucket privado, sin políticas públicas.
7. Probar con la app ya adaptada y un cliente real: autorización, subida POST, guardado del perfil,
   lectura, otro cliente rechazado, cliente inactivo rechazado. No pegar tokens en chats ni eventos
   guardados. Si LabRole no permite S3, consultar el error exacto antes de cambiar permisos.

## Contrato

- `GET /health`: 200 sin sesión, sin consultas remotas.
- `POST /profile-photos/upload-url`, Authorization Bearer Firebase ID token,
  JSON `{ "contentType": "image/jpeg", "sizeBytes": 12345 }`.
  Devuelve reference `s3://bucket/profilePhotos/uid/uuid.jpg`, url, method POST, fields y expiresIn.
  Subir multipart/form-data a url, agregar TODOS los fields recibidos, luego campo file con JPEG.
  El Content-Type del objeto va en los fields, no reemplazar el Content-Type multipart de la petición.
  S3 aplica el máximo de menos de 2 MiB mediante la política firmada. No usar PUT con esta respuesta.
- Tras éxito en S3, guardar reference estable en users.photoUrl (requiere adaptar reglas).
- `POST /profile-photos/read-url`, mismo Bearer, JSON `{ "reference": "s3://..." }`.
  Solo acepta ruta propia con UUID; comprueba existencia, tamaño y Content-Type del objeto.
  Devuelve URL GET de 120 segundos. No persistir URL firmada en Firestore ni registros.
- No hay borrado ni sobrescritura: una carga fallida al guardar perfil puede dejar un objeto privado
  sin referencia. Definir limpieza posterior. Reemplazar foto no elimina el archivo anterior.

## Seguridad y límites

JOSE verifica firma RS256 con claves públicas de Google, emisor, audiencia, expiración, iat,
auth_time y UID. Firestore REST consulta el perfil con el mismo token: se aplican las reglas
publicadas, y adicionalmente se exige uid coincidente, active true y role CLIENT.
No requiere clave privada Firebase ni claves AWS: el SDK usa el rol de ejecución Lambda.
No realiza comprobación adicional de revocación Auth; el perfil active se consulta en cada solicitud.
Los enlaces firmados pueden reutilizarse/compartirse mientras no caduquen; duran 120 segundos
y también dependen de la vigencia de las credenciales temporales del laboratorio.
El formato se declara JPEG y limita tamaño; no se inspecciona el contenido binario en servidor.
La app convierte la imagen antes de subir; si se requiere análisis completo, añadir validación S3
posterior antes de publicar las imágenes. Las fotos permanecen privadas.
No registrar cuerpos, tokens ni URLs firmadas. Los errores internos devuelven mensajes genéricos.

## Construcción local

`npm ci --ignore-scripts` y `npm test`. Incluir dependencias instaladas al comprimir.
Las pruebas locales usan dobles de Google/Firestore/S3; no certifican permisos remotos ni consumen créditos.
La consola y los recursos siguen bajo control del usuario.
