# Acceso y taxi del cliente

La sesión activa se conserva únicamente en memoria mientras vive el proceso.
«Recordarme» guarda el correo y la contraseña cifrada con AES-GCM, usando una clave
del Android Keystore. Al tocar el correo aparece una lista de cuentas guardadas;
seleccionar una completa ambos campos y mantiene la contraseña oculta. Siempre
debe pulsarse «Iniciar sesión»; no se entra automáticamente al abrir la aplicación.
Solo se recuerdan credenciales después de una autenticación correcta. Ingresar
con la casilla desmarcada elimina la credencial recordada de esa cuenta.
Cerrar sesión borra la sesión activa y limpia la pila de navegación, pero conserva
las credenciales recordadas para completar el siguiente inicio de sesión.
Los datos editables del perfil continúan en `PerfilRepository`.

`TaxiRepository` conserva en Room una solicitud por reserva y cliente, con hora, hotel,
origen, destino, pasajeros, maletas, conductor y estado. Volver a solicitar desde la misma reserva
abre la solicitud existente. Las vistas del cliente y del taxista siguen separadas.

La base `taxis_cliente.db` está aislada de la base de reservas. Su entidad, DAO y
Database están en `data/local/cliente/`. Las consultas y escrituras se ejecutan en
un `Executor`; el seguimiento observa cambios con `LiveData` ligado al ciclo de vida.
El perfil conserva sus preferencias locales; la sesión activa permanece en memoria.

Los pedidos de la versión anterior se importan desde `solicitudes_taxi` en una
transacción, sin cambiar sus IDs ni volver a emitir avisos. Las preferencias originales
se conservan como respaldo; después de la importación las solicitudes se leen y
actualizan exclusivamente en Room.

Flujo de comprobación en una misma instalación:

En la versión de desarrollo, al abrir `Mis reservas` se agregan automáticamente
7 ejemplos, cada uno en un hotel diferente: 3 con salida el día de la carga y monto
elegible para taxi, 1 en curso, 1 próxima, 1 finalizada y 1 cancelada. No hay botones
de prueba. Sus códigos estables evitan duplicarlos al volver a entrar o reiniciar
la app; las reservas anteriores y sus estados se conservan.

1. Entrar como cliente, abrir una reserva y solicitar taxi: aparece «Solicitado».
2. Cerrar sesión y entrar como taxista. En Solicitudes aparece el pedido del cliente.
3. Aceptar: se guarda «Taxista asignado» y se genera un aviso.
4. Iniciar recorrido: pasa a «En camino». El pedido propio sigue accesible en Solicitudes.
5. Llegar al destino y validar en el flujo existente: pasa a «Finalizado».
6. Entrar nuevamente como cliente y abrir la reserva o el aviso: aparece el estado guardado.
7. Ingresar con «Recordarme», cerrar sesión y tocar el correo: seleccionar la cuenta
   completa correo y contraseña, pero no inicia sesión hasta pulsar el botón.
8. Ingresar con la casilla desmarcada y cerrar sesión: esa cuenta ya no aparece en la lista.
9. Terminar el proceso y abrir la aplicación: aparece el login y las cuentas recordadas
   siguen disponibles; los pedidos continúan guardados en Room.

Cada cambio genera un aviso en la bandeja local; el aviso del sistema requiere
el permiso de notificaciones. Repetir una transición no genera otro aviso.
Solo el taxista que aceptó puede avanzar el estado.

Esta implementación utiliza almacenamiento local compartido por ambos roles en
el mismo dispositivo. Para cliente y taxista en dispositivos distintos hacen falta
un backend compartido y notificaciones remotas. El mapa, el QR y los datos de ruta
del flujo de taxista conservan las simulaciones existentes.
