# Lab 5 - Taxista: almacenamiento local y notificaciones

Implementado sobre `feat/hotel-admin`, base `7995b00`. Los cambios de código y recursos pertenecen únicamente al rol taxista. No se modificaron los repositorios del curso ni las pantallas, bases, permisos, dependencias o repositorios de los otros roles. No se hizo commit ni push.

## Qué puede demostrarse

- Se mantienen los seis pedidos de ejemplo, los siete registros iniciales del historial, RecyclerView, filtros y orden por mayor tarifa.
- Los ejemplos se insertan una sola vez por cuenta. Aceptar un pedido lo retira de disponibles y el cambio permanece después de cerrar la app.
- Un taxista no puede aceptar otro pedido mientras tiene un servicio activo o está marcado como no disponible.
- Inicio permite continuar el servicio en la etapa guardada: Asignado, En camino o En traslado.
- Pasajero, hotel, destino y tarifa de demostración corresponden al pedido elegido durante todo el recorrido y en su resumen.
- Finalizar actualiza el mismo registro de servicio, evitando duplicar el historial. Se guarda la fecha y hora de finalización.
- Los contadores de servicios y traslados de Inicio se calculan con la actividad del día. La valoración 4.8 sigue siendo un dato de ejemplo, no una calificación calculada de un servidor.
- La disponibilidad se comparte entre Inicio y Perfil y permanece después de reiniciar.
- Cambiar foto abre el selector de imágenes. Se guarda una copia privada y reducida de la imagen; quitar foto elimina esa copia. Se conserva el avatar circular.
- Los mensajes del chat se guardan por cuenta y servicio con fecha y hora. La pantalla identifica las respuestas automáticas como demostración local.
- Perfil > Notificaciones muestra los avisos guardados. Tocarlos abre el pedido o el resumen del servicio. Incluye acceso a la configuración del canal en Android.

## Relación con las clases

Material revisado: Clase 07 Storage, Clase 07 Extra Database, Clase 08.1 Notificaciones y Clase 08.2 NoSQL proporcionados por el usuario.

- Clase 7: `SharedPreferences` para valores pequeños y archivos internos para la foto. `apply()` guarda las preferencias de disponibilidad.
- Clase 7 extra: Room con Entity, DAO y Database. `ConductorRepository` realiza I/O en un `ExecutorService`, fuera del hilo UI. `TaxistaViewModel` conserva el repositorio y las vistas observan los cambios con `LiveData` y su ciclo de vida.
- Clase 8.1: `NotificationChannel`, permiso `POST_NOTIFICATIONS`, `NotificationCompat.Builder`, `PendingIntent` con el identificador del servicio y `setAutoCancel(true)`.
- Clase 8.2 trata la persistencia remota. Este entregable conserva la persistencia en el teléfono; no añade MongoDB, Firebase ni un servidor.

Repositorios consultados exclusivamente en modo lectura:

