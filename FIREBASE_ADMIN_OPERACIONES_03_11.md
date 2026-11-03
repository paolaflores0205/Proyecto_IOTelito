# Administrador: reservas, mensajes y reportes

Preparación local en la rama `superadmin`. No se publicaron reglas, no se escribieron datos en el proyecto Firebase real y no se hizo commit ni push.

## Datos y alcance

| Pantalla | Lectura | Escritura |
|---|---|---|
| Reservas | `reservations`, filtrada por `hotelId` del administrador | Al cerrar, cambia únicamente `estado` a `COMPLETADA` |
| Checkout | Reserva y comprobante, si existe | `hotelCheckouts/{idReserva}` y estado de la reserva en una transacción |
| Mensajes | `reservations/{idReserva}/messages`, ordenada por `createdAt` | Mensajes nuevos con UID, rol y fecha del servidor |
| Reportes | Reservas, comprobantes y habitaciones del hotel | Ninguna |

Las colecciones y subcolecciones nuevas aparecen automáticamente al guardar su primer documento. **No hay que crearlas manualmente en la consola ni ingresar campos a mano.** Se reutilizan las reservas existentes y la asignación de hotel en `users` y `hotels`.

El chat del cliente también usa esa subcolección. Se abre desde el detalle de la reserva o desde Mensajes. La bandeja muestra las reservas como hilos disponibles; no inventa mensajes previos, respuestas automáticas ni cantidades de mensajes sin leer. Para reservas nuevas se guardan `clientName` y `hotelName` como etiquetas. Las antiguas siguen funcionando con el código de reserva y el ID del hotel, sin dar al admin acceso a todos los perfiles.

El checkout registra un cierre administrativo, **no carga tarjetas ni implementa una pasarela de pago**. Solo permite cerrar una reserva confirmada cuya fecha de salida ya llegó en Lima. Un extra exige monto válido, motivo y observación. El comprobante es inmutable: no se sobrescribe ni se borra; repetir la operación no duplica el importe. Al volver a abrirlo, se muestra también el extra guardado. Si el cliente ya finalizó una reserva sin este comprobante, se muestra en lectura y no se le inventa un ingreso.

Reportes permite día, mes y año, con navegación entre períodos. Importes = comprobantes emitidos en el período; reservas = llegadas; ocupación estimada = noches reservadas / (habitaciones actuales × días). Los cuatro tramos del gráfico dividen el período. El inventario actual puede diferir del histórico: la ocupación es una estimación. Los servicios del catálogo no son ventas y aún no existe un registro remoto de consumos: esa tarjeta muestra «—» y lo explica. No hay notificaciones push, adjuntos ni confirmaciones de lectura en el chat.

Las pantallas de ejemplo siguen disponibles únicamente para los fixtures locales de prueba. Las sesiones Firebase usan datos remotos. No se modificaron los flujos de taxi ni de gestión de usuarios/hoteles del superadmin. Del cliente se conectaron su chat y su bandeja y se añadieron las etiquetas opcionales a reservas nuevas.

## Publicación de reglas (pendiente)

Archivo preparado: `firestore.rules`.

1. En Firebase, ir a **Firestore Database → Reglas**.
2. Comparar las reglas publicadas con el archivo local para conservar cambios recientes de los compañeros. Guardar una copia de la versión publicada antes de editar.
3. Integrar los cambios de operaciones del hotel: ampliar `get/list` de `reservations` al administrador asignado; añadir la actualización administrativa de estado, el bloque anidado `messages` y el bloque raíz `hotelCheckouts`. Mantener los permisos de los demás roles y el bloqueo general final.
4. No duplicar `match /reservations`: las reglas de mensajes van dentro del bloque existente. `hotelCheckouts` va al mismo nivel que `reservations`, dentro de `match /databases/{database}/documents`.
5. Si la consola coincide con la base del repositorio y no contiene aportes adicionales, puede copiarse el archivo local completo. Publicar solo después de esa comparación. Este cambio no requiere reglas nuevas de Storage ni índices compuestos.
6. Volver a ejecutar la app desde Android Studio. Si hay un error de clases duplicadas con sufijo «2», limpiar los archivos generados y recompilar; no borrar archivos fuente.

## Prueba manual en el proyecto real, después de publicar

Usar cuentas y reservas identificadas como prueba. El cierre queda guardado y es definitivo para esa reserva de prueba.

1. **Asignación:** verificar que `users/{uidAdmin}` tenga `role: HOTEL_ADMIN`, `active: true`, `hotelId` entero; en `hotels/{hotelId}`, `administratorUid` debe coincidir y `active` ser verdadero.
2. **Reservas:** entrar con el admin, abrir Reservas, seleccionar el estado correspondiente. Probar búsqueda por código, nombre disponible o habitación y filtros de fecha. Solo deben aparecer reservas de su hotel. Entrar en detalle y volver; no debe aparecer información de ejemplo.
3. **Chat cliente → admin:** desde una reserva del hotel, el cliente envía un texto identificable. En admin → Mensajes, abrir el mismo código de reserva. Debe aparecer. Responder, volver al cliente y comprobar la respuesta. Salir de la pantalla y entrar otra vez: ambos mensajes deben persistir. En consola se ven dentro de `reservations → id → messages`.
4. **Checkout:** con una reserva de prueba confirmada cuya salida sea hoy o anterior, abrir Checkout. Revisar el total; opcionalmente añadir un daño de prueba con motivo y observación. Confirmar una vez. En consola deben existir simultáneamente `estado: COMPLETADA` y `hotelCheckouts/{mismoId}` con base, extra, total, autor y fecha. Abrir nuevamente: queda en lectura, con el extra si lo hubo. Una reserva futura no debe ofrecer cierre.
5. **Reportes:** seleccionar el día actual y verificar que el importe coincida con el comprobante anterior. Cambiar a mes/año y período anterior. Un período sin registros debe mostrar cero; un fallo de consulta muestra un aviso y «—», no datos de demostración.
6. **Regresión:** cliente puede iniciar sesión, ver hoteles y reservas; taxista conserva disponibilidad y sus servicios; superadmin conserva usuarios y hoteles. Revisar acceso con un segundo administrador asignado a otro hotel si se dispone de cuenta de prueba.

## Verificación automatizada

- Compilación debug, APK de instrumentación y 82 pruebas JVM: correctas.
- Seis suites de reglas contra Firestore efímero: autenticación/usuarios, hoteles/asignaciones, inventario admin, reportes/logs superadmin, taxi y operaciones admin.
- Operaciones admin: 44 verificaciones de permisos y validaciones más prueba de cierre concurrente. Incluye chat de ambos participantes, lectura de comprobante ausente, comprobante inmutable, hotel ajeno, cuenta inactiva y reasignación.
- Prueba Android de integración: correcta en AVD separado con Auth y Firestore emulados. Verifica mensaje cliente → admin → cliente, navegación de las tres pestañas, aislamiento por hotel, cierre idempotente y reporte.
- Prueba Android opcional `HotelOperationsEmulatorTest`: usa únicamente `demo-iotelito-auth`, Auth 9099 y Firestore 8180 por `adb reverse`; requiere `hotelEmulator=true` en un AVD desechable. Sin ese argumento se omite. Nunca ejecutar esta prueba opcional en el emulador de trabajo con cuentas reales.

Comando de compilación y JVM:

```sh
bash gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon
```

La comprobación en el Firebase real permanece pendiente hasta publicar las reglas y efectuar la prueba manual con las cuentas del equipo.
