# Fotos del perfil en AWS

## Estado

- Bucket privado creado por el usuario: `iot-telito-imagenes`, `us-east-1`.
- Lambda subida por el usuario: `iot-telito-imagenes-api`, `LabRole`.
- API HTTP creada por el usuario: `https://9s5rtfhae6.execute-api.us-east-1.amazonaws.com`.
- Comprobado: `/health` devuelve 200; Lambda devuelve 401 al pedir una subida sin sesión.
- La app local ahora solicita autorización con su token Firebase, sube por multipart POST a S3
  y guarda la referencia `s3://iot-telito-imagenes/profilePhotos/{uid}/{uuid}.jpg` en users.photoUrl.
- La lectura solicita una URL temporal nueva, descarga fuera de UI y verifica que la sesión y la
  vista sigan siendo las mismas antes de mostrar el resultado. No guarda enlaces temporales.
- El token Firebase solo se envía al host de nuestra API. No se agregan claves AWS al APK.
- No se modificaron las imágenes de hoteles ni sus pantallas. El bucket las podrá alojar después.
- Pendiente: publicar manualmente el pequeño ajuste de Firestore y comprobar una subida/lectura
  con una cuenta real. Las pruebas locales no acreditan los permisos de LabRole sobre S3.

## Firestore: cambio manual necesario

1. Abrir Firebase, proyecto `iot-telito` > Firestore Database > Reglas.
2. Copiar las reglas actualmente publicadas a un respaldo nuevo. No reemplazar cambios del equipo.
3. Buscar `function ownProfilePhoto(data, uid)`.
4. Si existe y tiene la versión anterior gs://, reemplazar SOLO esa función por la siguiente:

```text
function ownProfilePhoto(data, uid) {
  return data.photoUrl is string && (data.photoUrl == ''
    || data.photoUrl.matches('gs://[^/]+/profilePhotos/' + uid + '/[a-zA-Z0-9-]+[.]jpg')
    || data.photoUrl.matches('s3://iot-telito-imagenes/profilePhotos/' + uid
      + '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.]jpg'));
}
```

5. Revisar y publicar personalmente. Si la función no existe, no basta con añadirla: confirmar
   primero si están publicados los cambios previos del perfil (nombres/contactos e identidad protegida).
   Comparar con firestore.rules e integrar los cambios del equipo antes de publicar.
6. No modificar reglas de Firebase Storage para esta integración. S3 no usa esas reglas.

La compatibilidad gs:// se conserva en la validación de datos anteriores; la subida y lectura del
perfil en esta app utilizan AWS. No se migraron objetos ni se ha habilitado Firebase Storage.

## Prueba desde Android Studio

1. Compilar/ejecutar esta versión de Entregable4. Entrar con un cliente habilitado.
2. Perfil > Editar perfil > elegir foto: debe verse la vista previa.
3. Cancelar y comprobar que no cambia la foto guardada. Volver a editar y elegirla otra vez.
4. Guardar con internet. Esperar la confirmación; los datos solo se anuncian guardados tras Firestore.
5. En S3 > bucket > Objetos, comprobar `profilePhotos/{uid}/{uuid}.jpg`. Mantener acceso público bloqueado.
6. En Firestore > users > UID, comprobar que photoUrl sea s3://... y no una URL HTTPS temporal.
7. Volver al perfil y reiniciar la app: debe solicitar un enlace de lectura y mostrar la foto.
8. Entrar en un segundo dispositivo con la misma cuenta y comprobar la foto.
9. Entrar con otra cuenta: no debe mostrar la imagen de la cuenta anterior.
10. Probar sin internet: debe conservar el formulario y no anunciar éxito. Reintentar al recuperar conexión.
11. Registro con foto opcional: se crea primero el perfil y después se sube la foto. Si falla solo la
    foto, la cuenta permanece creada; se puede añadir la foto después desde Editar perfil.

Si hay error, compartir el mensaje de la app y, si procede, el error de CloudWatch sin tokens,
URLs firmadas, credenciales ni datos personales. No abrir el bucket al público ni añadir claves al APK.
Una foto que sube a S3 pero no logra guardarse en Firestore puede quedar como objeto privado sin
referencia; no se borran automáticamente fotos anteriores. Limpieza de objetos queda para después.

Código y contrato de la API: aws/imagenes/README.md. No hace falta volver a subir el ZIP por este
cambio de Android: el contrato de la Lambda que ya se subió no cambió.

## Identidad incompleta y teclado

Si users/{uid} no tiene documentType, documentNumber y birthDate, la app muestra No registrado.
Guardar nombres/foto no completa ni elimina esos campos. Firebase Auth no proporciona documento
ni nacimiento. La ausencia de campos no permite determinar por sí sola cómo se creó esa cuenta.
El registro actual prepara los tres campos validados antes de crear Auth; las reglas exigen los
tres al registrar un cliente nuevo. La edición conserva los datos de identidad bloqueados.

Para una cuenta existente incompleta, el administrador autorizado puede añadir manualmente en
Firestore los datos reales verificados: documentType (string: DNI, Carnet de Extranjería o Pasaporte),
documentNumber (string, conservar ceros iniciales), birthDate (string AAAA-MM-DD).
No se realizan estas modificaciones automáticamente ni es necesario recrear la cuenta.

Registro y Editar perfil reservan espacio para el teclado y desplazan el campo enfocado con margen;
Siguiente recorre los campos editables y Listo cierra el teclado. En Editar perfil se oculta el menú
inferior mientras está abierto para aprovechar el espacio y se restaura al cerrarlo.
Compilación y 106 pruebas JVM correctas; 38 comprobaciones de autenticación y 34 de perfil en el
emulador local correctas. La revisión visual del teclado requiere ejecutar la app en el dispositivo.
