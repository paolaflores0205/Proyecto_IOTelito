# Lab 5 — Administrador de hotel

El entregable del 20 de octubre de 2026 es **Storage Local y notificaciones**. La autenticación y base de datos Firebase figuran en Lab 6 (3 de noviembre). Este avance funciona sin servidor y no requiere conexión a Internet.

## Implementación en esta rama

- `HotelAdminDatabase` usa Room 2.8.5, la misma versión declarada en `origin/superadmin`. Guarda habitaciones, servicios, estado/consumos de checkout y respuestas del chat. Las consultas y escrituras corren en un executor, fuera del hilo de interfaz.
- `HotelAdminLocalStore` inicializa los ejemplos solo en la primera carga y después recupera los cambios guardados. El perfil pequeño del hotel se guarda en `SharedPreferences`; la lista de recursos de la galería se guarda como JSON en un archivo interno.
- El administrador recibe una notificación local por cada checkout pendiente. Al pulsarla abre el cobro de esa reserva. Se solicita permiso de notificaciones una vez; al confirmar el checkout se retira el aviso.
- Las respuestas enviadas desde el chat quedan guardadas con fecha y hora local. Los mensajes de ejemplo iniciales permanecen como demostración. El chat todavía no intercambia mensajes con otra instalación ni con el cliente.

## Relación con las otras ramas

Se revisaron `origin/superadmin` (incluye `cb80a4b`, storage y notificaciones de superadmin, y `e9c497e`, integración local de cliente/superadmin) y `origin/feature/inicio-cliente-reserva` (`41a4ffc`, Room y notificaciones de cliente/taxi). Los roles usan bases locales separadas y trabajan sin servidor. Este cambio se limita al módulo admin de la rama actual; **no incorpora los commits de esas ramas**.

## Validación manual

1. Ingresar con el usuario administrador del hotel. Conceder permiso de notificaciones cuando Android lo solicite. Debe aparecer una alerta de checkout pendiente; al tocarla se abre el cobro de la reserva de Alejandro.
2. Abrir **Hotel → Habitaciones**. Crear o editar una habitación, cerrar completamente la aplicación y volver a abrirla. El cambio debe continuar. Repetir con **Servicios**.
3. Abrir **Hotel → Datos del hotel**. Cambiar nombre o descripción y guardar. Volver al panel y comprobar el texto. Reiniciar la app y comprobar que se mantiene.
4. Abrir **Hotel → Fotos**. Reemplazar o agregar una foto de demostración, guardar y reiniciar. La selección debe mantenerse.
5. Abrir **Mensajes**, responder a un chat y reiniciar. La respuesta debe reaparecer con fecha y hora.
6. Abrir **Reservas**, filtrar por **Checkout**, confirmar el cobro de Alejandro (con o sin daño). Reiniciar: la reserva debe seguir finalizada y no debe emitirse otra alerta pendiente para ella.

Estas comprobaciones son locales al dispositivo. Desinstalar la aplicación o borrar sus datos elimina las bases y preferencias de prueba.
