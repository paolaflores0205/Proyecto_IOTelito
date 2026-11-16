# Perfil remoto del cliente

> Actualización: las fotos del cliente ahora usan S3 + Lambda + API Gateway. Consultar
> PERFIL_CLIENTE_AWS.md para activación y pruebas actuales. Las instrucciones de Firebase Storage
> de este documento describen la implementación anterior y ya no se aplican a las fotos del perfil.

Implementación preparada en Entregable4, rama ultima_03_11. No se publicaron reglas, no se crearon buckets ni se modificaron datos o planes del proyecto Firebase del equipo.

## Comportamiento

- Firestore `users/{uid}` es la fuente principal del perfil. Se conserva una copia de sus datos por UID en las preferencias del dispositivo.
- Se pueden editar nombres, apellidos, teléfono, domicilio y cambiar la fotografía. El correo de acceso, tipo/número de documento y nacimiento se muestran como datos de solo lectura.
- El guardado usa una transacción y solo actualiza campos editables más `updatedAt`. No envía documentos, cumpleaños, correo, rol o estado. El éxito se anuncia después de la confirmación del servidor.
- Sin conexión, Perfil muestra la última copia guardada y un aviso. Editar requiere cargar el perfil del servidor antes de habilitar el formulario. Si un guardado falla, conserva lo escrito y la imagen preparada para reintentar.
- La fotografía se elige mediante el selector del sistema, se orienta y convierte a JPEG de hasta 1024 px y menos de 2 MiB. La vista previa no se sube hasta Guardar. Puede descartarse la nueva foto sin cambiar la anterior.
- Las fotos se guardan en `profilePhotos/{uid}/{uuid}.jpg`; `users.photoUrl` conserva una ruta gs privada, sin enlaces públicos de descarga. Las reglas limitan acceso al propietario cliente habilitado y no permiten sobrescribir objetos.

## Registro y cuentas anteriores

El registro permite DNI (8 dígitos), carnet de extranjería (9 a 12 caracteres alfanuméricos) o pasaporte (6 a 12 caracteres alfanuméricos). Son validaciones de formato de esta aplicación, no verificaciones oficiales de identidad. El nacimiento exige una fecha real y mayoría de edad. Se mantienen los límites del teléfono peruano de 9 dígitos.

La foto puede elegirse en el registro. Primero se crea la cuenta y su perfil; después se sube la imagen, porque Storage exige un perfil cliente habilitado. Si solo falla la foto, se informa que la cuenta ya existe y se permite añadirla después desde Editar perfil. No se borra ni se vuelve a crear la cuenta.

No se inventan números de documento, cumpleaños ni apellidos para cuentas anteriores. Si solo existe un nombre completo, se muestra ese nombre en el primer campo y apellidos queda vacío hasta que el usuario lo complete. La identidad faltante permanece como No registrado: debe corregirla el administrador autorizado desde Firebase, tras verificarla. El cliente no puede completarla por su cuenta.

Los cambios de nombres no modifican la identidad, el UID, el rol ni la habilitación. Firebase Authentication mantiene el mismo correo y contraseña. Se intenta sincronizar también el nombre visible de Auth, pero Firestore sigue siendo la fuente de los datos del perfil.

## Activación manual: Firestore

1. Abre la consola del proyecto del equipo y entra a Firestore Database > Reglas.
2. Guarda otra copia del contenido actualmente publicado, por ejemplo `firestore-publicadas-perfil-respaldo.txt`, sin reemplazar los respaldos anteriores.
3. Compara con `firestore.rules` de esta carpeta antes de publicar. Esta versión conserva el checkout y los bloques de usuarios, hoteles y taxi, y restringe los campos de identidad del cliente.
4. Si hubo cambios de compañeros desde la última publicación, intégralos antes de sustituir el contenido.
5. Publica las reglas combinadas revisadas y ejecuta la versión actualizada de la app. Las versiones antiguas del formulario, que intentaban modificar identidad o solo un nombre inconsistente, pueden ser rechazadas.

## Activación manual: Storage

1. En la consola del mismo proyecto, abre Storage.
2. Si ya hay pestañas de Archivos y Reglas, guarda una copia de sus reglas publicadas como `storage-publicadas-respaldo.txt` para compararlas con el `storage.rules` local. No reemplaces bloques nuevos del equipo que no estén en el archivo local.
3. La versión local conserva `hotelPhotos` y añade el bloque independiente `profilePhotos`. Publica la versión combinada solo después de comparar ambos archivos.
4. Si aparece Comenzar, solicitud de crear un bucket, escoger ubicación o cambiar un plan, comparte ese estado antes de continuar: no se ha activado nada de eso desde esta tarea.
5. Las reglas de Storage consultan Firestore para verificar el perfil habilitado. Si Firebase solicita permisos adicionales para esa integración, revisa el mensaje antes de aceptarlos; no cambies reglas a acceso público para evitar el paso.

## Prueba manual después de activar

1. Inicia sesión con una cuenta cliente y comprueba sus datos en Perfil.
2. En Editar perfil, verifica que nombres, apellidos, teléfono y domicilio sean editables; documento, nacimiento y correo deben seguir visibles y sin teclado.
3. Guarda un cambio pequeño y comprueba que solo los campos editables y updatedAt cambiaron en el documento propio de Firestore.
4. Cierra sesión y entra en otro dispositivo con esa cuenta. Debe cargar los datos guardados del servidor.
5. Elige una foto y pulsa Cancelar: el perfil remoto debe conservar la foto anterior.
6. Elige una foto y Guarda: debe aparecer la imagen en ambos dispositivos. Si Storage no está listo, conserva el formulario y muestra el fallo; puedes descartar la nueva foto y guardar los demás datos.
7. Sin conexión, Perfil debe mostrar el aviso y los últimos datos guardados. No debe anunciar un guardado remoto exitoso.
8. El registro con documento alternativo debe guardar su tipo y número correctos, y protegerlos de edición posterior.

## Verificación y límites

Compilación de la aplicación y del APK de pruebas Android; pruebas JVM del formato, conversión de perfiles antiguos y campos editables. Pruebas de Firestore local con proyecto demo para comprobar el registro, identidad inmutable, fotos propias, datos de contacto y rechazo de cuentas ajenas. También se ejecutaron las suites existentes de checkout, hoteles, reportes y taxi.

No se ejecutaron nuevas pruebas en dispositivos del equipo ni se usaron cuentas reales. La subida y lectura de Storage y sus reglas requieren comprobación manual tras activar su configuración. No se afirma que el bucket remoto esté habilitado o que sus reglas actuales coincidan con las locales. No se alteró la configuración de pagos ni de facturación.