- [Clase7](https://github.com/2026-2-1TEL05-AppsIot/Clase7), ejemplo `MainActivity.java`.
- [Clase8_1](https://github.com/2026-2-1TEL05-AppsIot/Clase8_1), ejemplo `NotificationActivity.java`.

Se reutilizan las versiones de librerías que ya utiliza el proyecto integrado.

## Organización del código

Base propia: `iotelito_taxista.db`, en el almacenamiento privado de la aplicación.

| Tabla | Datos |
| --- | --- |
| `conductor_servicios` | Pedidos, servicio activo e historial. Clave por cuenta e ID. |
| `conductor_avisos` | Avisos con clave estable, fecha, estado leído y publicación. |
| `conductor_mensajes` | Mensajes por cuenta y servicio con fecha y hora. |
| `conductor_inicio` | Registro de carga inicial por cuenta para evitar reinsertar los ejemplos. |

Los registros históricos iniciales usan IDs negativos para separarlos de los seis pedidos de demostración. Los pedidos del cliente conservan sus IDs originales, generados a partir de 100000 por el repositorio existente.

Los archivos nuevos de almacenamiento están en `data/taxista/`. Las clases auxiliares, ViewModel, notificaciones y bandeja están en `ui/taxista/`. Los textos nuevos están en `strings_taxista_local.xml`.

## Conexión con cliente

Se utilizan los métodos existentes de `TaxiRepository`, sin modificar su implementación ni su base.

1. Al entrar o actualizar el módulo, se consultan pedidos compartidos disponibles y propios en curso.
2. Al aceptar un pedido del cliente se utiliza su operación de asignación y se conserva una copia del viaje para el conductor.
3. El cliente guarda Asignado, En camino y Finalizado. El conductor guarda además En traslado en su propia base. Durante esa etapa el cliente conserva En camino, según su implementación actual.
4. Al finalizar se confirma el cambio compartido y se actualiza el registro propio del taxista.
5. Los pedidos asignados a otro conductor dejan de mostrarse como disponibles en la siguiente actualización.

La lista propia se observa independientemente de la consulta compartida, de modo que una falla al consultar pedidos del cliente no impide mostrar los ejemplos ya guardados. La tarifa de los pedidos compartidos no existe en el modelo del cliente: se muestra como no informada.

No hay una transacción única entre las dos bases. Se utiliza la asignación condicional existente y se reconcilia el estado compartido al actualizar el rol.

## Notificaciones

Canal exclusivo: `taxista_servicios`. Las notificaciones del taxista usan etiquetas con cuenta y clave para no cancelar ni sobrescribir las de otros roles.

- Se crean avisos por las solicitudes pendientes detectadas al entrar o actualizar el módulo y por los servicios finalizados.
- La combinación tipo/servicio evita avisos duplicados. La publicación exitosa se registra en Room.
- Al aceptar un pedido se retira su aviso pendiente y su notificación del sistema.
- Si Android no concede permiso, los avisos permanecen disponibles dentro de Perfil > Notificaciones.
- La disponibilidad controla la publicación de avisos de nuevas solicitudes. Los avisos de servicios finalizados no dependen de ese interruptor.
- El destino comprueba la sesión y la cuenta del taxista antes de abrir el servicio. Al iniciar la app sin sesión se muestra el login.

Son avisos locales, no push entre teléfonos. No se implementó un proceso que reciba pedidos nuevos con la app cerrada; se detectan al actualizar el módulo. Los seis pedidos de demostración pueden generar avisos en la primera carga.

## Cómo probarlo en Android Studio

1. Ejecutar el proyecto y entrar con la cuenta taxista del equipo. Si Android solicita permiso, concederlo.
2. Abrir Solicitudes. Comprobar los ejemplos y los filtros de menos de diez minutos y mayor tarifa.
3. Cambiar disponibilidad en Perfil. Volver a Inicio y comprobar que coincide. Cerrar la aplicación y volver a entrar: se conserva.
4. Marcar disponible y aceptar, por ejemplo, el pedido de Carlos Mendoza. Inicio debe permitir continuarlo y la lista disponible debe excluirlo.
5. Iniciar recorrido y llegar al hotel. Comprobar que aparece Carlos al ir al aeropuerto. Cerrar completamente la app, volver a entrar como taxista y continuar desde Inicio: debe recuperar En traslado.
6. Abrir el chat, enviar un mensaje y regresar. Los mensajes y sus horas permanecen, incluso después de reiniciar.
7. En Finalizar ingresar el código mostrado como demostración: `TAXI-` seguido del ID. Para Carlos, `TAXI-2`. Confirmar la finalización de demostración.
8. Comprobar el pasajero en el resumen, el nuevo registro del historial, sus filtros y el aviso de servicio finalizado. Reiniciar y comprobar que no se duplica ni reaparece el pedido como disponible.
9. Abrir Perfil > Editar foto, seleccionar una imagen, volver a Inicio y Perfil y reiniciar. La imagen debe permanecer. Quitarla y comprobar que reaparecen las iniciales.
10. Abrir Perfil > Notificaciones. Tocar un aviso y comprobar el registro correspondiente. Revisar también el aviso en la barra de Android.

Para probar integración: crear un pedido desde cliente, cerrar sesión, entrar como taxista en la misma instalación, atenderlo y finalizar. Al volver a cliente se conserva Finalizado.

No desinstalar ni borrar los datos durante la comprobación de persistencia: Android elimina con ello el almacenamiento privado.

## Verificación realizada

- APK de depuración compilado y pruebas unitarias del proyecto aprobadas.
- Cinco pruebas instrumentadas de almacenamiento e integración: cierre/reapertura de base, conservación de ejemplos, un servicio activo, transiciones válidas, historial sin duplicados, mensajes con fecha, aislamiento por cuenta, preferencias, canal y compatibilidad con el estado del cliente.
- Dos pruebas instrumentadas de interfaz en Android 14: listas y filtros, disponibilidad entre pestañas, PendingIntent real, recorrido conservando pasajero, recuperación al reabrir la actividad, finalización, importación de foto y acceso a la bandeja.
- Captura final de la bandeja inspeccionada visualmente; foto circular y tarjetas legibles.
- Las pruebas usan cuentas y bases de prueba y eliminan exclusivamente sus registros temporales. La prueba de integración limpia su pedido compartido y sus avisos.
- El cierre completo del proceso está incluido en la guía manual; la automatización comprueba reapertura de la base y de las actividades, no simula una muerte completa del proceso.

Comandos:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.proyecto_iotelito.data.taxista.ConductorStorageTest,com.example.proyecto_iotelito.ui.taxista.TaxistaUiTest'
```

## Funciones de demostración que siguen pendientes de integración remota

Mapas y tiempos de recorrido siguen siendo demostrativos. No hay seguimiento GPS real, lectura de QR con cámara ni validación del código contra un servidor. La pantalla de cierre indica expresamente que se trata de una validación local de demostración. El chat no intercambia mensajes entre dispositivos y la calificación conserva el valor de ejemplo.

## Ajustes de navegación y prioridad visual

- Campana en el encabezado de Inicio con contador de avisos sin leer. Se mantiene también el acceso desde Perfil.
- Tarjeta «Tu viaje en curso» inmediatamente debajo del saludo, con pasajero, ruta, estado y botón para continuar.
- El aviso de solicitudes disponibles usa el conteo y los datos de un pedido pendiente, sin un hotel fijo. Se oculta si hay viaje activo o el conductor está no disponible.
- Las pantallas de servicio asignado, recorrido, validación y resumen final muestran la misma navegación inferior. Cambiar de sección conserva el viaje; Inicio permite retomarlo.
- Compilación y pruebas unitarias aprobadas. Verificados en el emulador el acceso de la campana, los filtros, la navegación del recorrido a Inicio y la recuperación de En traslado. Las capturas de Inicio y del recorrido fueron inspeccionadas.
