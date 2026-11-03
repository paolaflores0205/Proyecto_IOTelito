# Firebase del administrador de hotel

03/11/2026. Rama local `feat/hotel-admin-firebase`.

## Configuración remota verificada

El 03/11/2026 se creó y verificó el perfil de prueba HOTEL_ADMIN, y se asignó
Hotel Miraflores Park (ID 1) actualizando ambos vínculos atómicamente.
Con autorización del usuario se publicaron las reglas de esta rama en
`iot-telito`, base `(default)`, a las 09:09 de Lima. La lectura posterior de la
release confirmó coincidencia exacta con el archivo local probado.

- Ruleset activo: `45c0efb0-ce35-4361-9a65-488f0db23e44`.
- SHA-256: `3588862fab6d37ef4923c1bdda577b9894b3538944fb25137a65adf59de42104`.
- Se conservaron las reglas de cliente, taxi y superadmin.
- El respaldo de reglas anteriores quedó fuera del repositorio, en la carpeta
  local de herramientas `D:/IoTelitoFirebaseTools`.

En Android se verificaron el login de la cuenta real, la lectura del hotel y el
guardado de su descripción. También se creó la habitación `LAB-ADMIN`, se cambió
su precio de 150.50 a 175.75 y se retiró desde la app. Las lecturas posteriores
en Firestore confirmaron los cambios y el UID del admin en el resumen del hotel.
La habitación quedó archivada y el inventario volvió a 7 unidades.
Los servicios de demostración se cargaron mediante la API administrativa y se
comprobó su lectura en la app; ese alta inicial no prueba escrituras desde Android.
No se hizo push a GitHub ni se incluyeron credenciales en el repositorio.

## Datos preparados para la presentación

Se conservaron las 7 habitaciones del ejemplo. La descripción temporal de
validación se sustituyó por la descripción de `HotelAdminSampleData`; se completaron
teléfono y atracciones cuando estaban vacíos. No se sustituyeron otros hoteles.
Los servicios reutilizan los ejemplos existentes del mismo archivo:

| Servicio | Precio S/ | Unidad | Estado |
|---|---:|---|---|
| Desayuno Buffet | 45.00 | persona | Activo |
| Lavandería Express | 35.00 | carga | Activo |
| Traslado Aeropuerto | 80.00 | servicio | Activo |
| Spa & Masajes | 120.00 | sesión | Inactivo |

Son datos de demostración. No se crearon reservas, cobros ni cifras de ocupación
o ingresos para llenar las secciones que todavía no tienen integración remota.

## Alcance implementado

El login compartido autentica por correo/contraseña y consulta `users/{UID}` en
el servidor. Admin requiere `role: HOTEL_ADMIN`, `active: true` y un `hotelId`
entero positivo. La app muestra y edita el hotel asignado, sus habitaciones y sus
servicios adicionales mediante el SDK de Firestore ya incluido por el equipo.
No se agregó MongoDB, Retrofit, un servidor o dependencias nuevas de Firebase.

Los RecyclerView escuchan sus colecciones mientras sus pantallas están activas.
Se retiran las suscripciones al salir. Una lectura de caché no se presenta como
confirmación del servidor y un error no se reemplaza por ejemplos de Room.
Los formularios leen el documento por ID para editar, incluso después de recrear
el proceso; no dependen de una lista estática de otra pantalla.

Los botones Guardar confirman únicamente al completar la operación remota. Las
transacciones verifican el perfil y ambos lados de la asignación del hotel. Si
cambia la cuenta durante la operación, la pantalla no la confirma para otra sesión.
Cambiar rol, desactivar admin o cambiar su hotel provoca cierre de sesión al
recibir la actualización del perfil. Las reglas también rechazan las escrituras
con un perfil inactivo o una asignación inconsistente.

