# Firebase superadmin: autenticación y usuarios (pasos 3 y 4)

Fecha: 02/11/2026. Rama: superadmin. No se hizo push ni se publicaron cambios en Firebase.

Nota de continuidad: `GUIA_FIREBASE_HOTELES_PASO_5.md` documenta la integración
posterior de hoteles y asignaciones. Sus cambios y reglas sustituyen las
referencias de esta guía a hoteles todavía locales o con escrituras cerradas.
La preparación de cuentas y perfiles de los pasos 3 y 4 sigue siendo necesaria.

## Resultado y requisito antes de ejecutar

El código usa Firebase Authentication para el login y users/{uid} para resolver rol
y estado. Usuarios, su detalle y la activación/desactivación usan Firestore.

**Antes de usarlo contra la nube debes preparar la cuenta inicial, los documentos
users y publicar las reglas de este repositorio en el Firebase compartido.**
Tener google-services.json no concede permisos de administración de la consola.
Esta configuración remota no se realizó automáticamente ni se usaron contraseñas
reales durante las pruebas.

Los correos y contraseñas de demostración ya no permiten entrar por el login.
Esto incluye admin y taxista: sus cuentas reales también necesitan un documento
users válido. Sus pantallas y datos locales permanecen; no se borró Room.

## A. Crear la primera cuenta superadmin en la consola

Hazlo con la persona que tenga permiso de administración del Firebase del equipo.

1. Abre Firebase Console y selecciona **iot-telito**, el mismo proyecto del cliente.
   No crees un proyecto independiente para tu rol.
2. Ve a **Authentication > Sign-in method** y confirma que **Email/Password** esté habilitado.
   No necesitas Google Sign-In ni FirebaseUI para este login de correo/contraseña.
3. En **Authentication > Users > Add user**, crea tu cuenta con un correo tuyo y una
   contraseña privada. No la pongas en el código, documentos, capturas ni GitHub.
   Si ya existe la cuenta, reutilízala; no crees otra con un UID distinto.
4. Copia el **UID** de esa cuenta. No es el correo, DNI ni ID numérico del usuario demo.
5. Ve a **Firestore Database > Data**, base **(default)**.
6. Crea la colección **users** si no existe. Crea un documento con **Document ID = UID copiado**.
7. Agrega estos campos y tipos desde la consola:

| Campo | Tipo | Valor |
|---|---|---|
| uid | string | Exactamente el mismo UID del documento y Authentication |
| name | string | Tu nombre para mostrar |
| email | string | El correo de la cuenta de Authentication |
| role | string | SUPERADMIN, exactamente en mayúsculas |
| active | boolean | true, no el texto "true" |
| hotelId | null | Sin hotel asignado |
| createdAt | timestamp | Fecha/hora actual |
| updatedAt | timestamp | Fecha/hora actual |

Opcionales: phone y documentNumber como strings. lastAccessAt se registra al ingresar.
No guardes password ni passwordHash. La aplicación no crea ni promueve superadmins
desde el registro público: esa asignación inicial requiere al operador de consola.

## B. Preparar las cuentas ya existentes del equipo

**Coordina esto antes de publicar las nuevas reglas.** Las cuentas de clientes que tu
compañera creó antes podían existir solo en Authentication y datos del teléfono.
Ahora un perfil inexistente se rechaza: no se le inventa automáticamente un rol.

1. Para cada cuenta de pruebas existente, copia su UID en Authentication.
2. Crea users/{uid} con uid, name, email, role y active; usa timestamps para auditoría.
3. Para clientes usa CLIENT. Para las cuentas reales de tus compañeros usa HOTEL_ADMIN
   o TAXISTA según corresponda. No asignes SUPERADMIN a todos para evitar errores.
4. Los documentos deben tener el UID exacto y active booleano. Un perfil mal formado
   se rechaza al entrar y se informa como incompleto al listar.
5. Los administradores y taxistas se provisionan manualmente en esta etapa; la app
   todavía no crea cuentas administrativas nuevas. No confundas el alta de Auth con
   crear una fila local en Room.

Los nuevos registros desde la app sí crean Auth y su documento CLIENT/active true.
Si se crea Auth pero falla la escritura del perfil por reglas o conexión, se informa
el resultado parcial y se cierra sesión. Repara users/{uid} de esa cuenta existente;
no intentes volver a crear el mismo correo ni le concedas permisos administrativos.

