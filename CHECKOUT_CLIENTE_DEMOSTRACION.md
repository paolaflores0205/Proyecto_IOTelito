# Checkout compartido y cobro de demostración

Implementación en `feature/inicio-cliente-reserva`. La publicación de reglas y cualquier corrección de reservas reales se realiza manualmente por el equipo.

## Flujo implementado

1. El cliente registra una tarjeta de prueba al confirmar su reserva.
2. Desde el día de salida puede solicitar checkout con 1 a 5 estrellas y observaciones obligatorias de hasta 4000 caracteres. También se permite una solicitud atrasada.
3. La transacción solo guarda `EN_CHECKOUT`, `rating`, `review` y `checkoutRequestedAt`. Lee la tarjeta ya guardada en la reserva y no permite añadirla ni cambiarla durante el checkout.
4. El administrador ve la solicitud en su panel y en el filtro Checkout de Reservas. Puede añadir un cargo de demostración con monto, motivo y observación.
5. Al confirmar, una transacción crea `hotelCheckouts/{reservationId}` y cambia la reserva a `FINALIZADA`. Reintentar no genera otro comprobante ni altera el primero.
6. El cliente consulta el estado pendiente o el comprobante desde el detalle de su reserva. Solo un comprobante confirmado en servidor acredita el cobro simulado. Se muestran alojamiento, adicional, motivo, observación, total, tarjeta enmascarada, fecha e identidad del administrador.
7. El taxi tiene una acción independiente y no redirige automáticamente fuera del detalle de reserva.

## Datos guardados

En `reservations/{id}` se añaden `cardType`, `cardLast4`, `cardToken` con prefijo `DEMO_` y `paymentMode` igual a `SIMULATED`. No se conserva el número completo, el CVV ni el vencimiento. Los campos sensibles del formulario no participan del guardado automático de vistas Android.

`hotelCheckouts/{id}` conserva el desglose y la tarjeta enmascarada, sin token ni datos completos de la tarjeta. Es un comprobante interno de demostración, no tributario; no se cobra dinero ni se envía factura por correo.

Las reservas históricas `COMPLETADA` siguen siendo legibles; no se inventa un comprobante ni se reabren automáticamente. Las reservas sin tarjeta válida guardada muestran un aviso para solicitar revisión al hotel y bloquean la solicitud de checkout. No piden registrar otra tarjeta ni generan un token de reemplazo.

La opinión se conserva en la reserva. Esta entrega no publica un catálogo público de reseñas ni recalcula todavía el promedio del hotel.

## Activación manual en Firebase (la realiza el usuario)

1. Abre la consola del proyecto Firebase del equipo y entra a Firestore Database > Reglas.
2. Guarda una copia de las reglas actualmente publicadas. Compáralas con el archivo `firestore.rules` de esta carpeta, especialmente los bloques de taxi, usuarios y hoteles. Si otro integrante añadió reglas nuevas, integra esas diferencias antes de reemplazar el archivo. El archivo local contiene las reglas combinadas del proyecto.
3. Coordina que cliente y administrador usen el APK de esta versión. La versión antigua del cliente intentaba finalizar directamente y será rechazada por las nuevas reglas; el administrador antiguo tampoco entiende el nuevo estado pendiente.
4. Copia el contenido completo del archivo combinado revisado al editor de reglas y pulsa Publicar. No requiere cambiar Authentication, activar facturación ni configurar Storage para este bloque.
5. Ejecuta la versión actualizada desde Android Studio en ambos roles. No hace falta crear a mano colecciones de comprobantes: aparecen al confirmar el cobro desde la app.

No se debe publicar una versión que elimine los bloques de taxi del equipo. No se ha verificado ni sustituido el contenido actualmente publicado en la nube desde esta tarea.

## Recorrido de comprobación con el equipo

Temporalmente se permite crear reservas con ingreso y salida anteriores a hoy para probar checkout.
El calendario de búsqueda, el selector del hotel y el guardado usan BookingDatePolicy.
Para restaurar la restricción, cambiar PERMITIR_FECHAS_PASADAS_PARA_PRUEBAS a false en
app/src/main/java/com/example/proyecto_iotelito/data/BookingDatePolicy.java y volver a compilar.
La salida debe seguir siendo posterior al ingreso. Se conservan tarjeta obligatoria, disponibilidad,
bloqueo de noches por cliente/habitación y límite del tamaño de la transacción. No se reabren
reservas finalizadas ni se modifican sus fechas en Firebase para efectuar esta prueba.
Para ver el beneficio de taxi, el alojamiento debe sumar al menos S/ 1000 (incluido exactamente
S/ 1000), independientemente del número de noches. Los adicionales del cierre no cambian la
base de elegibilidad. Se puede solicitar desde el checkout pendiente o finalizado. TaxiBenefitPolicy
concentra esta condición y los textos muestran el mismo mínimo. El umbral actual es fijo en la
app; no hay todavía un mínimo configurable por hotel.

