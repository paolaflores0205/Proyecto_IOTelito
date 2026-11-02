# Paso 5: hoteles y asignación de administradores en Firestore

Fecha: 02/11/2026. Rama: superadmin. Implementado y probado localmente.
No se publicaron reglas, importaron datos en Firebase real ni hizo push.

## 1. Qué cambia y por qué

Antes, registrar o editar un hotel modificaba Room en ese teléfono. Ahora las
pantallas de hoteles del superadmin consultan y guardan en Firestore. El cliente
autenticado consulta ese mismo catálogo y sus habitaciones. Así ambos roles
pueden compartir información cuando se configure el Firebase del equipo.

Se conserva el diseño Material, el selector de ubicación, el carrusel, la lógica
del menú inferior y el catálogo local. Room no se borra ni se publica solo al
abrir la aplicación. Los errores remotos muestran un aviso; no se presentan
datos locales como si Firebase hubiera confirmado la operación.

La asignación usa el UID real del administrador, no el número de usuario de
Room ni su nombre. Una transacción cambia juntos `hotels/{id}.administratorUid`
y `users/{uid}.hotelId`, incluyendo la liberación del administrador anterior.
Esto evita que dos hoteles queden asignados al mismo administrador por una
operación incompleta o concurrente.

## 2. Preparación antes de probar con la nube

1. Abre el proyecto IoTelito en Android Studio y sincroniza Gradle.
2. Revisa primero `GUIA_FIREBASE_SUPERADMIN_PASOS_3_4.md`: necesitas una cuenta
   real de Firebase Authentication y su documento `users/{uid}` con el mismo
   UID, `role: SUPERADMIN` y `active: true`. No basta con crear el correo en Auth.
3. Con la persona administradora del Firebase compartido **iot-telito**, revisa
   el archivo completo `firestore.rules` de esta versión. Conserva los permisos
   de usuarios/reservas e incorpora hoteles, asignaciones y contador.
4. En Firebase Console → Firestore Database → Rules, publica esas reglas
   revisadas. Este paso no se hizo automáticamente desde este trabajo.
5. Ejecuta la app con Internet e inicia sesión con la cuenta superadmin real.
   Abre Hoteles. Si aparece un error de permisos, comprueba UID, perfil activo,
   proyecto y reglas; no abras temporalmente la base a todo el mundo.

El archivo de configuración Android ya existente se reutiliza. No se creó otro
proyecto Firebase. No se usaron cuentas ni contraseñas reales para las pruebas.

## 3. Cómo publicar los ejemplos locales, si los necesitas

1. En Hoteles pulsa **Publicar catálogo local de ejemplo**.
2. Lee la confirmación y acepta solo si quieres añadir esos ejemplos al Firebase
   compartido. El botón no se ejecuta automáticamente.
3. Espera el resultado. Se informa cuántos hoteles nuevos se publicaron.
4. En la consola revisa `hotels`: el catálogo de referencia contiene 12 hoteles;
   la importación utiliza la instantánea local existente, que puede variar si
   antes se editaron o añadieron ejemplos. Cada ID antiguo se conserva.
5. Abre un hotel recién importado y su subcolección `rooms`: conserva los IDs de
   unidades físicas del inventario de ejemplo. No son solo tipos de habitación.
6. Si el documento del hotel ya existía, se omite completo: no se reemplazan sus
   campos ni se rellenan sus habitaciones. Si tenía datos incompletos, deben
   revisarse de forma explícita, no repararse sobrescribiendo silenciosamente.
7. Si falla a mitad, puedes repetir. Cada hotel y sus habitaciones se escriben
   juntos, pero toda la lista no es una sola transacción. Los hoteles publicados
   antes del fallo quedan y se omiten en el siguiente intento. Si existen
   habitaciones huérfanas bajo un ID sin documento de hotel, se detiene ese caso.

La importación copia servicios e inventario de ejemplo. Inicializa reservas,
valoraciones y reseñas en cero; no convierte el historial simulado en reservas
reales. No crea cuentas Auth ni asigna los administradores locales por nombre.
Los hoteles quedan sin administrador hasta elegir una cuenta real.