## C. Publicar las reglas de manera coordinada

1. Abre **Firestore Database > Rules** en el proyecto compartido.
2. Guarda una copia de las reglas actualmente publicadas y compáralas con el equipo.
   Si alguien cambió la consola sin subirlo a GitHub, no lo sobrescribas a ciegas.
3. Revisa **firestore.rules** de esta rama. Conserva las reglas de reservas y bloqueos
   del cliente y añade users con permisos por rol/estado.
4. Una vez preparados los perfiles existentes, publica la versión revisada.
5. No uses allow read, write: if true ni deshabilites protecciones para que la demo funcione.

Las reglas bloquean la autopromoción, autoreactivación, eliminación de perfiles y
cambio del estado de cuentas SUPERADMIN desde la app. Solo un superadmin activo
puede activar/desactivar CLIENT, HOTEL_ADMIN o TAXISTA, sin modificar rol o UID.
Las escrituras remotas de hoteles siguen cerradas: se habilitarán en el paso 5.

active false no deshabilita administrativamente la cuenta en Firebase Authentication.
Sí impide el login funcional y el acceso Firestore protegido. La app observa cambios
del perfil y cierra la sesión cuando recibe desactivación o cambio de rol. Las reglas
rechazan operaciones aunque el dispositivo conserve un token anterior; la recepción
visual del cambio depende de que el dispositivo vuelva a tener conexión.

El estado del taxista en el sistema web y su API no se actualizan con este cambio;
deberán integrarse de forma coordinada, sin acceder directamente a su base de datos.

## D. Probar en Android Studio y el celular

1. Abre IoTelito, rama superadmin, sincroniza Gradle y ejecuta app sin desinstalarla.
2. Usa tu correo/contraseña reales de Authentication. El login consulta users en el
   servidor, no se autoriza con caché ni por una coincidencia de correo o nombre.
3. Se abre el rol correspondiente. Tras reiniciar el proceso, la sesión Firebase se
   vuelve a resolver contra el documento remoto, sin asumir que todos son CLIENT.
4. En superadmin abre **Usuarios**. Ahora son perfiles reales de Firestore, no los
   16 ejemplos locales. Estos ejemplos siguen en Room, pero no se mezclan en esa lista.
5. Prueba nombre, correo o documento y filtros por rol y estado. Abre un detalle:
   el identificador transmitido es UID, no un entero inventado desde su hash.
6. Desde otra instalación registra un cliente de prueba. Comprueba que aparezca
   automáticamente en Usuarios con el mismo UID de Authentication.
7. Desactiva ese cliente introduciendo un motivo. Debe actualizarse el detalle/lista.
   En el teléfono del cliente, con conexión, debe cerrarse la sesión; un nuevo login
   se rechaza. Reactívalo desde superadmin y comprueba que vuelva a ingresar.
8. Comprueba que el botón de cambiar estado esté deshabilitado para SUPERADMIN.
9. Prueba cerrar sesión y volver a entrar. El perfil de superadmin muestra el nombre
   y correo de la cuenta verificada, no el correo fijo de demostración.
10. Si necesitas recuperar contraseña, usa **Olvidé mi contraseña**: envía una
    solicitud real de recuperación por Firebase. El cambio se realiza con el enlace
    del correo, no escribiendo una contraseña nueva solo por conocer el email.

Los perfiles mal formados se reportan; un error de permisos no se presenta como una
lista vacía exitosa. En modo sin conexión no se muestran perfiles de usuarios desde
caché como si el servidor hubiera confirmado el permiso. Al regresar a una pantalla
superadmin se verifica nuevamente el perfil en el servidor.

## E. Qué cambió en el código y por qué

- RemoteUserProfile: valida UID, rol canónico y active booleano. Se prueba sin Android.
- FirebaseProfileRepository: lee users/{uid} con Source.SERVER; no eleva roles ni crea
  automáticamente perfiles administrativos. Escribe el perfil CLIENT al registrarse.
- LoginActivity y SessionRepository: resuelven el rol y restauran una sesión verificada;
  observan la revocación por cambios del perfil. Se retiró el fallback de credenciales demo.
- SuperadminAccessGuard e IoTelitoApplication: protegen las actividades de superadmin,
  también si se abren por una notificación o intent interno. No cambian los insets ni el menú.
- FirebaseUsersRepository: escucha users, retira listeners al dejar de observar,
  separa carga/error/datos y cambia active mediante una transacción autorizada.
