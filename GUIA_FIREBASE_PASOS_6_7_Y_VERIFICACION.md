# Fotos, reportes, actividad y alertas de superadmin

02/11/2026 · Rama superadmin · Continuación del paso 5.

## Qué se implementó

- **Fotos:** al guardar un hotel, sus copias privadas nuevas se suben primero a
  Firebase Storage. Se conserva el orden y la primera sigue siendo portada.
  Firestore guarda referencias `gs://.../hotelPhotos/{uid}/{uuid}.jpg`, sin
  publicar enlaces con tokens. Si falla la subida no se guarda el hotel como si
  hubiera funcionado; se conservan las copias locales para reintentar.
- **Visualización compartida:** el carrusel, las tarjetas de superadmin y las
  fotos del cliente descargan imágenes de Storage con el SDK autenticado. También
  admiten HTTPS y recursos locales. La descarga/decodificación se realiza fuera
  del hilo de interfaz, tiene límite de 5 MB y evita poner una foto atrasada en
  una tarjeta reutilizada por RecyclerView.
- **Reportes:** superadmin consulta todas las reservas autorizadas de Firestore,
  no la consulta personal del cliente. Los filtros existentes de hotel y período
  recalculan métricas, gráfica y ranking. El historial simulado sigue disponible
  mediante un interruptor, ahora apagado por defecto y rotulado como demostración.
  No se publica en la colección de reservas reales.
- **Inicio:** el contador mensual utiliza reservas remotas según fecha de entrada;
  usuarios/hoteles siguen sus fuentes remotas. Se muestra una raya mientras no
  hay confirmación de las reservas, en vez de simular un contador confirmado.
- **Actividad:** altas/importación/edición/estado/asignación de hoteles y cambios
  de estado de usuarios incluyen un evento `logs` en la misma transacción. Si el
  evento es rechazado, tampoco se confirma la operación. La pantalla de actividad
  y el inicio leen los últimos 200 eventos remotos.
- **Alertas:** se derivan de hoteles remotos activos sin administrador. La bandeja
  y las notificaciones usan esa condición; el permiso, leído y deduplicación
  permanecen privados por UID y dispositivo. Asignar o desactivar el hotel retira
  su aviso cuando llega la actualización. Cerrar sesión cancela sus notificaciones.

No se cambió el menú inferior ni su tratamiento de insets. No se rediseñó cliente;
solo se conectó el cargador compartido de imágenes. No se migró la gestión local
de inventario del rol hotel-admin: sigue requiriendo coordinación con su responsable.

## Configuración que falta cuando tengas acceso

No necesitas enviarme tu contraseña. La persona administradora debe invitarte al
proyecto Firebase compartido y concederte el acceso correspondiente.

1. Verifica que estás en **iot-telito** y con la aplicación Android existente.
2. Completa cuentas y perfiles de la guía de pasos 3/4: el rol vive en
   `users/{uid}` y no se deduce del correo. Un usuario Auth sin perfil no basta.
3. Revisa y publica el archivo completo **firestore.rules** de esta versión.
   Añade lectura global de reservas solo para SUPERADMIN activo y permisos de
   `logs`; conserva los permisos de los compañeros. No pegues solo un fragmento.
4. En Storage confirma que existe el bucket predeterminado configurado en
   `google-services.json`: `iot-telito.firebasestorage.app`. Si aún no existe,
   debe prepararlo una persona autorizada; no se habilitó ni contrató nada aquí.
   Verifica en consola cualquier requisito de facturación antes de habilitarlo.
5. Revisa y publica **storage.rules** en la pestaña Rules de Storage. Es distinto
   del archivo de reglas de Firestore. Acepta altas JPEG menores de 5 MB solo
   bajo la carpeta del SUPERADMIN activo autenticado; otros usuarios activos
   pueden leer las fotos. No permite reemplazar archivos existentes.
6. Las reglas Storage consultan perfiles en Firestore: acepta/configura el permiso
   de integración entre servicios que solicite Firebase al publicar esas reglas.
7. Ejecuta la aplicación con Internet. Si no hay hoteles remotos, importa los
   ejemplos explícitamente desde el botón descrito en la guía del paso 5.
8. No subas la carpeta local `app/build_anterior_2`, claves de cuentas de servicio
   o archivos privados para solucionar permisos.

No se crearon cuentas, cambiaron reglas, subieron fotos o importaron datos en el
Firebase real durante esta implementación. Tampoco se hizo push.

## Recorrido manual de aceptación, una vez configurado

1. Inicia como superadmin real y crea un hotel con dos fotos y ubicación válida.
   Espera la confirmación. En consola verifica archivos en Storage y referencias
   `gs://` en `hotels/{id}.photos`.
