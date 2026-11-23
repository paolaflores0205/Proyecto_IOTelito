# Manual de instalación — IoTelito

Cómo instalar el sistema completo desde cero: Firebase, los servicios en AWS, la web de
gestión de taxistas y la app Android. Arquitectura: [01-ARQUITECTURA.md](01-ARQUITECTURA.md).

> **Nunca subas al repositorio** contraseñas, la clave de la cuenta de servicio de Firebase ni
> credenciales de AWS. Se guardan en Parameter Store o Secrets Manager.

## 0. Requisitos

| Herramienta | Para qué |
|---|---|
| Cuenta de Firebase (Google) | Autenticación, Firestore y notificaciones. |
| Cuenta de AWS (o AWS Academy Learner Lab), región **us-east-1** | EC2, RDS, S3, API Gateway, Lambda. |
| Android Studio (con su JDK incluido) | Compilar e instalar la app. |
| Java 17 o superior | Compilar la web (el JDK de Android Studio sirve). |
| Node.js 20 o superior y npm | Empaquetar las Lambdas y usar la CLI de Firebase (`npx firebase-tools`). |
| AWS CLI v2 | Subir archivos y administrar recursos desde la terminal. |
| Git | Clonar `https://github.com/paolaflores0205/Proyecto_IOTelito`. |

Ramas: la app está en `feat/hotel-admin` (y `main`); la web de taxistas en `feat/taxi-api-web`.

---

## 1. Firebase

1. Crear el proyecto (en este despliegue: `iot-telito`).
2. **Authentication** → *Método de acceso* → habilitar **Correo electrónico/contraseña**.
3. **Firestore Database** → crear la base en modo producción.
4. **Configuración del proyecto** → *Tus apps* → agregar app Android con el paquete
   `com.example.proyecto_iotelito` y descargar `google-services.json` en `app/`.
5. **Publicar las reglas de seguridad** desde la raíz del repositorio:
   ```bash
   npx firebase-tools deploy --only firestore:rules --project iot-telito --config firebase.local.json
   ```
   Antes de publicar, comparar con las reglas que ya están en la consola: si un compañero publicó
   cambios que no están en el repositorio, se perderían.
6. **Superadmin inicial:** en *Authentication* crear un usuario y, en *Firestore*, el documento
   `users/{uid}` con `uid`, `name`, `email`, `role: "SUPERADMIN"` y `active: true`.
   Los demás usuarios se crean desde la app (clientes y hoteles) o desde la web (taxistas).
7. **Cuenta de servicio:** *Configuración* → *Cuentas de servicio* → *Generar nueva clave privada*.
   Se usa en la web de taxistas (paso 4) y en el emisor de avisos (paso 3). Guardarla fuera del repositorio.

## 2. AWS — API de fotos (carpeta `aws/imagenes`)

Detalle paso a paso: [`aws/imagenes/README.md`](../../aws/imagenes/README.md).

1. **S3:** bucket privado `iot-telito-imagenes` (prefijos `profilePhotos/` y `hotelPhotos/`).
2. **Lambda** `iot-telito-imagenes-api`: Node.js, handler `index.handler`, 256 MB, 15 s, rol con
   acceso al bucket (en el laboratorio, `LabRole`). Variables: `BUCKET_NAME`, `FIREBASE_PROJECT_ID`.
   Empaquetar en Windows:
   ```bash
   npm ci --ignore-scripts
   ```
   ```bash
   tar.exe -a -c -f iot-telito-imagenes-api.zip index.mjs package.json package-lock.json node_modules
   ```
3. **API Gateway (HTTP API)** con integración a la Lambda y rutas `GET /health`,
   `POST /profile-photos/upload-url`, `POST /profile-photos/read-url`,
   `POST /hotel-photos/upload-url` y `POST /hotel-photos/read-url`.
4. Si la URL de la API cambia, actualizarla en
   `app/src/main/java/com/example/proyecto_iotelito/data/ProfilePhotoPolicy.java` (constante `API`).
5. Prueba: `POST /profile-photos/upload-url` sin token debe responder **401**.

## 3. AWS — emisor de notificaciones (carpeta `aws/notificaciones`)

1. **Secrets Manager:** secreto con el JSON de la cuenta de servicio de Firebase
   (en este despliegue: `iot-telito/firebase-push`).
2. **Lambda** `iot-telito-notificaciones`: Node.js, 256 MB, 120 s. Variables: `FIREBASE_PROJECT_ID`,
   `FIREBASE_SECRET_ARN` (ARN del secreto), `MAX_DOCUMENTS` y `PUSH_START_AT`. Permiso para leer
   el secreto: `aws/notificaciones/secrets-manager-policy.json`. Se empaqueta igual que la de fotos.
3. **EventBridge:** regla programada `rate(1 minute)` con destino la Lambda.
4. Prueba: al confirmar un cobro de checkout, al cliente le llega la notificación en menos de un minuto.

## 4. AWS — web de gestión de taxistas (rama `feat/taxi-api-web`, carpeta `web-taxis`)

### 4.1 Recursos

