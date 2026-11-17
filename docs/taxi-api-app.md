# Integración de la app con la API de taxis

Rama local: `feat/taxi-api-app`, basada en `origin/main` (`c3b16eb`).
Contrato: `docs/api-taxis-v1.yaml`, sin modificaciones.

## Implementación

- `data/taxiapi/TaxiApi`: las 15 operaciones usadas por cliente/taxista y el catálogo/health. Las dos operaciones exclusivas de superadmin quedan para su responsable.
- `HttpTaxiApi`: HttpURLConnection, JSON UTF-8, Bearer Firebase, un reintento con token renovado ante 401, plazo total de 10 segundos, sin redirecciones ni tráfico HTTP en la app. El HTTP loopback solo está habilitado en el constructor de pruebas JVM.
- `FakeTaxiApi`: almacén compartido en memoria, identidad por cuenta, roles, relación con la reserva, cinco aeropuertos, estados, conflictos, aceptación y transiciones idempotentes, exclusión de servicio activo, QR aleatorio y calificación única. Cada cuenta TAXISTA de prueba recibe un perfil aprobado simulado. Esto no comprueba su aprobación real en MySQL.
- Los repositorios de cliente y conductor usan la API. `FirebaseTaxiStore` se retiró. No quedan accesos a las tres colecciones de taxi en `app/src/main`.
- Consultas del cliente cada 5 segundos y del conductor cada 10, ligadas a pantallas reanudadas. Se detiene la programación al pausarlas; una petición que ya está en curso puede terminar.
- Perfil, disponibilidad e historial del taxista proceden de la API. Room conserva datos auxiliares del conductor; no sustituye a la API para aceptar ni avanzar.
- Solicitud: catálogo de aeropuertos filtrado por ciudad normalizada del hotel. No se inventa un aeropuerto si no hay correspondencia.
- Seguimiento: osmdroid, marcador del conductor, vehículo, valoración y hora de última ubicación en Lima. Se utiliza la URL canónica de OSM, identificación estable de IoTelito y caché de al menos siete días, además de atribución visible. Sin descarga masiva ni mapas offline.
- Ubicación: servicio de primer plano con notificación y permiso de ubicación; consulta FusedLocation de alta precisión y envío cada 15 segundos durante el viaje. Se detiene al finalizar, cerrar sesión o recibir 409/401/403/404. El usuario puede continuar atendiendo el viaje si no concede ubicación, con un aviso.
- QR: ZXing dibuja el código de la API, sin persistirlo. Google Code Scanner permite leerlo sin permiso de cámara; la entrada manual acepta el código completo del mismo QR. Se retiró el código fijo predecible. La finalización siempre se valida mediante la API.
- Calificación: guarda estrellas y comentario opcional, muestra el conductor/vehículo del servicio y evita enviar mientras ya hay una operación pendiente.

## Configuración local

En `local.properties` (ignorado por Git):

```properties
taxiApi.url=https://HOST-DEL-BACKEND/api/v1
taxiApi.fake=false
```

Sin esa configuración, debug usa la API falsa y muestra un aviso permanente de prueba. Release desactiva siempre la API falsa. La URL queda vacía hasta que el compañero comunique la real. Una base vacía o inválida produce el aviso «El servicio de taxis no está disponible», sin cerrar la app.

Para las comprobaciones automatizadas pueden pasarse `-PtaxiApi.fake=false` y `-PtaxiApi.url=...`; tienen prioridad sobre las propiedades locales. No se incluyen direcciones de despliegue ni credenciales reales en fuentes o commits.

La API falsa no necesita un servidor de taxis, pero el login y la lectura de reservas siguen usando la configuración Firebase de la instalación. No crea cuentas ni reservas. Un recorrido manual requiere una cuenta cliente con reserva y una cuenta con rol TAXISTA. Cambiar entre esas cuentas dentro del mismo proceso permite atender el mismo pedido simulado.

Al cerrar completamente el proceso se pierde el almacén simulado. Dos teléfonos no comparten ese almacén. No presentar estos resultados como prueba del backend real ni como migración de viajes antiguos.