2. Abre el catálogo desde otro dispositivo/cliente activo de la misma versión.
   Verifica portada y carrusel. La ruta no depende de archivos del primer celular.
3. Edita el hotel, cambia el orden de fotos, añade una y guarda. Reabre el detalle.
4. Deniega permisos de Storage o desconecta Internet antes de subir otra foto:
   la aplicación debe informar el fallo y conservar tu borrador/copias locales.
5. Crea una reserva real de prueba desde cliente en un hotel con inventario válido.
   En reportes superadmin elige el hotel y la fecha de entrada; prueba diario,
   mensual y anual. Mantén apagado el interruptor de demostración para comprobar
   únicamente datos remotos. No inventes reservas en consola para llenar números.
6. Cambia estado de un usuario y edita/asigna un hotel. En Actividad comprueba los
   eventos, el UID autor y la hora del servidor. Un usuario cliente no debe leerlos.
7. Concede notificaciones. Un hotel activo sin administrador debe aparecer en
   Avisos; abrirlo lleva al detalle protegido por sesión. Asignarlo debe quitarlo.
   Sal y vuelve a abrir para comprobar que no se repite el mismo aviso ya enviado.
8. Cierra sesión e inicia con otro UID: sus marcas de leído y avisos son propios.

## Límites que debes conocer

- Las notificaciones son **locales derivadas de un listener de Firestore** mientras
  la app está abierta, y se revisan al abrirla. No son FCM ni garantizan entrega
  con la aplicación cerrada. No se configuró un servidor de envío push.
- Storage y Firestore no comparten una transacción. Se suben fotos primero y
  luego se confirma el hotel. Si falla, se intenta limpiar solo los nuevos
  archivos de esa operación; si la app se cierra abruptamente o la limpieza falla,
  puede quedar algún archivo huérfano. No se borran automáticamente fotos antiguas
  al quitarlas del carrusel: podrían seguir referenciadas en otro lugar.
- Las referencias antiguas privadas no se publican al listar hoteles ni durante
  el arranque. Abre y guarda el hotel con sus fotos para migrarlas a Storage.
- Los reportes consultan el historial remoto disponible y calculan en memoria.
  Para grandes volúmenes deberá añadirse paginación o agregación de servidor.
  Ocupación es una estimación con inventario actual; ingresos prorrateados no
  equivalen a cobros bancarios ni a inventario histórico reconstruido.
- Los logs no se editan/borran desde la app y vinculan autor/fecha/entidad por
  reglas. Son eventos escritos por la aplicación, no una auditoría infalsificable
  de servidor. No se migró el historial simulado de Room a eventos reales.
- Las reglas Storage están preparadas y revisadas, pero su publicación, permisos
  entre servicios, subida y descarga reales **no se probaron** sin acceso al proyecto.
- No se verificó el A55 ni se ejecutó el recorrido positivo completo en la nube.

## Archivos principales para estudiar

1. `HotelCloudPhotos`: encadena las subidas usando Tasks del SDK Firebase.
2. `FirebaseHotelsRepository.save`: valida, sube fotos y después guarda el hotel;
   informa errores y conserva las copias locales cuando falla.
3. `HotelImageLoader`: carga Storage/HTTPS y protege las tarjetas recicladas.
4. `FirebaseAdminReadRepository`: listeners con estados de carga/error, retiro
   por ciclo de vida y aislamiento por sesión; convierte reservas y eventos.
5. `FirebaseAdminEvents`: añade un evento dentro de la transacción administrativa.
6. `ReportesFragment` y `ReportCalculator`: toman reservas remotas y recalculan
   los filtros existentes sin mezclar simulaciones de forma automática.
7. `RemoteHotelAlerts`: deriva pendientes y conserva las marcas locales por UID.
8. `firestore.rules` y `storage.rules`: permisos del servidor, independientes de
   que un botón esté visible en Android.

## Verificación local

Se compilan app, pruebas Java y APK de instrumentación. Las pruebas JVM incluyen
las referencias de Storage admitidas/rechazadas, además de cálculos y validaciones
anteriores. Las reglas Firestore se verifican en `demo-iotelito-auth`, emulador
local: usuarios/autenticación, hoteles/asignaciones, concurrencia y 15 casos nuevos
de reportes/logs. La suite de reportes confirma permisos globales solo para
superadmin, autoría, evento atómico, inmutabilidad y ausencia de borrado.

No se ha ejecutado instrumentación Android adicional en esta etapa. Compilar su
APK no equivale a ejecutar sus pruebas ni a verificar Storage contra la nube.

Referencias oficiales consultadas:
[Subir archivos desde Android](https://firebase.google.com/docs/storage/android/upload-files),
[Condiciones de las reglas de Storage](https://firebase.google.com/docs/storage/security/rules-conditions).