| Recurso | Configuración usada |
|---|---|
| RDS | MySQL 8.4, `db.t3.micro`, 20 GB, sin acceso público. Grupo de seguridad: puerto 3306 solo desde la EC2. |
| EC2 | Amazon Linux 2023, `t3.micro`, con perfil de instancia que permita S3 y Systems Manager (en el laboratorio, `LabInstanceProfile`). Elastic IP. Grupo de seguridad: puerto 8081 abierto. |
| S3 | Bucket privado para `deploy/` (el .jar y los scripts SQL) y `taxistaDocs/` (documentos). |
| Parameter Store (SecureString) | `/iotelito/taxiweb/admin-password` (contraseña del superadmin web, 12 caracteres o más) y `/iotelito/taxiweb/firebase-service-account` (JSON de la cuenta de servicio). |

### 4.2 Compilar y subir

```bash
cd web-taxis
./mvnw -DskipTests package
aws s3 cp target/taxiweb-0.0.1-SNAPSHOT.jar s3://<bucket>/deploy/taxiweb.jar
aws s3 cp db/ s3://<bucket>/deploy/db/ --recursive
```

### 4.3 Instalar en la EC2

Conectarse por **Session Manager** (EC2 → instancia → *Conectar*) y ejecutar
`web-taxis/deploy/instalar-junto-a-web-existente.sh` (ajustar `BUCKET` y `RDS` al inicio). El script:

1. Descarga el .jar y los scripts SQL desde S3.
2. Pide la contraseña de administrador de RDS (no queda guardada).
3. Crea la base `iotelito_taxis` y el usuario `taxiweb` con una contraseña aleatoria y SSL obligatorio.
4. Aplica `schema.sql`, `01-chat-y-avisos-taxi.sql` y `data.sql`.
5. Escribe `/etc/taxiweb.env` (solo root) e instala el servicio `systemd` **`taxiweb`** en el puerto 8081,
   que arranca solo al encender la EC2.

Para crear automáticamente la cuenta de la app al aprobar un taxista y guardar sus documentos,
agregar en `/etc/taxiweb.env`:

```bash
IOTELITO_FIREBASE_CREDENCIALES=/etc/taxiweb-firebase.json
IOTELITO_FIREBASE_WEB_API_KEY=<clave web de Firebase (está en google-services.json)>
TAXI_DOCS_BUCKET=<bucket>
```

y crear `/etc/taxiweb-firebase.json` (permisos 600) con el valor del parámetro
`/iotelito/taxiweb/firebase-service-account`. Luego `sudo systemctl restart taxiweb`.

Alternativa sin RDS: `deploy/instalar-servidor.sh` instala MySQL local en una EC2 Ubuntu.

### 4.4 HTTPS con API Gateway

La app Android necesita HTTPS. Crear una **API Gateway HTTP** con integración *HTTP proxy* hacia
`http://<IP elástica>:8081/{proxy}` y las rutas `ANY /{proxy+}` y `ANY /`.
La web ya está configurada con `SERVER_FORWARD_HEADERS_STRATEGY=framework` para generar bien los enlaces.

Pruebas:

```bash
curl https://<api-id>.execute-api.us-east-1.amazonaws.com/api/v1/health
```

Debe responder 200. `GET /api/v1/taxistas/me` sin token debe responder 401.

### 4.5 Actualizar una versión

```bash
aws s3 cp target/taxiweb-0.0.1-SNAPSHOT.jar s3://<bucket>/deploy/taxiweb.jar
```

En la EC2:

```bash
sudo aws s3 cp s3://<bucket>/deploy/taxiweb.jar /opt/taxiweb/taxiweb.jar && sudo systemctl restart taxiweb
```

Consultar la base desde la EC2: comando `taxidb` (`taxidb ayuda` muestra los atajos).

## 5. App Android

1. Abrir la raíz del repositorio en Android Studio (rama `feat/hotel-admin`).
2. Revisar `local.properties` (no se sube al repositorio):
   ```properties
   sdk.dir=<ruta del SDK de Android>
   taxiApi.url=https://<api-id>.execute-api.us-east-1.amazonaws.com/api/v1
   taxiApi.fake=false
   ```
   `taxiApi.fake=true` usa una API simulada en memoria, útil sin la web encendida.
3. Conectar un celular o emulador con **Android 14 o superior** y pulsar **Run ▶**, o generar el APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Al abrir la app, conceder los permisos de **notificaciones** y **ubicación** (taxista). La lectura
   del QR usa el escáner de Google Play services, que no pide permiso de cámara a la app.

### Pruebas automáticas

| Qué | Comando |
|---|---|
| Pruebas unitarias de la app | `./gradlew testDebugUnitTest` |
| Reglas de Firestore (emulador local) | `npx firebase-tools emulators:exec --only firestore --project demo-iotelito-auth --config firebase.local.json "node app/src/test/js/firestore_auth_rules.test.cjs"` (y el resto de `app/src/test/js/*.test.cjs`) |
| Web de taxistas | `./mvnw test` en `web-taxis` |

## 6. Encender el sistema para una demostración (Learner Lab)

1. Iniciar los dos laboratorios de AWS Academy. La EC2 y el RDS arrancan solos y el servicio
   `taxiweb` se inicia con la EC2.
2. Verificar `https://<api-id>.execute-api.us-east-1.amazonaws.com/api/v1/health` (debe dar 200).
3. Las credenciales del laboratorio duran unas 4 horas: si se necesita usar la AWS CLI, descargarlas de nuevo.
4. Seguir el checklist [`docs/CHECKLIST_PRUEBAS_FINALES.md`](../CHECKLIST_PRUEBAS_FINALES.md).
