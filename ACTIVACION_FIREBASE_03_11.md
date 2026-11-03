# Activación del Firebase compartido: revisión del 03/11/2026

Proyecto remoto revisado: iot-telito, Firestore (default). Rama local: superadmin.

## Hallazgos de la revisión de consola

- La búsqueda exacta de `a20222079@pucp.edu.pe` en Authentication no encontró una
  cuenta. El acceso como colaborador a Firebase no crea esa cuenta de la app.
- La lista visible de colecciones contenía clientNights, reservations, roomNights,
  taxiDrivers, taxiRequests y taxistas. No aparecían users ni hotels en esa lista.
- Las reglas publicadas permiten lectura autenticada de hoteles y rechazan sus
  escrituras. También contienen reglas específicas de taxi que no estaban en el
  archivo local anterior de superadmin.
- No se modificaron cuentas, documentos, permisos, reglas, plan ni facturación
  en la consola durante esta revisión.

## Cambio local preparado antes de publicar

Se incorporaron al archivo `firestore.rules` los bloques existentes de taxistas,
taxiRequests y taxiDrivers leídos de la consola, conservando su contrato:

- taxistas: lectura propia y cambio exclusivo de disponible cuando activo es true.
- solicitudes de taxi: creación por dueño de reserva, asignación y progresión de
  estados por conductor, finalización con fecha de servidor.
- bloqueo taxiDrivers: aceptar/finalizar el servicio y reservar/liberar el
  conductor en la misma operación atómica.

Esto se combina con las reglas de users, hoteles, contador, reservas y logs de
superadmin. No publiques el archivo de una revisión anterior que carezca de esos
bloques: bloquearía las operaciones de taxi.

El estado del contrato de taxi sigue siendo `taxistas.activo`, como en las reglas
existentes. No se enlazó automáticamente con `users.active`: hay que coordinar
con el responsable de taxi una transición de ambos estados si se desea un único
interruptor de desactivación para ese módulo. Tampoco se cambiaron sus datos.

## Acción manual 1: crear tu cuenta de la app

1. Authentication → Usuarios → Agregar usuario.
2. Correo: a20222079@pucp.edu.pe.
3. Escribe una contraseña privada y guarda. No la compartas en el chat ni en Git.
4. Abre la cuenta creada y copia su UID exacto.

## Acción manual 2: crear el perfil que determina tu rol

En Firestore → Datos, en la raíz de (default), inicia la colección `users` si
todavía no existe. No la crees dentro de clientNights ni de otro documento.

ID del documento: el UID exacto copiado de Authentication, sin usar ID automático.

| Campo | Tipo en consola | Valor |
|---|---|---|
| uid | string | El mismo UID que el identificador del documento |
| name | string | Superadmin, o tu nombre visible |
| email | string | a20222079@pucp.edu.pe |
| role | string | SUPERADMIN, exactamente en mayúsculas |
| active | boolean | true (valor booleano, no texto) |
| hotelId | null | null |

Son los campos mínimos del perfil inicial. El login guarda sus marcas de acceso
con timestamps de servidor; el registro cliente incorpora los campos de su formulario.
No modifiques los documentos de tus compañeros ni los promociones por nombre.

## Acción manual 3: revisar y publicar las reglas combinadas

1. Abre el `firestore.rules` de esta rama actualizado con los bloques de taxi.
2. En la consola comprueba que nadie haya publicado una versión posterior a la
   revisada. Si hubo otra publicación, vuelve a comparar antes de sustituir reglas.
3. Guarda una copia del texto publicado o utiliza su historial de versiones.
4. Copia el contenido completo del archivo combinado al editor de reglas y
   publica una vez revisado con el equipo. No publiques fragmentos sueltos.
5. Inicia sesión en la app con tu cuenta y comprueba las vistas de superadmin.
6. Publica el catálogo de ejemplo solo si el equipo quiere esos datos en el
   Firebase real, usando su botón y confirmación. No se ejecutó en esta revisión.

Configura también perfiles users para las otras cuentas que deban entrar con el
login integrado; Auth por sí solo no aporta el rol de negocio. Storage sigue
pendiente de configuración del equipo y no se activó facturación.

## Verificación local de la combinación

En el emulador efímero demo-iotelito-auth se ejecutan las suites de autenticación,
hoteles/asignaciones y reportes/logs, además de 21 casos nuevos de compatibilidad
de taxi. Cubren disponibilidad, propiedad de reserva, horario, transacciones de
asignación/bloqueo, progresión de estados, finalización/liberación e inmutabilidad
de campos no autorizados. No escriben datos en el Firebase real.
