# Lab 5 — Administrador de hotel

El entregable del 20 de octubre de 2026 es **Storage Local y notificaciones**. La autenticación y base de datos Firebase figuran en Lab 6 (3 de noviembre). Este avance funciona sin servidor y no requiere conexión a Internet.

## Implementación en esta rama

- `HotelAdminDatabase` usa Room 2.8.5 y guarda habitaciones, servicios, estado/consumos de checkout y respuestas del chat. Las consultas y escrituras corren en un executor, fuera del hilo de interfaz.
- `HotelAdminLocalStore` inicializa los ejemplos solo en la primera carga y después recupera los cambios guardados. Los datos generales del hotel (incluido el texto de atracciones) se leen y guardan en `iotelito_superadmin.db`, que también alimenta el catálogo del cliente. La galería de fotos de demostración de admin sigue en un archivo interno JSON.
- Si una instalación antigua guardó datos generales en `SharedPreferences`, se importan una sola vez por campo cuando el valor compartido conserva el dato inicial. Los valores ya editados por superadmin tienen prioridad; las preferencias antiguas se conservan como respaldo. La base de superadmin migra de versión 4 a 5 sin borrar hoteles ni fotografías.
- El administrador recibe una notificación local por cada checkout pendiente. Al pulsarla abre el cobro de esa reserva. Se solicita permiso de notificaciones una vez; al confirmar el checkout se retira el aviso.
- El checkout registra un cobro **de demostración** en el dispositivo. No carga una tarjeta ni envía una notificación al cliente; la pantalla lo indica expresamente.
- Las respuestas enviadas desde el chat quedan guardadas con fecha y hora local. Los mensajes de ejemplo iniciales permanecen como demostración. El chat todavía no intercambia mensajes con otra instalación ni con el cliente.

## Relación con las otras ramas

La rama local integra `origin/superadmin`, que incluye el almacenamiento de superadmin y los avances de cliente/taxi. El perfil del hotel ahora se comparte mediante la base local de superadmin. Habitaciones, servicios, checkout, chat y la galería de demostración siguen en el almacenamiento local propio de admin. No se usa servidor ni se han subido estos cambios.

## Validación manual

1. Ingresar con el usuario administrador del hotel. Conceder permiso de notificaciones cuando Android lo solicite. Debe aparecer una alerta de checkout pendiente; al tocarla se abre el cobro de la reserva de Alejandro.
2. Abrir **Hotel → Habitaciones**. Crear o editar una habitación, cerrar completamente la aplicación y volver a abrirla. El cambio debe continuar. Repetir con **Servicios**.
3. Abrir **Hotel → Datos del hotel**. Cambiar nombre o descripción y guardar. Volver al panel y comprobar el texto. Entrar como superadmin y cliente en la misma instalación: ambos deben mostrar el cambio. Reiniciar la app y comprobar que se mantiene.
4. Abrir **Hotel → Fotos**. Reemplazar o agregar una foto de demostración, guardar y reiniciar. La selección debe mantenerse.
5. Abrir **Mensajes**, responder a un chat y reiniciar. La respuesta debe reaparecer con fecha y hora.
6. Abrir **Reservas**, filtrar por **Checkout**, confirmar el cobro de Alejandro (con o sin daño). Reiniciar: la reserva debe seguir finalizada y no debe emitirse otra alerta pendiente para ella.

Estas comprobaciones son locales al dispositivo. Desinstalar la aplicación o borrar sus datos elimina las bases y preferencias de prueba.

## Verificación en emulador — 19/oct/2026

Se instaló el APK de depuración en Android 14 (`Pixel_4_Lab5`) y se ingresó con la cuenta admin. Se confirmó en pantalla que los cambios de perfil, precio de habitación, precio de servicio, cantidad de fotos y respuesta de chat reaparecen después de forzar el cierre y abrir la app de nuevo. El checkout permaneció fuera de pendientes tras reiniciar y su aviso dejó de aparecer. También se abrió la notificación de un checkout pendiente y se verificó que conduce al cobro de la reserva correcta. La compilación `:app:assembleDebug` y las pruebas `:app:testDebugUnitTest` terminaron correctamente.
