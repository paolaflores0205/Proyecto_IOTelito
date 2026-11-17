# Administrador: reservas, mensajes y reportes

Integración en la rama de admin a partir del commit `8e90c52` de `ultima_03_11`, adaptada al contrato activo de Firestore del 16 de noviembre de 2026.

## Datos y alcance

| Pantalla | Lectura | Escritura |
|---|---|---|
| Reservas | `reservations`, filtrada por `hotelId` del administrador | El huésped solicita la salida y deja la reserva en `EN_CHECKOUT`. Admin registra el cierre y la deja en `FINALIZADA`. |
| Checkout | Reserva y comprobante, si existe | `hotelCheckouts/{idReserva}` y estado de la reserva en una transacción |
| Mensajes | `reservations/{idReserva}/messages`, ordenada por `createdAt` | Mensajes nuevos con UID, rol y fecha del servidor |
| Reportes | Reservas, comprobantes y habitaciones del hotel | Ninguna |

Las colecciones y subcolecciones nuevas aparecen automáticamente al guardar su primer documento. **No hay que crearlas manualmente en la consola ni ingresar campos a mano.** Se reutilizan las reservas existentes y la asignación de hotel en `users` y `hotels`.

El chat del cliente también usa esa subcolección. Se abre desde el detalle de la reserva o desde Mensajes. La bandeja muestra las reservas como hilos disponibles; no inventa mensajes previos, respuestas automáticas ni cantidades de mensajes sin leer. Para reservas nuevas se guardan `clientName` y `hotelName` como etiquetas. Las antiguas siguen funcionando con el código de reserva y el ID del hotel, sin dar al admin acceso a todos los perfiles.

El huésped puede solicitar checkout cuando llega la fecha de salida en Lima. El formulario existente envía su calificación y comentario. Para las reservas nuevas y las antiguas sin datos de pago, la app genera metadatos de tarjeta de demostración. El administrador actúa sobre la solicitud y registra un cierre administrativo simulado. Si agrega un cargo adicional debe ingresar el monto, motivo y observación. Una transacción guarda el comprobante inmutable y el estado final. Al repetir el cierre se conserva el mismo comprobante. Las reservas históricas `COMPLETADA` se pueden consultar.

Reportes permite día, mes y año, con navegación entre períodos. Importes = comprobantes emitidos en el período; reservas = llegadas; ocupación estimada = noches reservadas / (habitaciones actuales × días). Los cuatro tramos del gráfico dividen el período. El inventario actual puede diferir del histórico: la ocupación es una estimación. Los servicios del catálogo no son ventas y aún no existe un registro remoto de consumos: esa tarjeta muestra «—» y lo explica. No hay notificaciones push, adjuntos ni confirmaciones de lectura en el chat.

Las pantallas de ejemplo siguen disponibles únicamente para los fixtures locales de prueba. Las sesiones Firebase usan datos remotos. No se modificaron los flujos de taxi ni de gestión de usuarios/hoteles del superadmin. Del cliente se conectaron su chat y su bandeja y se añadieron las etiquetas opcionales a reservas nuevas.

## Reglas de Firestore

Las reglas de checkout y chat mantienen el contrato publicado en `iot-telito` consultado el 16 de noviembre. Limitan las operaciones al hotel asignado, exigen la solicitud del huésped antes del cierre y conservan los mensajes como historial después de finalizar una reserva. Las reglas publicadas también aceptan las referencias de fotos de Amazon S3.

## Prueba manual en el proyecto real

Usar cuentas y reservas identificadas como prueba. El cierre queda guardado y es definitivo para esa reserva de prueba.

1. **Asignación:** verificar que `users/{uidAdmin}` tenga `role: HOTEL_ADMIN`, `active: true`, `hotelId` entero; en `hotels/{hotelId}`, `administratorUid` debe coincidir y `active` ser verdadero.
2. **Reservas:** entrar con el admin, abrir Reservas, seleccionar el estado correspondiente. Probar búsqueda por código, nombre disponible o habitación y filtros de fecha. Solo deben aparecer reservas de su hotel. Entrar en detalle y volver; no debe aparecer información de ejemplo.
3. **Chat cliente → admin:** desde una reserva del hotel, el cliente envía un texto identificable. En admin → Mensajes, abrir el mismo código de reserva. Debe aparecer. Responder, volver al cliente y comprobar la respuesta. Salir de la pantalla y entrar otra vez: ambos mensajes deben persistir. En consola se ven dentro de `reservations → id → messages`.
4. **Checkout.** Con una reserva de prueba cuya fecha de salida haya llegado, el huésped envía la solicitud con estrellas y comentario. En admin, abrir Reservas → Checkout y escoger esa reserva. Revisar el total, probar un extra con motivo y observación y confirmar. Firestore debe guardar `FINALIZADA` y el comprobante `hotelCheckouts/{mismoId}`. Al volver a abrir la reserva se muestra el comprobante en lectura.
5. **Reportes:** seleccionar el día actual y verificar que el importe coincida con el comprobante anterior. Cambiar a mes/año y período anterior. Un período sin registros debe mostrar cero; un fallo de consulta muestra un aviso y «—», no datos de demostración.
6. **Regresión:** cliente puede iniciar sesión, ver hoteles y reservas; taxista conserva disponibilidad y sus servicios; superadmin conserva usuarios y hoteles. Revisar acceso con un segundo administrador asignado a otro hotel si se dispone de cuenta de prueba.

