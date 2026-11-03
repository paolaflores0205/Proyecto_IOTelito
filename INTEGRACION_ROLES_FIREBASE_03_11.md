# Integración de roles Firebase en superadmin — 03/11/2026

Se integró `origin/feat/hotel-admin` (`d8de558`) sobre `superadmin` (`fdcb77a`).
La rama de admin ya contiene `origin/feature/inicio-cliente-reserva` (`7c0b83e`),
por lo que no se repitió un segundo merge de los cambios de taxista.

## Resultado

- Superadmin conserva sus hoteles/usuarios remotos, asignaciones, reportes,
  actividad, alertas, cargador de imágenes y configuración de Storage.
- Admin recibe el hotel asignado desde el perfil users, valida ambos vínculos y
  gestiona sus datos, habitaciones y servicios con Firestore. Las retiradas de
  inventario conservan los documentos con archived y deshabilitan su disponibilidad.
- Taxista incorpora perfil operativo, disponibilidad, solicitudes, aceptación,
  estados de servicio y bloqueo por conductor. Se conserva users como origen del
  rol compartido y taxistas como perfil operativo del conductor.
- El perfil de sesión conserva hotelId; cambiarlo invalida una sesión admin
  anterior. El login valida también el perfil operativo para TAXISTA.
- La galería de consulta del admin utiliza el cargador compartido de superadmin.

## Resolución de la integración

Git no reportó conflictos textuales, pero combinó dos copias del contrato de taxi
en `firestore.rules`, incluida la función taxiDriver duplicada. Se retiró la
copia repetida y quedó un único bloque de taxistas/taxiRequests/taxiDrivers,
junto con los permisos de superadmin y los nuevos permisos de hotel-admin.

Se comprobó que no cambiaron el layout del menú de superadmin, su Activity de
navegación, su repositorio de catálogo ampliado ni SampleData/SampleHotelGalleries.
IoTelitoApplication solo añade el guard de acceso del admin; no cambia insets.
Los archivos personales .idea y app/build_anterior_2 permanecen fuera del commit.

## Verificación local

- Gradle: assembleDebug, testDebugUnitTest y assembleDebugAndroidTest correctos.
- 68 pruebas JVM, 0 fallos y 0 errores.
- Firestore local: autenticación/usuarios, hoteles/asignaciones y concurrencia,
  31 comprobaciones de hotel-admin más su caso concurrente, 15 de reportes/logs y
  21 de compatibilidad taxi. Todas correctas. No se escribieron datos reales.
- No se ejecutó instrumentación Android ni pruebas positivas con cuentas reales
  en esta integración. Las validaciones remotas descritas por el compañero en
  GUIA_FIREBASE_ADMIN.md son su evidencia previa, no pruebas repetidas aquí.

No se publicaron reglas ni se hizo push en esta integración. Las reglas remotas
que el compañero documenta haber desplegado pueden ser posteriores a las usadas
antes por superadmin: no publicar una versión antigua ni solo fragmentos.

Para probar: iniciar como superadmin; asignar un administrador real a un hotel;
iniciar como ese admin y editar inventario; consultar el hotel desde cliente;
solicitar taxi y avanzar su servicio con una cuenta taxista preparada. Los límites
de reservas/reportes/mensajes todavía locales del admin siguen descritos en su
guía. Storage continúa dependiendo de la configuración y plan del equipo.
