# Notificaciones del cliente: cuentas separadas y FCM

## Cambios de la app

- Room pasa de versión 2 a 3. La bandeja usa propietario UID + ID, con consultas y marcas de lectura por cuenta.
- Los avisos anteriores sin propietario se conservan en cuarentena con UID vacío; ninguna cuenta los hereda. Las reservas locales se conservan.
- Cada sesión cliente registra un dispositivo en `users/{uid}/notificationDevices/{binding}`. El binding cambia al cerrar sesión e iniciar otra. El token puede renovarse sin cambiar el binding.
- Al cerrar sesión se borra inmediatamente el vínculo local, se intenta eliminar el dispositivo remoto y se retiran los avisos Android del cliente. Sin red, el borrado remoto puede quedar pendiente; el teléfono igualmente rechaza esos mensajes.
- El receptor FCM acepta únicamente mensajes de datos con el UID autenticado y el binding vigente. No usa el bloque automático `notification`, que podría mostrar datos antes de comprobar la cuenta.
- Los enlaces incluyen el propietario. Al abrir un aviso con el proceso antes cerrado, se valida el perfil activo desde Firestore antes de abrir la reserva o el taxi.
- Los avisos de checkout remotos y locales se unifican en la bandeja y comparten su deduplicación. Lectura y deduplicación son por usuario y dispositivo, no sincronizadas entre teléfonos.

## Emisor AWS preparado

Paquete: `aws/notificaciones/iot-telito-notificaciones.zip`.
Función nueva independiente: `iot-telito-notificaciones`; no sustituir la Lambda de imágenes.

Lambda consulta Firestore y envía mensajes con Firebase Admin SDK a los dispositivos propios del cliente activo. Incluye reserva confirmada, checkout aprobado (total del comprobante) y estado de taxi. Los mensajes de chat no forman parte de este emisor.

EventBridge la invoca cada minuto. Escanea paginadamente `hotelCheckouts`, `taxiRequests` y `reservations`; el estado del envío por evento y sesión queda en `pushDeliveries`. Las transacciones evitan dos envíos simultáneos, liberan los fallos transitorios para reintentar y eliminan tokens revocados. Si el proceso cae después de que FCM acepte y antes de guardar el resultado, puede repetir; Android deduplica también por cuenta y aviso.

Es una implementación para la escala pequeña de la demostración, no un sistema inmediato ni un registro de todas las transiciones intermedias. Si el taxi cambia dos veces entre consultas, se notifica el estado más reciente. Existe un límite de 2000 documentos por ejecución; al superarlo se reporta error en vez de afirmar que todo se procesó.

Cada ejecución consume lecturas de Firestore, además de recursos AWS. Con 50 documentos y una consulta por minuto, solo el escaneo puede superar 72000 lecturas al día, sin contar perfiles, dispositivos y deduplicación. No dejar habilitada la programación permanentemente para las pruebas: activarla durante la demostración y deshabilitarla al terminar. FCM es gratuito; AWS usa las condiciones/créditos de la cuenta Academy y las lecturas Firestore tienen su propia cuota. Para uso continuo hay que sustituir este escaneo por un flujo de eventos y comprobar presupuesto/permisos.

## Activación manual, sin cambios automáticos en la nube

### 1. Reglas

1. Firebase > Firestore Database > Reglas: copiar las reglas publicadas y guardar `firestore-publicadas-antes-notificaciones.txt` en Entregable4.
2. Comparar ese respaldo con `firestore.rules` antes de reemplazar: conservar cambios recientes de otros integrantes. Se añade el bloque `users/{uid}/notificationDevices/{binding}` y se reserva `pushDeliveries` al servidor.
3. Publicar solamente la versión combinada revisada. No abrir permisos públicos para resolver un rechazo.
4. Ejecutar la app actualizada, entrar como cliente y permitir notificaciones. En Firestore debe aparecer la subcolección notificationDevices del usuario. No compartir su token ni binding.

### 2. Cuenta de servicio Google

1. Desde Firebase, Configuración del proyecto > Cuentas de servicio > Administrar permisos de las cuentas de servicio, abrir Google Cloud del mismo proyecto.
2. IAM y administración > Cuentas de servicio > Crear cuenta de servicio. Nombre: `iot-telito-push`.
3. Asignar roles `Cloud Datastore User` y `Firebase Cloud Messaging API Admin` en el proyecto. Si la cuenta educativa no permite crear cuentas/claves o asignar roles, solicitar esos permisos al responsable; no requiere rehacer Firebase.
4. En esa cuenta, Claves > Agregar clave > Crear clave nueva > JSON. Descargarla fuera del repositorio. Es una credencial privada del servidor; no pegarla en el chat ni incluirla en Android o Git.
5. En APIs y servicios > APIs habilitadas, comprobar `Firebase Cloud Messaging API`. Habilitarla si no está activa.

### 3. Secreto AWS