## Verificación automatizada

- Compilación debug, APK de instrumentación y 79 pruebas JVM aprobadas.
- Suites de reglas aprobadas para autenticación y usuarios, hoteles y asignaciones, inventario admin, reportes y logs, taxi y operaciones admin.
- Operaciones admin pasó 50 comprobaciones y un caso de cierre concurrente. Incluye la solicitud del huésped, la inmutabilidad del comprobante, el historial del chat y el aislamiento por hotel.
- `HotelOperationsEmulatorTest` pasó en Android 14 con servicios emulados. Verifica la solicitud de checkout desde el repositorio del cliente, el chat de ambos roles, las tres pestañas del admin, el cierre idempotente, el total del reporte y el rechazo de reservas ajenas.
- Para repetir la prueba Android, inicia Auth (9099) y Firestore (8180) con el proyecto vacío `demo-iotelito-auth`, usa `adb reverse` para esos puertos y ejecuta con `hotelEmulator=true` en un AVD desechable.

Comando de compilación y JVM:

```sh
bash gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon
```

Queda por ejecutar el recorrido manual con cuentas y reservas de prueba del equipo en Firebase real.

## Alertas y fotos del administrador

- La portada de admin muestra las solicitudes `EN_CHECKOUT` de su hotel y abre el checkout correspondiente. Android publica un aviso por solicitud y lo retira al sincronizar el cierre. El cliente ve el aviso con el importe simulado en su bandeja y puede abrir la reserva.
- Los avisos del sistema requieren el permiso de Android. Llegan al sincronizar la app con la sesión activa y se deduplican por cuenta y reserva en el dispositivo. La tarjeta de admin y la bandeja del cliente siguen disponibles con el permiso denegado.
- Desde Hotel → Datos del hotel → Fotos, admin puede agregar, reemplazar y quitar fotos, con un mínimo de cuatro y un máximo de ocho. Desde el formulario de servicios puede seleccionar o reemplazar una imagen. La app reduce las imágenes a JPEG, las sube al bucket privado de Amazon S3 y guarda las referencias en Firestore.
- Las fotos nuevas se guardan en `s3://iot-telito-imagenes/hotelPhotos/{hotelId}/{uuid}.jpg`. La API comprueba la asignación del administrador y emite enlaces temporales. El cliente puede leer las fotos del catálogo con una cuenta activa. Eliminar una foto de la galería retira su referencia del catálogo.

### Cómo probarlo

1. Con el cliente, envía una solicitud de salida con estrellas y comentario. Con admin, abre la tarjeta de alertas del panel, verifica el mismo código de reserva y registra el cierre.
2. Al volver al panel, la alerta desaparece. Con el huésped, abre Perfil → Notificaciones y comprueba el aviso con el importe. Abre la reserva desde el aviso y comprueba que quede marcado como leído.
3. Abre la galería del hotel y agrega o reemplaza una foto. Guarda y vuelve a entrar para comprobar que permanece. Haz lo mismo desde un servicio. La prueba de subida usa la API desplegada de Amazon S3 y una cuenta Firebase activa. [Configuración de la API](aws/imagenes/README.md)

Para repetir la prueba aislada, inicia Auth y Firestore con `firebase emulators:start --only auth,firestore --project demo-iotelito-auth --config firebase.local.json`. En el AVD desechable, asigna los puertos `9099` y `8180` con `adb reverse` y concede el permiso de notificaciones. Ejecuta `HotelOperationsEmulatorTest` con `hotelEmulator=true` y `hotelNotifications=true`, usando emuladores con datos vacíos.

La prueba aislada comprueba las alertas, el cierre sin duplicados, la bandeja y el guardado de referencias S3 de galería y servicio. También comprueba que Firestore rechaza referencias de otro hotel. La subida y descarga de imágenes se comprueban con el recorrido de la app en AWS real.