Al editar inventario se consulta primero el hotel y su revisión, después las
habitaciones, y se comprueba esa revisión dentro de la transacción. Si hubo otro
cambio, se pide recargar antes de guardar. Se calcula el número de habitaciones
no retiradas y el menor precio de las disponibles. El resumen es mantenido por la
app: las reglas validan tipos, permisos y avance de revisión, pero no agregan una
colección para demostrar matemáticamente los totales. Las reservas verifican los
documentos de habitación. Las ediciones administrativas desde consola no siguen
este protocolo; cambia el inventario desde la app para mantener sus resúmenes.

Eliminar retira el elemento de la lista con `archived: true`, conservando el
documento para las referencias históricas. Una habitación retirada también queda
`disponible: false`; un servicio, `activo: false`. No se permite reutilizar su ID.
Las imágenes de habitaciones y servicios siguen siendo recursos de demostración.
La galería del hotel lee las fotos compartidas que administra superadmin.

## Preparación en la consola compartida

Estos pasos quedan como referencia para nuevas cuentas o instalaciones. Para el
perfil de prueba y el hotel 1 ya se verificó la configuración descrita arriba.
No necesitas compartir contraseñas, tokens de acceso ni claves privadas.

1. En Authentication de `iot-telito`, reutiliza tu cuenta real si ya existe; en otro
   caso crea una de prueba por correo/contraseña y conserva su contraseña privada.
   Copia su UID para el documento de perfil.
2. Coordina con superadmin la creación de `users/{UID}`. Campos mínimos:
   `uid` texto igual al UID, `name` texto, `email` texto igual al de Auth,
   `role` texto `HOTEL_ADMIN`, `active` booleano `true`, `hotelId` inicialmente
   `null`, y `createdAt`/`updatedAt` timestamps. Admin no podrá ingresar hasta
   que tenga hotel asignado. No uses el correo como ID documental.
3. Si aún no existe la cuenta inicial de superadmin, su responsable debe preparar
   su cuenta Auth y `users/{UID}` con `role: SUPERADMIN`, `active: true`,
   `hotelId: null`, `uid`, `name`, `email` y timestamps. Sigue la guía de pasos 3/4.
4. Prepara también los perfiles de las cuentas de cliente existentes, conservando
   exactamente sus UID, con `role: CLIENT` y `active: true`. Para taxista se requiere
   el perfil `users` con `role: TAXISTA` además del documento operativo `taxistas`.
   Esto evita que el login integrado rechace una cuenta antigua sin perfil.
5. Guarda una copia de las reglas publicadas y revisa con el equipo el archivo
   COMPLETO `firestore.rules` de esta rama. Incluye los permisos anteriores de
   cliente/taxi, los de superadmin y los de admin. Publícalo cuando los perfiles
   estén preparados y el equipo vaya a probar esta versión. No reemplaces las
   reglas por un fragmento ni abras la base para todas las cuentas.
6. Superadmin debe crear el hotel o importar explícitamente los hoteles de ejemplo
   desde su módulo. La importación conserva IDs, usa los recursos de imagen de la
   app y no requiere subir fotografías a Storage. No importa reservas simuladas
   ni crea cuentas administrativas ficticias. Mientras no exista inventario, el
   hotel nuevo no estará disponible para reservar.
7. Asigna tu cuenta al hotel desde superadmin. La transacción existente escribe
   juntos `users/{tuUID}.hotelId` y `hotels/{id}.administratorUid`. No edites solo
   uno de esos lados. Comprueba que el hotel esté activo. El ID puede ser distinto
   de 1; la versión admin nueva utiliza el ID que recibió del perfil.

## Recorrido para validar el laboratorio

1. Instala la app de esta rama e inicia como admin con tu cuenta Firebase.
   El saludo debe mostrar tu nombre y el panel, el nombre de tu hotel asignado.
   Si el perfil o el hotel no están preparados, se informa el problema.
2. Abre Datos del hotel. Cambia descripción o teléfono y guarda con Internet.
   Comprueba el campo en `hotels/{id}`. Reabre el formulario y compara.
3. Abre Habitaciones. Crea una unidad con código, área, capacidad y precio decimal.
   Comprueba `hotels/{id}/rooms/{idHabitacion}` y los campos de resumen del hotel.