1. En AWS Academy, misma cuenta y región us-east-1, buscar Secrets Manager. Su disponibilidad y permisos deben comprobarse en la consola del laboratorio.
2. Almacenar nuevo secreto > Otro tipo de secreto > Texto sin formato. Pegar el contenido completo del JSON descargado. Usar la clave administrada predeterminada.
3. Nombre: `iot-telito/firebase-push`. Sin rotación automática para esta prueba. Guardar y copiar su ARN (no su contenido).
4. La función necesita `secretsmanager:GetSecretValue` únicamente para ese secreto. Si LabRole no lo permite y el laboratorio impide añadir permisos, detener esta activación y coordinar el permiso. No guardar la clave en un bucket público ni en el código para evitarlo.

### 4. Lambda

1. Lambda > Crear función > Crear desde cero. Nombre `iot-telito-notificaciones`, Node.js 24.x o 26.x, arquitectura x86_64. Usar el rol existente autorizado del laboratorio (habitualmente LabRole), con lectura del secreto y logs.
2. Código > Cargar desde > Archivo .zip: seleccionar el paquete preparado. Sus archivos index.mjs y node_modules deben quedar en la raíz.
3. Configuración de tiempo de ejecución: controlador `index.handler`.
4. Configuración > Configuración general: memoria 256 MB, timeout 120 segundos. No configurar una VPC para esta función: necesita salida a Google.
5. Configuración > Variables de entorno:
   - `FIREBASE_PROJECT_ID`: `iot-telito` (comprobar en el proyecto y en el JSON).
   - `FIREBASE_SECRET_ARN`: ARN del secreto creado.
   - `PUSH_START_AT`: fecha/hora ISO UTC de activación. En PowerShell se obtiene con `(Get-Date).ToUniversalTime().ToString("o")`; pegar el resultado, no el comando.
   - `MAX_DOCUMENTS`: `2000`.
6. No crear Function URL, API pública ni integración con API Gateway. La invocación es interna.
7. Prueba manual con evento `{}`. El resultado esperado es `ok: true`, con contadores scanned/sent. No imprime claves, tokens ni datos de usuarios. Los errores de permisos de Secrets Manager/Google deben resolverse antes de continuar.

### 5. Programación

1. EventBridge > Reglas > Crear regla programada. Usar el flujo de regla programada clásico si el laboratorio ofrece esa opción; la consola puede recomendar Scheduler.
2. Nombre `iot-telito-notificaciones-cada-minuto`, expresión `rate(1 minute)`, destino función Lambda `iot-telito-notificaciones`, entrada constante `{}`.
3. Autorizar la invocación desde EventBridge a esa función. Si se usa Scheduler, su rol debe permitir `lambda:InvokeFunction`; no crear roles nuevos si el laboratorio no lo permite.
4. Habilitar para las pruebas. Comprobar CloudWatch: cada ejecución registra solo scanned y sent. Deshabilitar al terminar la sesión de demostración para limitar consumo.

## Pruebas con cuentas reales, después de activar

- Abrir cliente, dar permiso de avisos y cerrar normalmente la app desde recientes; NO usar Forzar detención en Ajustes.
- En otro teléfono, aprobar un checkout nuevo con adicional. En alrededor de un ciclo de consulta más la entrega FCM, debe aparecer el aviso con el total. Tocar abre la reserva propia y su comprobante.
- Aceptar un taxi desde otro teléfono y comprobar el aviso con acceso al seguimiento/chat habilitado. Repetir avance y finalización dejando un ciclo entre cambios.
- Volver a la bandeja: el checkout aparece una sola vez. Marcar leído y regresar: conserva lectura.
- En el mismo teléfono, cerrar sesión de A e iniciar B. No deben aparecer avisos ni reservas de A. Un mensaje retrasado de la sesión A se descarta.
- Iniciar A otra vez: recupera su bandeja propia. No hereda los avisos históricos sin propietario.
- Denegar notificaciones: los avisos recibidos se conservan en la bandeja, aunque no aparezcan en Android. El permiso no se concede automáticamente.

Android puede retrasar mensajes por ahorro de batería o conectividad. Si se fuerza la detención desde Ajustes, hay que reabrir la aplicación. FCM no garantiza entrega instantánea. La app necesita Google Play services para esta integración.

## Validación local y fuentes

La prueba Android ClientNotificationIsolationTest utiliza una base temporal propia: migra versión 2, conserva reserva y aviso antiguo, separa dos cuentas con el mismo ID de aviso y verifica lectura independiente. No toca la base de usuario ni consulta Firebase real.

Pruebas del emisor usan mensajes FCM simulados y Firestore local: evento/importe/destinatario, concurrencia, reintentos y token revocado. Las pruebas de reglas cubren dueño, cuenta ajena, roles, campos, fecha del servidor y estado privado del emisor. La prueba real con app cerrada queda pendiente de esta activación manual.

- https://firebase.google.com/docs/cloud-messaging/android/receive-messages
- https://firebase.google.com/docs/cloud-messaging/android/get-started
- https://firebase.google.com/docs/cloud-messaging/send/v1-api
- https://firebase.google.com/docs/admin/setup
- https://firebase.google.com/pricing
- https://docs.aws.amazon.com/lambda/latest/dg/with-secrets-manager.html
- https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-scheduled-rule-pattern.html