Si una ubicación antigua no tiene coordenadas, se conserva vacía: no se inventa
una ubicación. Al editar, selecciona su ubicación real en el mapa para guardar.

## 4. Registrar y editar un hotel

1. En Hoteles pulsa el botón de registro.
2. Completa nombre, dirección, distrito, ciudad y los demás datos del formulario.
3. Abre el selector de mapa, busca o marca la ubicación y confirma. Verifica la
   dirección que devuelve. Se mantienen coordenadas internamente para ubicarlo;
   no tienes que escribirlas como formulario de latitud/longitud.
4. Añade fotografías si quieres probar el carrusel. Por ahora esas fotos nuevas
   son privadas del teléfono: consulta el apartado de fotos de esta guía.
5. Pulsa Guardar y espera la confirmación del servidor. Ante un fallo no se
   guarda una copia en Room como sustitución de la escritura remota.
6. Abre el detalle y comprueba los datos; luego entra a editar, modifica un campo
   y guarda. La edición actualiza solo campos del formulario y auditoría:
   conserva asignación, inventario y estadísticas existentes.
7. Activa o desactiva el hotel desde su detalle. Se cambia `active`, no se borra
   el documento ni se eliminan reservas anteriores.

Los hoteles creados normalmente reciben IDs desde **1000000**, con un contador
transaccional `system/hotelIds`. No lo crees ni incrementes manualmente para cada
alta: la app lo hace junto con la creación. Si encuentra el ID ocupado, rechaza
el alta en lugar de sobrescribirlo. Los IDs anteriores se reservan al catálogo
importado y no se renumeran.

Un hotel nuevo comienza con `hasInventory: false`, sin habitaciones y sin precio
de reserva. Puede aparecer en el catálogo, pero no se puede reservar hasta que
se publique inventario válido. No se inventan habitaciones para completar el flujo.

## 5. Asignar, sustituir y retirar un administrador

1. Prepara en Auth una cuenta real de administrador y su perfil `users/{uid}`
   con `role: HOTEL_ADMIN`, `active: true` y `hotelId: null`, siguiendo la guía
   de cuentas. Este paso no crea cuentas desde la sesión del superadmin.
2. Abre el detalle del hotel → Asignar administrador.
3. Se muestran cuentas remotas activas del rol Administrador que no estén
   asignadas a otro hotel. Ya no son nombres fijos del catálogo de demostración.
4. Selecciona la cuenta y confirma. Revisa en la consola ambos documentos:
   hotel con `administratorUid` y usuario con `hotelId` coincidente.
5. Para sustituirlo, selecciona otro administrador disponible y confirma. La
   transacción libera al anterior, enlaza al nuevo y actualiza el hotel juntos.
6. Para retirarlo usa Desvincular y confirma. Ambos lados quedan sin asignación.
7. Para moverlo a otro hotel, desvincúlalo primero del anterior. Si la cuenta
   fue desactivada, puedes retirarla y elegir otra cuenta activa.

Si dos teléfonos intentan asignar la misma cuenta a hoteles distintos, la
transacción y las reglas impiden confirmar las dos asignaciones.
`administratorName` sirve para mostrar el nombre, no para conceder permisos.

## 6. Fotos: qué se comparte y qué sigue pendiente

Las fotos empaquetadas de demostración se referencian por nombre de recurso y
se ven en instalaciones de la misma versión. No se guardan IDs numéricos de
drawables como identificadores remotos estables.

Al seleccionar fotos del teléfono, se conserva la copia local y una referencia
privada por UID e ID de hotel en `hotel_private_galleries`. Una URI `file:` o
`content:` no se envía a Firestore como si otro celular pudiera abrirla.

Por eso tu carrusel puede mostrar esas fotos en tu teléfono y no en el de tu
compañera. El **paso 6** debe subirlas a Storage y completar su carga por URL.
El filtrado de referencias compartidas admite HTTPS, pero eso por sí solo no
implementa la descarga/visualización completa de imágenes remotas.

