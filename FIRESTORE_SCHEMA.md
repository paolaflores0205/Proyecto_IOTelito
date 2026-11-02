# Contrato compartido Firestore - Cliente y Superadmin

Revisión: 02/11/2026, pasos 1 a 5. Este documento define el contrato de integración;
**no crea colecciones, cuentas, índices ni publica reglas en Firebase**. Conserva
los nombres y campos que ya usa el módulo cliente. Las ampliaciones de
superadmin se marcan como pendientes hasta que se implementen y prueben.

## Estado de la integración

Actualización pasos 3 y 4: el código ya resuelve roles desde users/{uid}, registra
perfiles CLIENT y conecta la vista Usuarios y sus cambios de estado a Firestore.
firestore.rules incorpora permisos de usuarios y gates por active. Esto está probado
localmente, **no desplegado**. Antes de usarlo hay que provisionar cuentas/perfiles
existentes y revisar/publicar las reglas: ver GUIA_FIREBASE_SUPERADMIN_PASOS_3_4.md.

Actualización paso 5: hoteles, estado y asignaciones de superadmin ya usan Firestore.
El cliente autenticado lee el mismo catálogo e inventario remoto. La importación
desde Room es explícita y omite documentos existentes. Ver
GUIA_FIREBASE_HOTELES_PASO_5.md para la preparación y los límites de esta etapa.

| Área | Implementado en el código integrado | Pendiente |
|---|---|---|
| Authentication | Login Firebase de los cuatro roles, resolución por UID/rol/active | Configuración de cuentas y perfiles en consola y prueba remota |
| Perfil cliente | Registro de users/{uid} y perfil adicional local por UID | Migración de cuentas Auth antiguas sin perfil y prueba remota |
| Hoteles y habitaciones | CRUD sin borrado, asignaciones atómicas, lectura remota e importación explícita de ejemplos | Despliegue y prueba remota; gestión de inventario por hotel-admin |
| Reservas | Firestore para cliente autenticado, Room sin sesión Firebase | Consultas administrativas autorizadas y reportes remotos |
| Usuarios superadmin | Listado, detalle y activación Firestore; reglas locales verificadas | Despliegue de reglas y prueba con cuentas reales |
| Logs, alertas y reportes superadmin | Room y datos de demostración | Repositorios Firestore y reglas específicas |
| Fotografías | Recursos de la app y fotos locales | Almacenamiento compartido de nuevas fotos |

El contrato no implica que las reglas de este repositorio estén desplegadas ni
que se haya comprobado la consola del proyecto. Las reglas locales habilitan users
y hoteles con restricciones de rol y mantienen cerrados los logs. No se abren
provisionalmente a cualquier usuario.

Base: `(default)` del proyecto `iot-telito`. Las claves de documento de hotel
conservan los IDs numéricos usados por la aplicación Android: `hotels/1`, etc.

## Datos que el cliente consulta

La creación y edición de hoteles corresponde a superadmin; el cliente los lee.
Con sesión Firebase, HotelCatalogRepository usa Firestore para catálogo, detalle
e inventario: no sustituye un error remoto por ejemplos locales. El catálogo sin
sesión se conserva para consumidores locales y pruebas, pero el login público
requiere Firebase. CatalogoHabitaciones aporta el inventario de ejemplo durante
la importación explícita; no sustituye el inventario remoto al reservar.

`hotels/{hotelId}`:

| Campo | Tipo | Uso |
|---|---|---|
| `id` | número entero | ID del hotel, igual al documento |
| `name`, `city`, `address`, `description` | texto | Búsqueda y detalle |
| `active`, `hasInventory` | booleano | Visibilidad y botón de reserva |
| `pricePerNight`, `rating` | número | Filtros y tarjeta |
| `reviews` | número entero | Detalle |
| `services` | arreglo de textos | Servicios del hotel |

