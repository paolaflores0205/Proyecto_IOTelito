# Superadmin: almacenamiento local y notificaciones

> Documento de la primera etapa, anterior a la integración. La rama de cliente ya se integró localmente. Consulta `INTEGRACION_CLIENTE_SUPERADMIN.md` para el estado actual; las referencias siguientes a «sin merge» describen únicamente aquella primera etapa.

## Alcance de este avance

Trabajo en la rama `superadmin`, sin hacer merge de `feature/inicio-cliente-reserva`.
No se modificaron las actividades, fragmentos, modelos generales ni repositorios del cliente.
Se conservaron `IoTelitoApplication`, el layout del menú inferior, los métodos que navegan entre pestañas y los catálogos iniciales.
Los archivos compartidos modificados son Gradle (dependencias Room) y el manifest (permiso de notificaciones y nueva actividad administrativa).

La información de superadmin está en `iotelito_superadmin.db`, dentro del almacenamiento privado de la app.
La base del cliente de la rama de Paola, `iotelito_local.db`, no se fusionó ni se reemplazó.
Los cambios administrativos todavía no aparecen automáticamente en las pantallas del cliente.
Las tareas de integración están en `PENDIENTES_CLIENTE_STORAGE.md`.

## Relación con las clases

- Semana 6: se mantiene NavController, NavHostFragment y el gráfico existente. Se agregan ViewModel y observadores vinculados al ciclo de vida de cada vista.
- Semana 7: Room representa datos estructurados del teléfono. Entity define la tabla, DAO sus consultas y Database la conexión. El Repository usa un ExecutorService de un solo hilo para leer y escribir fuera del hilo de la pantalla.
- Semana 7: SharedPreferences guarda si se pidió el permiso de notificaciones, evitando solicitarlo en cada entrada.
- Semana 8.1: NotificationChannel, POST_NOTIFICATIONS, NotificationCompat y PendingIntent permiten publicar avisos y abrir el hotel correspondiente.
- Semana 8.2: MongoDB y los servicios web son otro tema. Esta base Room es local; no implementa la base NoSQL remota del sistema final.

## 1. Dependencias

En `gradle/libs.versions.toml` se agregan `room-runtime` y `room-compiler`.
En `app/build.gradle` se usa `implementation libs.room.runtime` y `annotationProcessor libs.room.compiler`.
El procesador genera el código de Room para Java al compilar. No hay que escribir ese código generado.

## 2. Tablas y conexión

Paquete `data.superadmin.local`:

| Archivo | Propósito |
| --- | --- |
| AdminHotelEntity | Hotel, ciudad, dirección, contacto, descripción, coordenadas, estado y administradorId. |
| AdminUserEntity | Usuarios del catálogo, su rol y estado. No guarda contraseñas. |
| AdminLogEntity | Evento real: tipo, título, detalle y fecha/hora. |
| AdminAlertEntity | Aviso pendiente: clave estable, hotelId, mensaje, fecha, leído y notificado. |
| SuperadminDao | Consultas observables e inserciones/actualizaciones. |
| SuperadminDatabase | Una conexión Room compartida, versión inicial 1. |

Los 16 usuarios y 12 hoteles se insertan al inicializar una base vacía. Se mantienen sus IDs.
En las siguientes aperturas se conservan los registros existentes; no se vuelven a reemplazar por los ejemplos.
El historial empieza con la carga inicial y se amplía con las acciones realizadas. No se copian los eventos ficticios de SuperadminSampleData.LOGS.
Las coordenadas del catálogo no existían: no se inventan coordenadas para todos los hoteles. Al editar se debe escoger la ubicación real en el mapa.

La base es nueva y separada: no hay una migración sobre las tablas del cliente.
Si se agregan o cambian columnas en una versión posterior, se deberá aumentar la versión y escribir una Migration que conserve los datos; no borrar la base para resolver el cambio.

## 3. Repositorio y ViewModel

`SuperadminRepository` es el acceso único a los datos administrativos.

1. Encola la carga inicial en un hilo de trabajo.
2. Room entrega listas observables de hoteles, usuarios y eventos.
3. El fragmento observa mediante `getViewLifecycleOwner()`.
4. Al actualizar un registro, Room vuelve a emitir la lista y se refresca el RecyclerView y el resumen.
5. Cada escritura y su evento se guardan en una transacción.
6. Solo después de confirmar la transacción se notifica el éxito a la pantalla.

No se usa `allowMainThreadQueries()`. El patrón coincide con el uso de tareas en segundo plano mostrado en clase.

`SuperadminViewModel` conserva el estado de una operación al girar la pantalla. Mientras guarda se deshabilita el botón, evitando registros duplicados por doble toque.
El resultado se consume una sola vez para no repetir mensajes al recrear la actividad.

## 4. Hoteles