## 7. Qué comunicar a tus compañeros

- Cliente: se ajustó la fuente compartida `HotelCatalogRepository`, las opciones
  de habitaciones, el precio del resultado y la validación de reserva para usar
  hoteles/habitaciones remotos con sesión Firebase. No se rediseñaron sus vistas.
  Debe probar búsqueda → detalle → opciones → reserva con cuentas y reglas reales.
- Hotel-admin: su pantalla todavía utiliza su repositorio local y hotel de
  demostración. Su responsable debe resolver el hotel desde `users/{uid}.hotelId`,
  comprobar `administratorUid` y adaptar inventario a ese hotel. Guardar una
  asignación remota no migra automáticamente sus pantallas.
- Las reglas de esta etapa permiten a superadmin crear habitaciones para la
  importación. Editar/borrar habitaciones y su gestión por HOTEL_ADMIN siguen
  cerrados; deben implementarse con sus validaciones antes de habilitarlos.
- Reportes, actividad e historial administrativo siguen usando datos locales
  o demostrativos. Su migración a datos remotos y sus permisos es una etapa
  posterior. No se subieron reservas ficticias para llenar reportes.
- No publicar reglas parciales que sustituyan las de los compañeros. Revisar
  el contrato compartido y probar los flujos afectados juntos antes de desplegar.

## 8. Cómo está organizado el código

1. `ManagedHotel` conserva los campos de pantalla y añade UID remoto, precio e
   inventario. `AdminUser.hotelId` representa su asignación real.
2. `HotelFirestoreCodec` convierte mapas Firestore al modelo, comprueba IDs y
   coordenadas, separa fotos compartidas y limita qué campos puede editar el
   formulario. Es Java puro para probarlo sin una cuenta real.
3. `FirebaseHotelsRepository` escucha el catálogo, guarda cambios, obtiene
   habitaciones y ejecuta transacciones. Sus listeners se retiran cuando no hay
   pantallas observando; los errores no activan un catálogo local alternativo.
4. `SuperadminViewModel` entrega estado de carga, resultados y hoteles a las
   vistas. La lectura de Room para importar se realiza fuera del hilo de UI.
5. Las Activities/Fragments conservan sus bindings XML y llaman al ViewModel.
   El formulario conserva el borrador al volver del mapa o selector de fotos;
   una reconexión del catálogo no debe rellenarlo y borrar lo que escribiste.
6. `firestore.rules` comprueba rol activo, campos editables, auditoría, contador
   y relación bidireccional. Ocultar un botón no reemplaza estos permisos.

## 9. Verificación y límites

Verificaciones locales realizadas:

- Compilación de app, pruebas JVM y APK de instrumentación.
- 38 pruebas JVM: 31 anteriores y 7 del codec de hoteles, sin fallos.
- Emulador Firestore: 38 comprobaciones de autenticación, 38 de hoteles y
  2 de concurrencia. Cubren permisos, asignaciones incompletas/dobles,
  IDs, coordenadas y ausencia de sobrescritura.
- Android emulado: 6 pruebas de acceso, incluyendo que sin sesión no se usa
  el catálogo local como reemplazo ni se asignan IDs de usuarios locales.
- 7 comprobaciones JavaScript del selector de ubicación.

No se ejecutó un recorrido positivo completo contra Firebase real ni se probó
en el A55. Las pruebas no crearon usuarios, hoteles o reservas en producción:
los datos de reglas pertenecen al emulador local `demo-iotelito-auth`.
La suite visual completa requiere una sesión superadmin real y no se abrió un
atajo que eluda ese requisito.

Después de preparar la consola, prueba manualmente: crear hotel, volver del
mapa con texto editado, añadir foto, rotar pantalla, guardar, editar, cambiar
estado, asignar/sustituir/retirar administrador y consultar desde otro celular.
Comprueba también que un hotel sin inventario no permita reservar.

Referencias oficiales: [Transacciones de Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions)
y [validación de operaciones atómicas con reglas](https://firebase.google.com/docs/firestore/security/rules-conditions#access_other_documents).
