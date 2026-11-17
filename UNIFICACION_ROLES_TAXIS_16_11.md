# Unificación de Android: cliente, administrador, superadmin y taxista

## Ramas incorporadas

- Base propia: superadmin 66178f2 (validaciones, filtros, reportes, menú y teclado).
- Cliente: origin/feature/inicio-cliente-reserva 57718db, incluido el main c3b16eb y sus fotos de servicios.
- Taxi Android: origin/feat/taxi-api-app 86136bf.
- La rama feat/taxi-api-web contiene otro proyecto Spring/MySQL; no se mezcla con los archivos Android ni se despliega desde aquí.

Se conservaron el layout/altura/color del menú de superadmin, el selector de distritos y las validaciones. Los conflictos de taxis se resolvieron usando la API para solicitar, consultar, aceptar, avanzar, enviar ubicación, verificar QR y calificar. Se conservaron FCM y los ajustes de reservas consecutivas del cliente. Las declaraciones de ambos servicios Android están presentes en el Manifest.

Los temporales, discos de emulador y logs que venían versionados en taxi-api-app se excluyeron del resultado del merge. La carpeta tmp queda ignorada. No se borraron datos de Firebase, MySQL ni archivos del celular.

## Superadmin

En Usuarios aparece Gestionar taxistas. Permite filtrar solicitudes, listar conductores y activar/desactivar mediante GET /taxistas y PATCH /taxistas/{id}/activo del contrato. La desactivación pide un motivo de 5–500 caracteres y las solicitudes pendientes/rechazadas no pueden activarse desde la app. La revisión/aprobación de documentos continúa en la web.

El detalle de un usuario taxista conduce a esa gestión en vez de actualizar solo users.active. El repositorio de usuarios rechaza esa vía separada para taxistas. Los demás roles conservan el cambio de estado anterior. La sincronización real de MySQL y Firebase la debe resolver el backend de ese endpoint; no se simula con dos escrituras independientes del celular.

En modo de prueba, los taxistas aparecen en la lista después de iniciar sesión con su rol dentro de la misma instalación. Activarlos/desactivarlos modifica solo el almacén de prueba. Sus documentos y fechas son ficticios; no altera cuentas reales.

## Cliente y taxis

Mis taxis consulta el servicio de la API asociado a cada reserva propia. No mezcla IDs con taxiRequests antiguos de Firestore. Es una consulta periódica solo mientras esa vista está activa; con muchas reservas deberá añadirse un endpoint paginado de servicios del cliente para evitar consultar uno por reserva.

El beneficio de traslado conserva el requisito de estadía de al menos S/ 1000 y checkout solicitado/finalizado. Se mantiene la selección de aeropuertos que corresponden a la ciudad del hotel. Las reservas, habitaciones, hoteles, fotos AWS, mensajes de hotel y comprobantes siguen en sus servicios actuales.

### Límite del chat de traslado

La API de taxis actual no incluye rutas de mensajes. El chat de traslado de la nueva rama cliente dependía de taxiRequests/messages, colección que la migración deja de actualizar. No se enlazan esos documentos por coincidencia de ID: podrían pertenecer a un viaje anterior. La pantalla muestra que la mensajería no está disponible y sugiere coordinar con recepción; no genera respuestas automáticas ni muestra mensajes de otro servicio. El chat cliente–hotel no cambia.

Para recuperar ese chat hay que ampliar el contrato/backend con autorización de participantes o crear desde el servidor una relación segura entre el viaje API y el documento de conversación. También debe adaptarse el emisor AWS de notificaciones de taxi, que actualmente observa taxiRequests. Las notificaciones de reservas y checkout conservan su implementación; no se desplegó el emisor ni se habilitó EventBridge.

## Prueba en el celular antes de tener URL

1. Abre el proyecto IoTelito en Android Studio, selecciona app y tu dispositivo físico, y pulsa Run. Mantén la instalación existente; no hace falta borrar datos.
2. Entra como superadmin: revisa menú, registro/edición de hoteles, selector de distritos, reportes y Usuarios → Gestionar taxistas.
3. Prueba cliente y administrador con el mismo hotel: habitaciones, reserva, solicitud de checkout, cobro registrado por el administrador y comprobante del cliente.
4. Para taxis, usa una reserva que cumpla el beneficio. Solicita el viaje como cliente, cambia a taxista en la misma instalación/proceso, acepta y avanza; vuelve a cliente para mapa/QR; valida el QR como conductor y vuelve al cliente para calificar. No fuerces el cierre del proceso entre cuentas: el almacén de prueba vive en memoria. La obtención de GPS, permisos y escáner óptico deben comprobarse en el celular real.
5. Luego entra como superadmin y comprueba el taxista de prueba y sus cambios de habilitación.

La franja de modo de prueba debe seguir visible. Dos celulares no comparten ese almacén. No presentar este recorrido como prueba del backend real, de FCM remoto o de sincronización de taxis entre equipos.

## Cuando llegue la URL HTTPS

En local.properties (ignorado por Git):

```properties
taxiApi.url=https://HOST-REAL/api/v1
taxiApi.fake=false
```

Sincroniza Gradle y reinstala con Run. La URL no es una contraseña; no hacen falta claves privadas AWS/Firebase en el APK. El backend debe estar desplegado, aceptar el proyecto Firebase iot-telito y tener cada conductor vinculado/aprobado en MySQL. Verificar dos teléfonos, permisos por rol, activación/desactivación y consistencia del servicio en curso. No se modificó la configuración local del usuario para inventar una URL.

## Aislamiento de las comprobaciones

Las pruebas usan exclusivamente demo-iotelito-auth en localhost y un AVD, con API falsa. En debug FCM no se inicializa antes de la sesión; la sesión cliente real lo habilita. Los proyectos demo omiten el registro FCM porque Firebase Messaging no ofrece emulador. Los mensajes se siguen validando por UID y vínculo de dispositivo. No se instalaron pruebas ni se borraron datos en el teléfono físico.

## Resultados de verificación

- assembleDebug, testDebugUnitTest y assembleDebugAndroidTest correctos; 171 pruebas unitarias sin fallos. Incluyen rutas/cuerpos de las operaciones de superadmin, rechazo de otros roles, filtros y habilitación en la API falsa.
- Reglas locales: autenticación/usuarios (38 comprobaciones), reservas consecutivas (14 y concurrencia), dispositivos de notificación (13), administración de hotel (50 y concurrencia), checkout cliente (30). Cada suite reinicia solo el proyecto demo local; no se desplegaron reglas.
- TaxiApiEmulatorTest pasó en 39,664 s: cliente solicita; conductor acepta, avanza y transmite ubicación ficticia del AVD; cliente consulta mapa/QR; se decodifica el QR generado y se valida mediante entrada manual en el conductor; cliente califica; superadmin lista y modifica la habilitación del taxista de prueba y la pantalla refleja el cambio. Se comprobó que Android admite PATCH sin hacer peticiones a un servidor externo. No equivale a verificar el endpoint remoto.
- Captura superadmin-taxistas-integrado.png inspeccionada. Las pruebas anteriores detectaron incompatibilidades de arranque FCM/red local del entorno demo; se corrigieron antes del recorrido exitoso. El escaneo óptico con cámara, FCM remoto, la web y la comunicación entre teléfonos quedan pendientes.
