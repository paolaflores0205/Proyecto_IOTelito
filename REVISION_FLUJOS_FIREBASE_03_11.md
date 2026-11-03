# Revisión independiente de flujos Firebase

Rama `superadmin`, commit `e97fe2a`. Revisión del 03/11/2026.

## Comprobaciones ejecutadas

- Compilación de app y APK de instrumentación: correcta.
- 68 pruebas JVM: 0 fallos, 0 errores.
- Cinco baterías de reglas sobre Firestore efímero y proyectos demo:
  - Autenticación y usuarios: 38 comprobaciones.
  - Hoteles y asignaciones: 38 comprobaciones y dos de concurrencia.
  - Administrador de hotel: 31 comprobaciones y su caso concurrente.
  - Reportes y logs: 15 comprobaciones.
  - Compatibilidad taxi: 21 comprobaciones.
  Todas pasaron. Cada batería también repite las comprobaciones base de autenticación;
  las cifras de la lista evitan contarlas varias veces.

Las pruebas de reglas verifican permisos y operaciones permitidas/denegadas con
perfiles ficticios. No prueban todas las pantallas Android ni las reglas desplegadas
actualmente en la nube.

## Pruebas Android en un dispositivo aislado

Se creó un dispositivo temporal con datos vacíos, distinto del emulador del usuario.
Se ejecutaron 18 casos seleccionados: 17 aprobados y uno fallido. El dispositivo
temporal se cerró al terminar. No se copiaron las sesiones del usuario.

Clases ejecutadas:

- `SuperadminAccessTest`: 3 casos aprobados; rechazo sin identidad verificada y sin
  sesión Firebase. No equivale al acceso positivo con una cuenta superadmin real.
- `FirebaseHotelsAccessTest`: 3 casos aprobados; rechazo de lecturas/escrituras y
  asignaciones sin sesión, sin sustituir datos remotos por ejemplos de Room.
- `HotelAdminInsetsTest`: 3 casos aprobados; menú y teclado con fixture local.
- `HotelAdminSharedProfileUiTest`: 1 caso aprobado; perfil compartido local.
- `ConductorStorageTest`: 5 casos aprobados; persistencia, estados, cuenta y conexión
  del pedido local entre cliente y conductor.
- `TaxistaUiTest`: 2 casos aprobados; recorrido, filtros, avisos y fotografía local.
- `ClientNavigationTest`: 1 caso fallido.

## Fallo del test cliente

`menuClienteQuedaEncimaDeLaBarraDelSistema` falla en la comprobación de insets
de `ClientNavigationTest.java:39`, antes de verificar el menú.

La prueba guarda una identidad CLIENT local sin UID. `SessionRepository.obtener`
solo conserva fixtures locales de HOTEL_ADMIN y TAXISTA y exige una sesión Firebase
para CLIENT. `MainActivity` redirige al login cuando no recibe el cliente verificado.
Esto hace incompatible el fixture del test con el acceso integrado y explica por
qué no se valida la pantalla esperada. Se debe adaptar la prueba para autenticar un
cliente de prueba, no permitir identidades no verificadas en producción.

El fallo no demuestra por sí solo que el login de un cliente real esté roto; tampoco
permite declarar aprobado su recorrido visual. No se cambió el guard ni el test.

## Cobertura pendiente

- Accesos positivos con cuentas reales de cliente, administrador y superadmin en esta
  pasada; no se proporcionaron credenciales ni se reutilizaron sesiones del usuario.
- Recorrido completo Android de cliente, menús remotos de administrador/superadmin,
  subida de fotos a Storage, notificaciones con la app cerrada, GPS y QR reales.
- La rama documenta como pendientes las reservas, cobro, mensajes y reportes del
  administrador de hotel para sesiones Firebase. No afirmar que todos los flujos
  de ese rol están conectados.

`RESULTADOS_PRUEBAS_REALES_03_11.md` contiene evidencia previa de otra pasada con
cuentas reales. No se contó como prueba repetida durante esta revisión.

## Estado del proyecto y de Firebase

Durante la pasada inicial no se modificó código de la app, cuentas, reglas publicadas,
documentos remotos, roles, asignaciones ni facturación. Las escrituras de prueba se
realizaron en Firestore local o en el dispositivo Android temporal.

## Comprobación manual posterior y corrección del catálogo

El usuario confirmó con una cuenta cliente real que carga su panel, el catálogo,
sus reservas y los detalles de hotel y reserva. Al volver al catálogo aparecía un
aviso de actualización fallida. Esto no forma parte del resultado de instrumentación.

Se encontró que `FirebaseHotelsRepository.observe` consideraba error cualquier
snapshot inicial de caché. Al reactivar la pantalla Firestore puede enviarlo antes
de la confirmación del servidor. Se separó esa situación como carga en espera;
se mantienen los errores reales y no se toma la caché como confirmación remota.

Se añadieron cuatro pruebas de regresión: caché inicial, servidor confirmado,
error real con caché y respuesta ausente. Compilación correcta y 72 pruebas JVM
aprobadas después de la corrección. El cambio es local en el repositorio y no
publica reglas ni modifica datos Firebase. Falta que el usuario repita el regreso
desde el detalle con el APK actualizado para cerrar esta comprobación visual.

Ante una segunda captura del aviso se comparó el APK instalado con el compilado:
la instalación de las 17:01:17 aún carecía de la corrección de caché, mientras el
APK local actualizado sí la contenía. Se instaló ese APK en emulator-5554 con
`adb install -r`, conservando los datos de la app. Falta repetir la comprobación
visual con esta instalación actualizada; la segunda captura no correspondía al fix.

### Instalación que volvía a quedar desactualizada

Después de otra ejecución desde Android Studio, el APK instalado volvió a no
contener la corrección. Se deshabilitó el init script local de Gradle que redirigía
las salidas a otra carpeta y se recompiló en `app/build`, la ruta convencional del
proyecto. Esa configuración local no formaba parte de Git ni de Firebase.

Compilación y 72 pruebas JVM correctas. Se instaló el APK de `app/build` conservando
los datos y se recorrió la cuenta cliente ya abierta: dos aperturas/regresos del
detalle de Barranco Art Boutique y una apertura/regreso del detalle de reserva.
Se comprobó la cola de avisos tras cada regreso: el mensaje de hoteles no se detectó
en esos tres recorridos. No se confirmaron reservas ni pagos. El arranque normal
puede actualizar las marcas de último acceso previstas por la aplicación.

Esta verificación cubre esos recorridos de lectura del cliente. Los accesos positivos
de administrador y superadmin con sus cuentas reales siguen pendientes en esta pasada.


## Conexión posterior de las pestañas del administrador

Se prepararon las consultas de reservas del hotel, el chat cliente/admin y los reportes remotos, con cierre administrativo transaccional y comprobante inmutable. Los pasos de publicación, contratos, límites y prueba manual están en `FIREBASE_ADMIN_OPERACIONES_03_11.md`. Esta preparación se validó con datos ficticios locales; no equivale a una nueva prueba con las cuentas reales ni publica reglas en el proyecto del equipo.