`FormularioHotelActivity` valida nombre, dirección, distrito, ciudad y una ubicación seleccionada.
Si se proporcionan correo y teléfono, se verifica su formato.
Guarda contacto, descripción y coordenadas junto con el estado del hotel.
El formulario de edición recupera esos campos guardados; ya no rellena todos los hoteles con el mismo correo o ubicación de ejemplo.
Las coordenadas seleccionadas se conservan al recrear la pantalla.

Al guardar, la pantalla vuelve a la lista. La lista observa la base y muestra el nuevo registro.
`DetalleHotelAdminActivity` observa el hotel y refleja los datos editados y el administrador asignado.
Su interruptor guarda el estado administrativo del hotel; no controla dispositivos IoT físicos.

## 5. Asignar administrador

La pantalla carga usuarios de la base y muestra únicamente los de rol Administrador que estén activos.
El buscador filtra nombre, correo y documento.
Al confirmar, guarda el ID del administrador y su nombre en el hotel, y registra un evento.
La validación se repite en el repositorio: un usuario inactivo no puede asignarse aunque la pantalla tuviera una lista anterior.

## 6. Usuarios y eventos

Activar/desactivar guarda el estado en Room.
Si se escribe un motivo de desactivación, se conserva en el detalle del evento.
Esta edición administrativa no altera el login local del cliente ni las cuatro cuentas de demostración.
Hacer que una desactivación bloquee el inicio de sesión requiere conectar el repositorio de autenticación con el registro del usuario; queda señalado para la integración.

Registro de eventos muestra los datos reales de `admin_events`, ordenados del más reciente al más antiguo, con búsqueda y filtros existentes.
Los reportes de ingresos y reservas siguen siendo demostrativos; no se conectaron con las reservas del cliente para respetar el alcance pedido.

## 7. Notificaciones administrativas

`SuperadminNotifications` crea el canal `superadmin_administration`.
Al entrar a superadmin se solicita el permiso una vez, si hace falta.
Se crean avisos por hoteles activos sin administrador asignado.

La clave `hotel_pending_ID` evita insertar y publicar de nuevo la misma alerta al regresar al inicio.
Si no hay permiso, el aviso permanece en la base y se puede ver dentro de la app.
Cuando se concede permiso y se regresa a superadmin, se intentan publicar los avisos pendientes.
Al asignar administrador o desactivar el hotel se retira su aviso pendiente y se cancela su notificación.
Un error de notificación no revierte ni presenta como fallido un hotel ya guardado.

Ruta para ver la bandeja: Inicio > avatar SA > Avisos administrativos.
Desde allí se pueden abrir los hoteles y configurar los permisos de Android.
Al tocar una notificación del sistema se abre el hotel usando su ID. El aviso se marca como leído.

Son notificaciones locales, generadas por la app en este teléfono. No son mensajes push entre teléfonos ni actualizaciones recibidas de un servidor.

## 8. Demostración para el entregable

1. Iniciar sesión como superadmin y conceder permiso de notificaciones.
2. Revisar que existan 16 usuarios y 12 hoteles iniciales.
3. Registrar un hotel con ciudad, distrito, contacto y un punto elegido en el mapa.
4. Volver a Hoteles y buscar su nombre.
5. Abrir Perfil > Avisos administrativos; abrir el aviso del nuevo hotel.
6. Asignar uno de los administradores activos. Confirmar que cambió en el detalle y desapareció el aviso pendiente.
7. Editar el hotel y comprobar que recupera contacto y coordenadas.
8. Cambiar el estado de un usuario y escribir un motivo.
9. Ir a Registro de eventos y revisar las operaciones con fecha y hora.
10. Cerrar completamente la app, volver a entrar y comprobar que los cambios permanecen.

No desinstalar ni borrar los datos para esta prueba: el almacenamiento privado se elimina al hacerlo.
La apariencia y la lógica del menú inferior permanecen como estaban en la rama superadmin.

## 9. Pruebas

`SuperadminStorageTest` utiliza un archivo temporal, distinto de la base de uso normal, para verificar:

- Carga inicial sin duplicados y persistencia del estado al cerrar y reabrir la conexión.
- Registro, edición, asignación, ubicación y estado del hotel.
- Alertas sin duplicados y retirada al asignar administrador.
- Rechazo de administrador inactivo sin cambios parciales ni eventos de éxito falsos.
- Publicación en el canal administrativo con PendingIntent.

`SuperadminUiTest` comprueba además el menú blanco de 64dp, su posición sobre la barra del sistema,
las opciones sin recorte, la navegación a Usuarios/Hoteles, la validación del formulario,
la lista de administradores activos y la carga de la bandeja de avisos.

Para ejecutar en un emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.proyecto_iotelito.data.superadmin.SuperadminStorageTest'
```

No se hizo commit ni push como parte de este avance.
