# Checkout compartido y cobro de demostración

Cambios preparados localmente en `ultima_03_11`. No se publicaron reglas, no se modificaron documentos del proyecto remoto y no se instalaron APK en los dispositivos del equipo.

## Flujo implementado

1. El cliente registra una tarjeta de prueba al confirmar su reserva.
2. Desde el día de salida puede solicitar checkout con 1 a 5 estrellas y observaciones obligatorias de hasta 4000 caracteres. También se permite una solicitud atrasada.
3. La transacción guarda `EN_CHECKOUT`, `rating`, `review` y `checkoutRequestedAt`. La tarjeta existente no se puede sustituir. Una reserva anterior sin tarjeta pide registrarla en ese momento.
4. El administrador ve la solicitud en su panel y en el filtro Checkout de Reservas. Puede añadir un cargo de demostración con monto, motivo y observación.
5. Al confirmar, una transacción crea `hotelCheckouts/{reservationId}` y cambia la reserva a `FINALIZADA`. Reintentar no genera otro comprobante ni altera el primero.
6. El cliente consulta el estado pendiente o el comprobante desde el detalle de su reserva. Solo un comprobante confirmado en servidor acredita el cobro simulado. Se muestran alojamiento, adicional, motivo, observación, total, tarjeta enmascarada, fecha e identidad del administrador.
7. El taxi tiene una acción independiente y no redirige automáticamente fuera del detalle de reserva.

## Datos guardados

En `reservations/{id}` se añaden `cardType`, `cardLast4`, `cardToken` con prefijo `DEMO_` y `paymentMode` igual a `SIMULATED`. No se conserva el número completo, el CVV ni el vencimiento. Los campos sensibles del formulario no participan del guardado automático de vistas Android.

`hotelCheckouts/{id}` conserva el desglose y la tarjeta enmascarada, sin token ni datos completos de la tarjeta. Es un comprobante interno de demostración, no tributario; no se cobra dinero ni se envía factura por correo.

Las reservas históricas `COMPLETADA` siguen siendo legibles. No se inventa un comprobante para las que se cerraron con el flujo anterior sin él, ni se reabren automáticamente. Las reservas anteriores confirmadas sin tarjeta pueden usar el diálogo de tarjeta de prueba.

La opinión se conserva en la reserva. Esta entrega no publica un catálogo público de reseñas ni recalcula todavía el promedio del hotel.

## Activación manual en Firebase (la realiza el usuario)

1. Abre la consola del proyecto Firebase del equipo y entra a Firestore Database > Reglas.
2. Guarda una copia de las reglas actualmente publicadas. Compáralas con el archivo `firestore.rules` de esta carpeta, especialmente los bloques de taxi, usuarios y hoteles. Si otro integrante añadió reglas nuevas, integra esas diferencias antes de reemplazar el archivo. El archivo local contiene las reglas combinadas del proyecto.
3. Coordina que cliente y administrador usen el APK de esta versión. La versión antigua del cliente intentaba finalizar directamente y será rechazada por las nuevas reglas; el administrador antiguo tampoco entiende el nuevo estado pendiente.
4. Copia el contenido completo del archivo combinado revisado al editor de reglas y pulsa Publicar. No requiere cambiar Authentication, activar facturación ni configurar Storage para este bloque.
5. Ejecuta la versión actualizada desde Android Studio en ambos roles. No hace falta crear a mano colecciones de comprobantes: aparecen al confirmar el cobro desde la app.

No se debe publicar una versión que elimine los bloques de taxi del equipo. No se ha verificado ni sustituido el contenido actualmente publicado en la nube desde esta tarea.

## Recorrido de comprobación con el equipo

- Reserva nueva: usa el número de prueba `4242 4242 4242 4242`, un vencimiento futuro y CVV `123`. Verifica que Firestore solo conserve los campos enmascarados descritos arriba.
- Para probar checkout, utiliza una reserva propia que ya tenga hoy o una fecha anterior como día de salida. Si no existe, coordina una reserva de prueba con el equipo; no cambies fechas de reservas comerciales solo para la demostración.
- Cliente: selecciona estrellas y observaciones y solicita checkout. Debe quedar pendiente, sin anuncio de pago realizado.
- Administrador: abre la solicitud del hotel asignado, añade si corresponde un cargo de prueba justificado y confirma. Debe aparecer un único documento `hotelCheckouts` con el mismo ID de la reserva.
- Cliente: vuelve a abrir el estado de checkout; debe mostrar el total con los adicionales, el motivo, la fecha y la tarjeta correcta. Si esa pantalla permanece abierta con conexión, observa el cambio remoto.
- Repite el acceso del administrador: el comprobante existente es de solo lectura y no se cobra de nuevo.
- Comprueba que la reserva y el comprobante no se puedan consultar desde otra cuenta cliente.

## Notificaciones y alcance

El administrador tiene un indicador en Reservas y una alerta en el panel. Los avisos Android requieren permiso de notificaciones.
El cliente recibe el aviso al observar los comprobantes con la aplicación abierta, al volver al panel o al consultar el comprobante. El importe proviene del comprobante del servidor. Los avisos nuevos de checkout comparten deduplicación y lectura por UID y se cancelan al cerrar sesión. La entrega push con la aplicación cerrada y el aislamiento completo de la bandeja antigua por cuenta pertenecen al bloque de notificaciones posterior.

## Verificación local

Compilación de APK de aplicación y APK de pruebas. Pruebas JVM de validación de tarjetas, política de checkout, estados y comprobantes. Pruebas REST contra Firestore efímero en localhost con proyecto `demo-iotelito-auth`: solicitud cliente, cierre administrativo, comprobante, rechazos por rol, campos privados, inmutabilidad y cierre concurrente. También se comprobaron las suites previas de hoteles/asignaciones, administrador, reportes y compatibilidad taxi.

La clase Android `HotelOperationsEmulatorTest` se adaptó al nuevo contrato y se compiló. No se ejecutó esa instrumentación ni se validaron pantallas en los dispositivos del equipo; tampoco se hicieron pruebas nuevas con cuentas reales.
