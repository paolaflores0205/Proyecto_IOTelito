# Integración local de cliente y superadmin

## Qué se integró

Se incorporó `origin/feature/inicio-cliente-reserva`, hasta el commit `41a4ffc`, a `superadmin`. El trabajo administrativo previo quedó protegido en `cb80a4b`. No se hizo push durante esta integración.

Se conservaron la lógica del menú inferior y sus insets, el fondo blanco y los catálogos iniciales de 16 usuarios y 12 hoteles. Si la base ya contiene datos, no se reemplazan por los ejemplos.

De cliente se incorporaron el login y la sesión local, el recordatorio de credenciales, la selección de habitaciones, reservas y avisos persistentes, y el flujo local de taxi compartido con taxista. No se sustituyeron sus pantallas por un diseño nuevo.

## Cambios adicionales para comunicar a la compañera

### Catálogo de hoteles compartido

`HotelCatalogRepository` adapta los hoteles persistidos por `SuperadminRepository` al modelo `Hotel` del cliente. Explorar, ListaHoteles y Resultados observan el catálogo mediante LiveData; DetalleHotel consulta el hotel persistido.

Se actualizó también la consulta del hotel en los detalles de habitación, confirmación, reservas, checkout, chat y taxi. `ReservaAdapter` muestra su nombre actualizado.

Se mantienen los IDs. En hoteles conocidos, precios, servicios, valoraciones y habitaciones siguen usando los datos de demostración del cliente. Nombre, dirección, ciudad, contacto, descripción, coordenadas y estado proceden del almacenamiento administrativo.

Los hoteles nuevos se muestran sin inventar tarifas o disponibilidad. El botón de habitaciones queda deshabilitado si no hay inventario configurado. Las búsquedas excluyen hoteles desactivados, pero una reserva histórica puede seguir mostrando su hotel. Un ID desconocido ya no abre silenciosamente el primer hotel o la primera reserva.

### Consultas fuera del hilo principal

Se retiró `allowMainThreadQueries()` de `IoTelitoDatabase`. `ClientLocalTasks` ejecuta las consultas y escrituras en segundo plano. `ClientDataLoader` carga los datos de las pantallas y entrega el resultado al observarlo con el ciclo de vida de la actividad.

`ReservaDao` y `NotificacionDao` exponen listas LiveData. ReservasFragment y NotificacionesActivity las observan y Room las actualiza después de una escritura.

ConfirmacionReservaActivity espera a que se guarde la reserva antes de navegar. CheckoutActivity espera a que se actualice el estado antes de continuar. Ambos deshabilitan el botón durante la operación para evitar pulsaciones repetidas. La persistencia de avisos del cliente también trabaja en segundo plano.

Se conservó la versión 2 de la base del cliente y su migración 1 a 2. No se borraron bases ni se usó una migración destructiva.

### Sesión y menú

PerfilSuperadminActivity limpia la sesión mediante el nuevo SessionRepository. Las inicializaciones de perfil y canal de avisos se añadieron a IoTelitoApplication manteniendo la lógica corregida de insets y onActivityPostCreated.

## Bases de datos

- `iotelito_superadmin.db`: hoteles, usuarios, eventos y alertas administrativos.
- `iotelito_local.db`: reservas y avisos del cliente.
- `taxis_cliente.db`: solicitudes de taxi.

El repositorio conecta el catálogo sin tener que juntar los archivos de base de datos ni reemplazar las reservas existentes. Todo se comparte entre roles de la misma instalación, no entre teléfonos. Esto no implementa todavía una base NoSQL remota.

## Cómo probarlo manualmente

1. Ejecutar la app e ingresar como superadmin con las cuentas de demostración existentes.
2. Editar el nombre o descripción de un hotel conocido y guardar.
3. Cerrar sesión e ingresar como cliente en el mismo dispositivo.
4. Buscar y abrir ese hotel: debe mostrar los cambios guardados.
5. Volver a superadmin y registrar un hotel nuevo.
6. Volver a cliente: debe aparecer en el catálogo, sin permitir reservar mientras no tenga habitaciones y tarifas configuradas.
7. Crear una reserva en un hotel con inventario de demostración, abrir su detalle y probar checkout.
8. Cerrar y volver a abrir la app para comprobar persistencia, sin borrar los datos de la aplicación.
9. Probar el flujo de taxi entre cliente y taxista en la misma instalación.
10. Comprobar visualmente el menú en el Samsung A55. Se conservó la implementación corregida, pero las pruebas automatizadas se ejecutaron en Pixel 4.

## Verificación realizada

Compilación de depuración y pruebas unitarias correctas. Pasaron las 17 pruebas instrumentadas en Pixel 4 con Android 14: navegación, almacenamiento administrativo, credenciales recordadas, taxi y dos pruebas nuevas de integración.

Las pruebas nuevas verifican que un hotel creado por superadmin llega al catálogo y detalle del cliente sin inventar disponibilidad, que un ID inexistente no abre otra reserva y que detalles, checkout y avisos cargan sin consultas Room en el hilo principal. Los registros creados por estas pruebas se eliminan por sus IDs específicos.

## Pendiente para otro avance

Reservas, perfil y bandeja de avisos todavía no están aislados por cuenta. El login de demostración no comprueba aún el estado activo del usuario administrativo. GPS y QR siguen siendo simulaciones. Las habitaciones y tarifas de nuevos hoteles requieren gestión persistente. Estos cambios no formaban parte del alcance acordado de esta fusión.