## Verificación realizada

- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`: 144 pruebas JVM correctas y APK de depuración generado.
- 25 pruebas nuevas: 11 de HTTP contra el HttpServer del JDK y 14 del simulador. Incluyen Bearer/cuerpo JSON, reintento 401 único, errores 409, timeout, falta de red, 204, JSON inválido, rechazo de HTTP/redirecciones, concurrencia, estados, QR y calificación.
- `assembleDebugAndroidTest`: correcto. La prueba histórica `FirebaseLiveRolesTest` dejó de invocar el almacén Firebase taxi retirado; conserva las operaciones de hotel/reservas y comprueba el perfil taxista mediante la API. No se ejecutó contra cuentas reales.
- `TaxiApiEmulatorTest`, habilitada únicamente con `taxiApiEmulator=true`, en un AVD temporal Android 14 y un Firebase **demo local**: cliente solicita, conductor acepta y avanza, FusedLocation obtiene la coordenada inyectada del AVD y la transmite a la API falsa, cliente consulta mapa/QR, conductor valida el código y cliente califica. Recorrido correcto.
- Segunda variante Android con API falsa desactivada y base HTTP inválida: aviso de indisponibilidad y reserva todavía accesible. Correcta. Se volvió a compilar con la configuración debug predeterminada después de probarla.
- Capturas de solicitud, mapa y calificación inspeccionadas. Se corrigieron los datos estáticos de la calificación y la configuración del mapa después de la revisión visual.
- El recorrido Android usa la entrada manual del código obtenido del QR. La lectura óptica con Google Code Scanner está implementada y compila, pero no se certifica escaneo con cámara física ni descarga del módulo de Play Services en un teléfono nuevo.

## Cómo repetir la prueba Android aislada

Usar exclusivamente un AVD desechable y Auth/Firestore locales del proyecto `demo-iotelito-auth` (puertos 9099 y 8180). La prueba crea sus cuentas y documentos ficticios en esos emuladores. No ejecutar la prueba con datos personales de un dispositivo.

```powershell
adb -s SERIAL-AVD reverse tcp:9099 tcp:9099
adb -s SERIAL-AVD reverse tcp:8180 tcp:8180
adb -s SERIAL-AVD install -r -g app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL-AVD install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL-AVD emu geo fix -77.0298 -12.1219
adb -s SERIAL-AVD shell am instrument -w -e taxiApiEmulator true -e class com.example.proyecto_iotelito.data.TaxiApiEmulatorTest com.example.proyecto_iotelito.test/androidx.test.runner.AndroidJUnitRunner
```

Para obtener GPS en el AVD puede ser necesario repetir `geo fix` mientras la pantalla del servicio permanece abierta.

## Límites y coordinación

Las dudas del contrato están en `api-taxis-dudas.md`. No se cambiaron `firestore.rules`, AWS, el contrato, las pantallas de hotel-admin/superadmin ni las ramas principales. El único ajuste del login compartido conserva los roles y muestra el mensaje del error operativo taxista de la API.

El compañero debe aportar el backend real, aprobar/vincular cuentas, integrar admin/superadmin y decidir la migración/limpieza de viajes anteriores. La app ya no consulta las colecciones antiguas; no se borraron sus documentos ni se publicaron reglas. No se importó el SQL ni se desplegó infraestructura.

Los mapas de las pantallas de indicaciones del conductor siguen siendo ilustrativos; el mapa conectado solicitado en este alcance es el del cliente. No se implementó navegación vial paso a paso ni chat remoto taxista/pasajero. Fotos y documentos se administran en la web: el contrato móvil no permite editarlos.

Referencias contrastadas: [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner), [servicios de ubicación Android](https://developer.android.com/about/versions/14/changes/fgs-types-required), [política de mapas OSM](https://operations.osmfoundation.org/policies/tiles/), [osmdroid](https://github.com/osmdroid/osmdroid/releases), [ZXing](https://github.com/zxing/zxing/releases).