`hotels/{hotelId}/rooms/{roomId}` (lectura e importación implementadas): `id`, `hotelId`,
`tipo`, `nombreCodigo`, `areaM2`, `adultos`, `ninos`, `precioNoche`,
`descripcion`, `disponible`. El ID de habitación representa una unidad física,
no solo un tipo. El filtrado por fechas ya consulta los bloqueos roomNights en
FirebaseReservaStore; el inventario del cliente autenticado también es remoto.
Las altas ordinarias de hoteles comienzan sin habitaciones y sin disponibilidad
para reservar. La gestión remota del inventario por HOTEL_ADMIN sigue pendiente.

## Datos que el cliente crea y consulta

`reservations/{reservationId}`: `id`, `clientUid`, `hotelId`, `roomIds`,
`roomName`, `roomNumber`, `fechaEntrada`, `fechaSalida`, `huespedes`,
`precioTotal`, `estado`, `codigo`, `createdAt`. Fechas ISO `AAAA-MM-DD`;
el intervalo es `[fechaEntrada, fechaSalida)`, por lo que el día de salida ya
puede reservarse como entrada de otra estadía.

`roomNights/{hotelId}_{roomId}_{fecha}` y `clientNights/{uid}_{fecha}` son
bloqueos por noche creados en la misma transacción que la reserva. Una reserva
se rechaza si ya existe cualquier bloqueo de habitación o de cliente para las
noches solicitadas. Los bloqueos históricos se conservan; el checkout no los
elimina.
Las imágenes del catálogo siguen siendo recursos locales de demostración.

`firestore.rules` permite leer el catálogo con una cuenta autenticada y crear
reservas propias. Solo SUPERADMIN activo puede crear/editar hoteles y efectuar
asignaciones. La reserva verifica de nuevo hotel activo, inventario y habitaciones
disponibles en la transacción. Las reglas también exigen hotel activo con inventario.

## Identificadores y convenciones compartidas

- Un único proyecto Firebase: iot-telito, base (default), configuración Android
  ya integrada. No se crea un Firebase separado para superadmin.
- users usa el UID real de Firebase Authentication como ID documental. No se
  usa el correo, DNI ni el ID entero de Room como identificador de autenticación.
- hotels conserva documentos con ID de texto decimal, por ejemplo hotels/1,
  y su campo id entero. rooms sigue la misma convención dentro de cada hotel.
  No renumerar los 12 hoteles existentes ni sus habitaciones al migrar.
- reservations conserva el ID entero aleatorio existente y su representación
  decimal como ID documental. No cambiarlo por UUID sin adaptar sus consumidores.
- Fechas de estadía: texto ISO AAAA-MM-DD, intervalo [entrada, salida).
  Auditoría y accesos: Timestamp de servidor; no fechas formateadas para pantalla.
- Roles canónicos: CLIENT, HOTEL_ADMIN, TAXISTA, SUPERADMIN, coincidiendo con
  UserRole.java. Traducirlos a etiquetas en la UI, no guardar "Administrador"
  como otro rol diferente. La asignación de roles no la decide el registro público.
- Campos nuevos de dinero pueden acordarse en céntimos enteros, pero se conserva
  precioTotal y pricePerNight como números para no romper el cliente integrado.
- No guardar passwords, hashes de passwords, tarjetas completas ni CVV en Firestore.

## users/{uid} - Perfil y autorización (código implementado; configuración remota pendiente)

| Campo | Tipo | Significado |
|---|---|---|
| uid | texto | Igual al ID del documento y al UID autenticado |
| name, email | texto | Perfil visible; name permite reutilizar las tarjetas actuales |
| firstNames, lastNames | texto | Datos del formulario de registro |
| documentType, documentNumber | texto | Documento personal, sin usarlo como UID |
| birthDate | texto ISO o null | Fecha de nacimiento |
| phone, address | texto | Contacto |
| role | texto enum | Uno de los cuatro roles canónicos |
| active | booleano | Habilitación de operaciones de la aplicación |
| hotelId | entero o null | Hotel asignado al HOTEL_ADMIN, no autorización por sí solo |
| photoUrl | texto o null | URL compartida cuando exista carga de fotos |
| createdAt, updatedAt, lastAccessAt | Timestamp | Auditoría; lastAccessAt puede ser null al crear |
| legacyLocalId | entero o null | Referencia de migración del catálogo local, no identidad Auth |
| statusReason | texto | Motivo de la última desactivación; vacío al reactivar |
| updatedByUid | texto | Superadmin que efectuó el cambio de estado |

