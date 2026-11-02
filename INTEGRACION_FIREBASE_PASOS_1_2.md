# Integración Firebase: pasos 1 y 2 de superadmin

Fecha: 02/11/2026. Rama de trabajo: superadmin.

## Qué se realizó

1. Se consultó origin y se integró origin/feature/inicio-cliente-reserva,
   commit a62be03, con el registro/login Firebase y las reservas Firestore del cliente.
   Merge local: b832952.
2. Se integró origin/feat/hotel-admin, commit 8f47518, con navegación de alertas
   y ocultación de checkout resuelto en el panel del administrador.
   Merge local: 8fab85a.
3. Se amplió FIRESTORE_SCHEMA.md para definir un contrato compartido entre cliente
   y superadmin, manteniendo los campos y nombres ya utilizados por cliente.

Los merges no tuvieron conflictos textuales. No se hizo reset, stash ni se eliminaron
archivos del usuario. Los cambios previos de .idea y la carpeta app/build_anterior_2
se dejaron fuera de los commits de integración.

## Qué se conservó

Se comprobó por comparación Git contra ef1aaa6 que no cambiaron:

- La lógica de insets y menú inferior de IoTelitoApplication.
- Los layouts del menú principal y chat del administrador.
- Las vistas y la lógica de superadmin, incluidos el selector de ubicación y reportes.
- SuperadminSampleData, SuperadminRepository y el catálogo ampliado local.
- HotelCatalogRepository, HotelMedia y SampleHotelGalleries.
- El selector de mapa HTML y su corrección de estabilidad.

El catálogo y los reportes de superadmin siguen siendo locales. Conservarlos en
este paso no significa que los registros locales ya se compartan con otros celulares.

## Ajuste necesario en pruebas

La integración del cliente añadió roomIds al método agregarReserva. Se actualizó
ClientTaxiNavigationTest para proporcionar el ID real de la primera habitación
disponible del hotel de su fixture. No se modificó el comportamiento productivo
de reservas para simular compatibilidad con la firma anterior.

## Estructura compartida acordada como contrato

- users/{uid}: identidad de Firebase, perfil, rol canónico, active y hotelId.
- hotels/{id}: conserva IDs numéricos, información del hotel, ubicación y administratorUid.
- hotels/{id}/rooms/{id}: inventario físico con IDs compatibles con las reservas.
- reservations/{id}: conserva el esquema implementado por cliente.
- roomNights y clientNights: conserva los bloqueos transaccionales de cliente.
- logs/{id}: eventos administrativos con autor y timestamp de servidor.

El contrato detalla tipos, relaciones, migración sin duplicados, distinción entre
datos de demostración y reales, fotos locales frente a compartidas y la matriz
objetivo de permisos. Los nombres propuestos nuevos deben ser respetados o
actualizados de forma coordinada cuando los demás responsables los implementen.

## Qué NO se hizo todavía

- No se crearon cuentas ni colecciones en la consola de Firebase.
- No se subió el catálogo local ni se crearon cuentas falsas para los ejemplos.
- No se publicaron ni se abrieron las reglas de Firestore.
- No se implementó todavía el login Firebase de superadmin ni la lectura de roles.
- No se conectaron Usuarios, Hoteles, Reportes ni Logs de superadmin a Firestore.
- No se sustituyó el catálogo local del cliente por uno remoto.
- No se configuró Firebase Storage ni se subieron imágenes.
- No se hizo push de esta integración.

Las reglas integradas del cliente permanecen intactas: no habilitan las operaciones
futuras de superadmin. Se actualizarán y probarán junto con la autenticación y roles,
antes de permitir las escrituras administrativas. Documentar un esquema no crea
automáticamente la base de datos ni configura los permisos remotos.

## Verificación local

Se ejecutó:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --no-configuration-cache --no-daemon --no-watch-fs --max-workers=2 --console=plain
```

Resultado: BUILD SUCCESSFUL. Se generaron la app debug y el APK de pruebas Android.
Las 24 pruebas unitarias terminaron con 0 fallos y 0 errores.

También se ejecutó app/src/test/js/location_picker.test.cjs: sus 7 comprobaciones
del selector de ubicación pasaron.

En el emulador Pixel_4 pasaron 3 pruebas instrumentadas, sin fallos:

- SuperadminUiTest.menuBlancoDe64dpMantieneOpcionesVisiblesYNavegacion:
  fondo blanco, altura 64dp, botones visibles y navegación a listas con al menos
  16 usuarios y 12 hoteles.
- HotelAdminInsetsTest.adminMenuUsesOneBottomInsetAnd64dp:
  menú compacto, etiquetas visibles y posición sobre la barra del sistema.
- HotelAdminInsetsTest.threeButtonInsetIsNotAddedTwice:
  aplicación repetida de insets sin duplicar la separación inferior.

No se ejecutó toda la suite instrumentada ni se probó el A55 en esta revisión.
ClientTaxiNavigationTest se adaptó y compiló, pero no se ejecutó: no se afirma
que su flujo local cubra el nuevo flujo remoto de reservas o la sesión Firebase.

No se probó contra el servicio remoto de Firebase. La compilación confirma que el
SDK y google-services.json se integran, pero no demuestra que Authentication esté
habilitado, que las reglas remotas coincidan ni que una reserva se escriba en nube.

## Cómo comprobarlo en Android Studio

1. Abre IoTelito y comprueba que la rama seleccionada sea superadmin.
2. Sincroniza Gradle: se agregaron Google Services, Firebase Auth y Firestore.
3. Ejecuta la app en tu dispositivo sin desinstalarla ni borrar datos.
4. Comprueba que tu menú, catálogo, formularios, mapas y reportes sigan apareciendo.
5. El cliente ahora requiere cuenta Firebase para el login. Sus credenciales locales
   antiguas no bastan; superadmin/admin/taxista mantienen por ahora el acceso demo.
6. Probar registro o reserva del cliente crea datos remotos reales: hazlo solo con
   cuentas de prueba coordinadas con el equipo y con su configuración Firebase lista.

## Próximo paso, todavía no implementado

Paso 3: definir el alta controlada de la primera cuenta superadmin, asociar su UID
a users/{uid}, resolver rol/estado después del login y preparar las reglas de acceso.
Después se conectarán los repositorios de superadmin, manteniendo las interfaces.