- UsuariosFragment, DetalleUsuarioActivity y SuperadminViewModel: conservan el diseño
  y usan UID para operar sobre perfiles remotos.
- PerfilSuperadminActivity: muestra la identidad verificada.
- RecoverPasswordActivity: usa el correo real de recuperación Firebase, sin comprobar
  cuentas en el catálogo demo. El antiguo cambio local de contraseña ya no se ofrece.
- firestore.rules: define las restricciones reales; ocultar un botón no es autorización.

Los listeners propios del perfil permiten recibir revocaciones durante una sesión.
signOut no equivale a borrar todos los documentos de la caché Firestore; por eso
la lista borra su estado al dejar de observar y no usa caché para mostrar datos protegidos.

## F. Lo que todavía NO pertenece a estos pasos

- Hoteles, asignación de administrador, reportes, logs y avisos administrativos siguen
  siendo locales donde no se haya conectado su repositorio. El menú y catálogo de hoteles
  se conservan. No son ahora estadísticas o logs remotos reales.
- La asignación local de administradores sigue usando exclusivamente el catálogo local:
  no se mezclan sus IDs enteros con los UID nuevos. Se reemplazará en el paso 5.
- No se importaron automáticamente los 16 usuarios de ejemplo como cuentas reales.
- No se creó un usuario real, no se desplegaron reglas y no se probó el servicio remoto.
- No se añadieron FirebaseUI, Google Sign-In, Cloud Functions, Admin SDK ni claves
  de servicio dentro del APK. La verificación de correo vista en clase no se añadió
  como una condición nueva de acceso en este paso.
- No se hizo push. Los cambios se pueden revisar antes de compartirlos con el equipo.

## G. Verificación realizada

- assembleDebug y assembleDebugAndroidTest: correctos.
- testDebugUnitTest: 31 pruebas, 0 fallos/errores (24 existentes + 7 de RemoteUserProfile).
- firestore_auth_rules.test.cjs: 38 comprobaciones REST contra Firestore Emulator,
  proyecto demo-iotelito-auth en 127.0.0.1:8180. Incluyen UID incoherente, rol inválido,
  permisos por rol, estado inactivo, autoreactivación, autopromoción, registro público,
  reserva con bloqueos y checkout del cliente. No usan cuentas ni tokens reales.
- location_picker.test.cjs: 7 comprobaciones correctas.
- SuperadminAccessTest: 3 pruebas Android correctas en Pixel_4 sin cuenta autenticada:
  rechazo de sesión superadmin local, redirección al login y no exposición de usuarios demo.

No se ejecutó toda la suite instrumentada. SuperadminUiTest ahora requiere una sesión
Firebase superadmin en un entorno de pruebas; se elimina la suposición de 16 usuarios
en la lista remota. No se añadió un bypass de seguridad para que las pruebas antiguas
siguieran entrando con datos demo.

No se probaron en la nube el login positivo, el registro, la recuperación por email ni
la lista con datos reales: necesitan la configuración de consola de A-C. Tampoco se
probaron en el A55. Ejecuta el recorrido de D con el equipo antes de presentar.

## H. Repetir las pruebas de reglas sin tocar Firebase real

Usa un emulador Firestore local con firestore.rules cargado y puerto 8180.
Si tienes Node/npm, Firebase CLI y un JDK compatible instalados, puedes iniciarlo
desde la raíz del proyecto en una terminal separada:

```powershell
firebase emulators:start --only firestore --project demo-iotelito-auth --config firebase.local.json
```

firebase.local.json es para emulación; no se usó para desplegar nada en la nube.
El proyecto de pruebas es siempre demo-iotelito-auth. Si editas reglas mientras usas
el JAR directamente, reinícialo para cargarlas antes de repetir las pruebas.

```powershell
node app/src/test/js/firestore_auth_rules.test.cjs
node app/src/test/js/location_picker.test.cjs
```

El script elimina únicamente los documentos del proyecto demo del emulador local
al comenzar. Nunca lo adaptes para apuntar al servicio remoto. El JAR oficial usado
fue cloud-firestore-emulator-v1.22.0, verificado con SHA-256 publicado por firebase-tools.
No se incluye el binario en el repositorio.

Referencias: https://firebase.google.com/docs/firestore/solutions/role-based-access
y https://firebase.google.com/docs/firestore/security/test-rules-emulator.