Firebase Auth valida credenciales; este documento aporta el perfil y rol de negocio.
Crear un registro de Auth no crea automáticamente este documento: el registro
cliente deberá escribirlo con role CLIENT y active true, bajo reglas que impidan
autopromoción. El alta de superadmin y administradores se resolverá por un flujo
controlado, sin cambiar accidentalmente la sesión del superadmin que crea la cuenta.

active false no equivale a deshabilitar automáticamente la cuenta en Firebase Auth.
El flujo futuro deberá rechazar el acceso funcional y las reglas deberán consultar
su estado para impedir lecturas/escrituras protegidas, incluso con una sesión previa.
Solo cambiar la etiqueta o guardar una variable local no cumple esta restricción.

La relación administrador-hotel ya se guarda de forma consistente: hotel.administratorUid
y user.hotelId se actualizan juntos mediante una transacción autorizada.
Para este contrato se mantiene un administrador por hotel y un hotel por administrador.
Un nombre visible o un hotelId proporcionado por el cliente no otorga permisos.

## Ampliaciones de hotels/{hotelId} (paso 5 implementado)

Se conservan los campos del catálogo cliente descritos arriba y se añaden:

| Campo | Tipo | Significado |
|---|---|---|
| district, phone, email | texto | Formulario administrativo |
| latitude, longitude | número o null | Ubicación; las dos son obligatorias para altas nuevas |
| administratorUid | texto o null | Referencia a users/{uid}; null representa sin asignar |
| administratorName | texto o null | Copia para presentación, nunca usada para autorización |
| photos | arreglo de textos | Referencias compartidas de imágenes |
| attractions | texto o null | Conserva el formato actual de lugares cercanos |
| createdAt, updatedAt | Timestamp | Auditoría |
| createdByUid, updatedByUid | texto | Responsable autenticado |
| dataOrigin | APP o LOCAL_IMPORT | Alta normal o importación explícita de ejemplos |

Los contadores rooms/reservations visibles hoy se calculan de inventario y reservas
o se mantienen como resúmenes controlados; no convertir los números de demostración
en estadísticas reales. hasInventory debe corresponder al inventario real publicado,
no activarse solo porque existe un hotel en Firestore.

Los IDs nuevos de hoteles no se obtienen con max(local)+1. La transacción lee
system/hotelIds.nextId (inicialmente 1000000), crea el hotel y avanza el contador
juntos; registra updatedAt y updatedByUid. Los IDs 1..999999 se reservan para
importación del catálogo antiguo, conservando también los IDs de sus habitaciones.
La carga inicial es explícita, repetible y no sobrescribe documentos existentes.
Cada hotel y sus habitaciones se publican atómicamente, pero el conjunto completo
puede quedar parcial si falla la conexión; repetir omite hoteles ya existentes.
No se crean cuentas Auth ficticias ni se importan reservas/estadísticas simuladas.
Los hoteles cuyo administrador local no tenga cuenta real quedan sin asignar hasta
resolver su UID; no se inventa un UID a partir de su nombre o correo.

Las fotos android.resource son demostración empaquetada. Las URI content:// de un
teléfono no son referencias compartidas: no se tratarán como URLs accesibles desde
otro dispositivo. No se publican imageRes/fotoRes de Android como IDs remotos estables.
El campo photos guarda referencias HTTPS o recursos android.resource con nombre.
Las fotos file/content se conservan aparte en el dispositivo por UID e ID de hotel
(hotel_private_galleries); la carga compartida y su visualización completa por URL
corresponden al paso 6. No se considera una copia local como foto ya subida.

## logs/{logId} - Eventos administrativos (pendiente)