4. Edita su precio y disponibilidad. Vuelve a la lista y comprueba que cambió.
   El cliente debe leer el mismo inventario remoto cuando el hotel esté disponible.
5. Retira una habitación creada para la prueba. Desaparece de la lista y permanece
   en Firestore con `archived: true`, `disponible: false`. No uses una habitación
   real ocupada para este recorrido.
6. En Servicios crea, edita y retira un servicio adicional. Verifica su documento
   bajo `hotels/{id}/services`. La unidad se conserva al editar; un alta usa
   `servicio`, como el formulario existente. Los precios admiten céntimos.
7. Cierra por completo y abre la app. Repite las lecturas: deben venir de Firestore.
   Si hay un segundo dispositivo, abre la lista allí y cambia un documento desde
   el primero para verificar la actualización del RecyclerView.
8. Desconecta Internet e intenta guardar: debe informar el error, conservar el
   formulario y evitar un mensaje de éxito. Reconecta y vuelve a intentarlo.
9. Usa Cerrar sesión en el panel. Entrar con contraseña incorrecta falla. Una
   cuenta cliente no debe abrir las pantallas protegidas de admin mediante intents.
10. Para probar revocación con tu compañero, desactiva la cuenta o cambia su hotel
    desde superadmin. Con conexión debe cerrarse la sesión; las escrituras quedan
    rechazadas por reglas aunque una pantalla anterior siga abierta.

## Límites de esta entrega

Reservas, cobro/checkout, mensajes y reportes de admin conservan su implementación
anterior de demostración local, pero no se habilitan para una sesión Firebase.
El panel remoto muestra una raya en las métricas que todavía no consulta, no
importes ficticios. Disponibles representa habitaciones habilitadas, no una
consulta de ocupación por fechas. Las notificaciones de checkout de ejemplo no se
generan en este recorrido remoto. La galería compartida es de consulta.

Las reglas ya están desplegadas y la cuenta de prueba tiene hotel asignado.
El login, los datos del hotel y el alta/edición/retirada de habitaciones se
comprobaron con la cuenta real. El CRUD de servicios en Android y las demás
secciones remotas conservan los límites indicados en esta guía.

## Código para explicar

- `FirebaseHotelAdminRepository`: lecturas, listeners, transacciones y sesión.
- `HotelAdminFirestoreCodec`: tipos y resumen del inventario, probado con JUnit.
- `HotelAdminAccessGuard`: protección de pantallas abiertas por intents.
- `DatosHotelActivity`, formularios, adapters y listas: conservan View Binding y
  RecyclerView; cambian la fuente de datos para cuentas Firebase.
- `firestore.rules`: autorización por perfil activo y asignación del hotel.

Se siguieron los conceptos del material de clase: SDK directo de Authentication,
colecciones/documentos, lecturas `get`, escritura y `addSnapshotListener`. Las
transacciones mantienen consistentes inventario y resumen al editar desde varios
dispositivos. Referencias oficiales consultadas:
https://firebase.google.com/docs/firestore/manage-data/transactions
https://firebase.google.com/docs/firestore/query-data/listen
https://firebase.google.com/docs/firestore/security/rules-conditions

## Pruebas locales

La app, sus pruebas unitarias y el APK de instrumentación se compilan con Gradle.
Las pruebas de reglas nuevas están en
`app/src/test/js/firestore_hoteladmin_rules.test.cjs`; requieren el emulador local
con este archivo de reglas y proyecto `demo-iotelito-auth` en el puerto 8180.
Se ejecutan junto con las suites anteriores de autenticación, hoteles/asignaciones
y reportes/logs. La suite `tools/test_taxi_firestore_rules.py` usa un emulador
efímero separado y perfiles `users` de prueba, sin cuentas reales.
Las pruebas Android antiguas de Room ahora crean una sesión local explícita:
no se debilita el acceso real para permitir abrir pantallas sin login en tests.