- Reserva nueva: usa el número de prueba `4242 4242 4242 4242`, un vencimiento futuro y CVV `123`. Verifica que Firestore solo conserve los campos enmascarados descritos arriba.
- Para probar checkout, utiliza una reserva propia que ya tenga hoy o una fecha anterior como día de salida. Si no existe, coordina una reserva de prueba con el equipo; no cambies fechas de reservas comerciales solo para la demostración.
- Cliente: selecciona estrellas y observaciones y solicita checkout. Debe quedar pendiente, sin anuncio de pago realizado.
- Administrador: abre la solicitud del hotel asignado, añade si corresponde un cargo de prueba justificado y confirma. Debe aparecer un único documento `hotelCheckouts` con el mismo ID de la reserva.
- Cliente: vuelve a abrir el estado de checkout; debe mostrar el total con los adicionales, el motivo, la fecha y la tarjeta correcta. Si esa pantalla permanece abierta con conexión, observa el cambio remoto.
- Repite el acceso del administrador: el comprobante existente es de solo lectura y no se cobra de nuevo.
- Comprueba que la reserva y el comprobante no se puedan consultar desde otra cuenta cliente.

## Notificaciones y alcance

Mis reservas abre en Todas y conserva los filtros Activas y Finalizadas. Al cerrar checkout,
la reserva permanece accesible en Todas y Finalizadas. El listado y el primer detalle consultan
el mismo comprobante privado de hotelCheckouts: muestran total con adicionales y dos decimales.
El detalle desglosa alojamiento, adicional y motivo; precioTotal sigue representando solo el
alojamiento. Si falta el comprobante, se muestra Total por consultar sin presentar la base como
cobro confirmado. El comprobante permite reintentar y las pantallas observan los cambios remotos.

Después de solicitar taxi, una cuenta TAXISTA activa y disponible debe aceptar el servicio.
El conductor avanza SOLICITADO -> ASIGNADO -> EN_CAMINO -> EN_TRASLADO -> FINALIZADO.
No se asigna automáticamente por crear la solicitud. El cierre actual permite el código de
demostración TAXI-{id}; el lector QR y la integración externa de rutas siguen siendo trabajo del
módulo de taxi.

El administrador tiene un indicador en Reservas y una alerta en el panel. Los avisos Android requieren permiso de notificaciones.
El cliente recibe el aviso al observar los comprobantes con la aplicación abierta, al volver al panel o al consultar el comprobante. El importe proviene del comprobante del servidor. Los avisos nuevos de checkout comparten deduplicación y lectura por UID y se cancelan al cerrar sesión. La entrega push con la aplicación cerrada y el aislamiento completo de la bandeja antigua por cuenta pertenecen al bloque de notificaciones posterior.

## Verificación local

### Seguimiento y chat del taxi

Reservas incluye un acceso permanente a «Mis solicitudes de taxi»: muestra solicitudes
pendientes, viajes activos y finalizados, independientemente del filtro de reservas.
El detalle de una reserva mantiene el botón de seguimiento cuando existe su solicitud.
Al asignarse un conductor se habilita «Chatear con el conductor»; no abre una ventana
automática. Cliente y conductor intercambian mensajes reales en
`taxiRequests/{requestId}/messages`. El historial queda de solo lectura al finalizar.
El chat remoto sustituye las respuestas automáticas del flujo de demostración local.

Publicar las reglas anidadas de messages de firestore.rules antes de probar el chat.
Solo el cliente del servicio y su conductor activo asignado pueden leerlo o escribir;
no se permite editar o borrar mensajes ni enviar antes de asignación o después del cierre.
La alerta Android dice «Checkout aprobado por el administrador» y se observa desde
cualquier pantalla visible del cliente. Requiere permiso de notificaciones; la entrega
con el proceso cerrado todavía necesita una implementación de notificaciones push.

Pruebas locales del chat: 15 comprobaciones de permisos en el emulador, incluyendo
suplantación, otro cliente/conductor, tamaño, inmutabilidad y servicio finalizado.
El intercambio entre cuentas reales queda pendiente de publicación manual de reglas.

Compilación de APK de aplicación y APK de pruebas. Pruebas JVM de validación de tarjetas, política de checkout, estados y comprobantes. Pruebas REST contra Firestore efímero en localhost con proyecto `demo-iotelito-auth`: solicitud cliente, cierre administrativo, comprobante, rechazos por rol, campos privados, inmutabilidad y cierre concurrente. También se comprobaron las suites previas de hoteles/asignaciones, administrador, reportes y compatibilidad taxi.

La clase Android `HotelOperationsEmulatorTest` se adaptó al nuevo contrato y se compiló. No se ejecutó esa instrumentación ni se validaron pantallas en los dispositivos del equipo; tampoco se hicieron pruebas nuevas con cuentas reales.