ID automático de Firestore. Campos: actorUid (texto), actorRole (rol canónico),
type (texto de categoría), action (texto estable), title/detail (texto visible),
entityType y entityId (texto), hotelId (entero o null), createdAt (Timestamp de servidor).
Ejemplos de action: HOTEL_CREATED, HOTEL_UPDATED, HOTEL_ADMIN_ASSIGNED,
USER_ACTIVATED y USER_DEACTIVATED.

Las credenciales y datos financieros sensibles nunca se incluyen en detail.
Los logs serán de solo anexado para los flujos autorizados; los clientes no podrán
editar o borrar el historial. Un log generado desde una app cliente no equivale a
una auditoría infalsificable de servidor: las reglas deben limitar sus campos y autoría.

## Reservas, reportes y alertas de superadmin (pendientes)

Superadmin consultará reservations con permisos globales por rol; HOTEL_ADMIN
consultará solo las de su hotel. No reutilizar sin cambios la consulta personal
whereEqualTo(clientUid, uid) del cliente, ni permitir lectura global a todo autenticado.
Los filtros de hotel/fecha podrían requerir índices compuestos, que se definirán
y probarán cuando se implemente cada consulta. No se asume que ya están desplegados.

Las alertas iniciales (usuarios inactivos y hoteles sin administrador) se derivarán
de users y hotels, evitando colecciones duplicadas innecesarias. Los contadores
y reportes reales se derivarán de datos remotos; DemoBookingHistory seguirá siendo
un conjunto de demostración separado. No subirlo a reservations como actividad real.

## Matriz objetivo de permisos (implementación parcial hasta el paso 5)

Actualmente HOTEL_ADMIN no escribe hoteles ni habitaciones. SUPERADMIN puede
importar/crear habitaciones, pero actualizar/borrar inventario permanece cerrado.
Las consultas administrativas de reservas y los logs de la tabla son objetivos
pendientes, no permisos ya desplegados. La pantalla hotel-admin debe adaptarse
por su responsable a users.hotelId; no se migró su repositorio local en este paso.

| Recurso | CLIENT activo | HOTEL_ADMIN activo | SUPERADMIN activo |
|---|---|---|---|
| users | Perfil propio, sin cambiar rol/active/asignación | Perfil propio con mismas restricciones | Gestión administrativa autorizada |
| hotels | Catálogo permitido | Hotel asignado; solo campos permitidos | Alta, edición, estado y asignación |
| rooms | Inventario permitido | Gestión del hotel asignado | Consulta administrativa |
| reservations | Solo propias y transiciones autorizadas | Solo su hotel y transiciones autorizadas | Consulta de reportes globales |
| logs | Sin consulta global ni modificación | Solo operaciones que se autoricen | Consulta y anexado validado |

TAXISTA no recibe acceso general a users/hotels/reservations por el hecho de iniciar
sesión; sus permisos se definirán para los servicios de taxi. La gestión del sistema
web de taxistas sigue pasando por su API REST: este contrato no la sustituye.

## Cómo verificar los siguientes pasos

1. Auth: correo/contraseña correctos y documento users del mismo UID; rechazar
   usuario inactivo, rol inexistente y autopromoción de rol.
2. Catálogo: editar un hotel y observarlo desde otra sesión cuando ambos repositorios
   estén conectados; no basta comprobar la lista local del mismo teléfono.
3. Usuarios: registro cliente crea perfil remoto y aparece una sola vez en superadmin.
4. Asignaciones: UID y hotelId consistentes y sin asignación doble al mismo administrador.
5. Reglas: probar lecturas y escrituras permitidas y denegadas por rol en un entorno
   de pruebas antes de publicar en el proyecto compartido.
6. Reportes: mostrar reservas reales, sin mezclar las de demostración.

Son criterios de aceptación para la integración completa. Los pasos 3 a 5 tienen
pruebas locales de contrato, reglas y rechazo del acceso sin cuenta; no se declara
superado el recorrido remoto de aceptación. Las guías de pasos 3/4 y 5 detallan sus límites.
