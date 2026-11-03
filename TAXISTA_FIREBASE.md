# Acceso del taxista con Firebase

## Nota para la integración en feat/hotel-admin-firebase

El login compartido de esta rama resuelve los cuatro roles desde `users/{UID}`,
mediante `FirebaseProfileRepository`. Un taxista requiere allí `role: TAXISTA`
y `active: true`, además de su documento operativo `taxistas/{UID}` descrito abajo.
Solo después de resolver el rol TAXISTA se verifica ese documento operativo;
si no existe, está inactivo o falla su consulta se rechaza el acceso.
Admin usa `role: HOTEL_ADMIN` y un `hotelId` válido; superadmin usa `SUPERADMIN`.
No se deduce CLIENT por ausencia de perfil, ni se sustituye el rol de admin por
una consulta a `taxistas`. La restauración de sesión verifica `users` en el servidor.
Las descripciones del login exclusivo de la rama de origen que siguen son históricas;
la disponibilidad, solicitudes y viajes mantienen el contrato operativo de esta guía.
El registro del UID taxista en preferencias solo selecciona su almacenamiento remoto;
no autoriza el acceso a las pantallas ni reemplaza la comprobación de `users`.

## Configuración

- Crear la cuenta de correo y contraseña en Authentication del proyecto compartido.
- Crear `taxistas/{UID}` en Firestore usando exactamente el UID de Authentication.
- Campos: `nombre` (string), `activo` (boolean, true), `disponible` (boolean, false).
- Publicar el bloque de lectura de `taxistas` incluido en `firestore.rules` junto con las reglas existentes.

## Comportamiento del login

El correo y la contraseña se validan con Firebase Authentication. Después se consulta
el documento propio en Firestore desde el servidor. Un documento existente y activo
abre el panel taxista; la ausencia de documento mantiene el flujo del cliente.
Un perfil inactivo o un error de consulta impide continuar e informa del problema.

El rol resuelto se conserva localmente asociado al UID para restaurar las pantallas
del taxista si Android recrea el proceso. Al abrir el login con una sesión Firebase
identificada previamente como taxista se vuelve a consultar el perfil remoto. Cerrar sesión limpia ese registro
y cierra Firebase Authentication. La navegación local no sustituye las reglas de acceso.

El nombre de inicio y el correo del perfil proceden de la cuenta autenticada.
Las cuentas locales de demostración conservan su acceso anterior. Para probar
Firebase se utiliza la cuenta nueva creada en Authentication.

## Comprobación manual en Android Studio

1. Ejecutar `app` en un teléfono o emulador con conexión a Internet.
2. Si hay otra sesión iniciada, cerrarla desde Perfil.
3. Entrar con el correo y la contraseña creados en Firebase.
4. Comprobar que se abre el panel taxista y que Inicio muestra el nombre de Firestore.
5. Cerrar y abrir la aplicación: comprobar que mantiene la cuenta taxista.
6. Cerrar sesión: comprobar que vuelve al login.
7. Cambiar `activo` a false en la consola y volver a intentar el acceso: debe rechazarlo.
   Restaurar true al terminar.
8. Comprobar también el acceso con una cuenta cliente que no tenga documento en `taxistas`.

## Disponibilidad en Firestore

Inicio y Perfil observan `taxistas/{UID}` y comparten el mismo estado. Se exige una
lectura confirmada por el servidor, `activo: true` y un campo `disponible` booleano.
Mientras carga o guarda, el interruptor queda bloqueado y no permite aceptar nuevas
solicitudes. Un cambio utiliza una transacción: verifica el perfil activo y actualiza
únicamente `disponible`. No deja escrituras pendientes para enviarlas luego sin conexión.

Los errores se muestran debajo del interruptor y conservan el último valor confirmado.
Un error de conexión o permisos deshabilita la aceptación hasta volver a verificar el
perfil. Tras publicar reglas nuevas, cerrar y abrir el panel para renovar la suscripción.
Cambiar la disponibilidad no interrumpe un viaje ya aceptado.

La preferencia local refleja el valor confirmado para los flujos existentes de
aceptación y avisos. La confirmación se invalida al reiniciar el proceso; un valor
viejo no habilita la aceptación de servicios al abrir de nuevo la aplicación.
Las cuentas de demostración conservan su disponibilidad local.

### Regla que se debe publicar

En Firebase Console, Firestore > Reglas, reemplazar SOLO el bloque `taxistas` por:

```javascript
match /taxistas/{uid} {
  allow get: if request.auth != null
    && request.auth.uid == uid;
  allow update: if request.auth != null
    && request.auth.uid == uid
    && resource.data.activo == true
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['disponible'])
    && request.resource.data.disponible is bool;
}
```

Publicar conservando los demás bloques del equipo. El archivo `firestore.rules`
contiene esta misma ampliación; editarlo localmente no publica los permisos.

### Comprobación manual de disponibilidad

1. Publicar la regla y ejecutar la versión actualizada desde Android Studio.
2. Entrar con la cuenta Firebase de taxista. Si Firestore tiene `disponible: false`,
   los interruptores de Inicio y Perfil deben aparecer apagados.
3. Activar desde Inicio. Comprobar `disponible: true` en la consola y el interruptor
   encendido al ir a Perfil. Desactivarlo desde Perfil y comprobar lo mismo a la inversa.
4. Cerrar y reabrir la aplicación: debe recuperar el último valor del servidor.
5. Cambiar el booleano desde la consola con la app abierta: ambos interruptores deben
   reflejarlo. Solo las lecturas y escrituras de la app se rigen por Security Rules;
   las ediciones administrativas de la consola no prueban esas restricciones.
6. Desconectar Internet: debe informar de la conexión al detectarla y bloquear el
   interruptor y las nuevas aceptaciones. Reconectar y esperar una lectura del servidor.
7. Poner `activo: false` desde la consola durante la sesión: debe bloquear el interruptor
   y las nuevas aceptaciones. Restaurar true al terminar.
8. En la Zona de pruebas de reglas comprobar: actualización booleana propia permitida;
   actualización de otro UID, de `activo`, de `nombre`, valor no booleano, creación
   o borrado de perfil denegados. El perfil de prueba debe existir y estar activo.

## Solicitudes y viajes

La integración posterior conecta solicitudes, aceptación, estados e historial con
Firestore para cuentas Firebase. El contrato y las pruebas están en
`TAXI_FIREBASE_CONTRATO.md`. Requiere publicar los bloques nuevos de reglas.
Las cuentas locales de demostración conservan Room. Chat, avisos y fotos siguen
siendo locales; GPS y lector QR reales continúan pendientes.

## Puntos compartidos para integrar con el equipo

La implementación nueva está en `data/taxista`. El login común añade una consulta
a `taxistas/{UID}` después de autenticar con Firebase para decidir el destino.
Una cuenta sin perfil taxista sigue entrando como cliente. Esta consulta requiere
la regla de lectura del documento propio también cuando el documento no existe.
Si la consulta falla por red o permisos, se muestra un error y se permite reintentar.
No se infiere el rol cliente de un error de permisos.

`SessionRepository` llama al módulo taxista para guardar, restaurar y limpiar
únicamente su rol. `TaxiRepository` incorpora el puente remoto de solicitudes para
cuentas Firebase, manteniendo sus interfaces y su recorrido local de demostración.
El registro, recuperación de contraseña, reservas y módulos administrativos
conservan su implementación anterior.
