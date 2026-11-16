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

El archivo local refleja la versión publicada en `iot-telito` consultada el 16 de noviembre. Las reglas limitan las operaciones al hotel asignado, exigen la solicitud del huésped antes del cierre y conservan los mensajes como historial después de finalizar una reserva. Se verificó esa versión mediante lectura de la publicación y pruebas en Firestore Emulator.

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
