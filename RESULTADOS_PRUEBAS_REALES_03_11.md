# Pruebas con cuentas reales — 03/11/2026

Versión local de superadmin con los merges de administrador y taxista y los
ajustes de presentación. Emulador Android Pixel_4, proyecto Firebase iot-telito.
El A55 conectado no se modificó ni se cerró su sesión.

Se usaron los SDK Android y los repositorios de la aplicación contra Firebase
real. Las credenciales se pasaron como argumentos temporales de instrumentación;
no se incorporaron contraseñas al código, archivos de configuración o Git.

## Acceso y datos de las cuentas

| Rol | Resultado |
|---|---|
| Cliente | Autenticación y perfil CLIENT válidos; lectura de su reserva previa permitida |
| Administrador | Autenticación válida; hotel 1 activo y asignación consistente en ambos documentos |
| Taxista | Autenticación y perfil TAXISTA válidos; perfil operativo activo y disponibilidad booleana |
| Superadmin | Autenticación y perfil válidos tras corregir la contraseña; lectura global y comprobaciones de pantallas correctas |

Las primeras comprobaciones de superadmin rechazaron la contraseña inicialmente
proporcionada. El usuario confirmó el correo con a al inicio y corrigió la clave;
la comprobación posterior fue satisfactoria. No se restableció ninguna contraseña
ni se modificaron perfiles/roles. Ninguna contraseña se incluye en este informe.

## Flujo compartido realizado y confirmado en servidor

1. Admin creó la habitación QA con ID 607663366 en el hotel 1.
2. Editó el precio de S/ 151.50 a S/ 175.75 y se confirmó el campo en Firestore.
3. Creó el servicio QA 1312055240 y editó su precio de S/ 12.50 a S/ 15.75.
4. Cliente obtuvo el mismo inventario remoto y encontró esa habitación con el
   precio actualizado. No se usó una copia de Room como confirmación.
5. Se creó la reserva 1938769436, código QA-MW0WITRS, entrada 01/02/2027 y
   salida 02/02/2027, por S/ 175.75. Las fechas se eligieron en el futuro y se
   verificó que el cliente no tuviera bloqueo previo para esa noche.
6. Cliente creó la solicitud de taxi 1938769436 para esa reserva.
7. Taxista confirmó ASIGNADO → EN_CAMINO → EN_TRASLADO → FINALIZADO.
   Cada estado se consultó de nuevo en el servidor.
8. Se comprobó que el bloqueo activeRequestId quedara vacío tras finalizar.
9. Cliente leyó FINALIZADO desde su propia sesión: se confirmó el estado compartido.

Resultado de esta pasada: **OK (1 test)**, 30.299 segundos. La prueba excluyó
explícitamente superadmin por la credencial pendiente; no equivale a cuatro roles
aprobados. Dos pasadas anteriores de acceso detectaron ese mismo problema antes
de crear registros de prueba.

## Estado dejado después de las pruebas

- Habitación y servicio QA archivados mediante las operaciones normales de admin.
  No se borraron registros históricos ni se alteró el inventario comercial previo.
- Disponibilidad original del taxista restaurada; sin servicio QA activo.
- Reserva QA conservada como CONFIRMADA y solicitud de taxi como FINALIZADO.
  Sus IDs/código permiten identificarlas. No se efectuó cobro ni cancelación.
- La reserva aparecerá en los reportes que incluyan febrero de 2027, si su lectura
  administrativa está configurada. La consulta posterior con superadmin confirmó
  su acceso y los cálculos diario, mensual y anual de esa reserva QA.
- Se cerró la sesión de prueba del emulador.

## Límites y siguientes comprobaciones

- No se ejecutaron cambios de estado de usuarios/hoteles ni se modificó la
  asignación de un administrador durante estas pruebas reales. La prueba de
  formulario/selección no confirma una reasignación remota.
- No se desactivaron cuentas reales de compañeros para ensayar revocación.
- Se comprobó además la navegación y algunos controles de superadmin mediante
  instrumentación. No se probaron los clics de todas las pantallas de los roles.
  No se certifican diseño del A55, navegación visual completa,
  manejo del teclado, notificaciones con la app cerrada, GPS o cámara/QR reales.
- No se probó carga de fotos Storage ni se cambió plan/facturación.
- No se publicaron reglas y no se hizo push.

La clase FirebaseLiveRolesTest queda como prueba optativa reutilizable. Sin el
argumento qaAccounts se omite y no cierra una sesión existente. Las escrituras
requieren además qaWrite=true. No ejecutarla sobre otro proyecto o cuentas de
producción sin preparar antes el recorrido y sus registros de prueba.

## Comprobación posterior de superadmin: satisfactoria

Con la contraseña corregida, la pasada solo de superadmin terminó **OK (1 test)**
en 36.505 segundos, sin crear nuevas reservas ni modificar datos comerciales.

- Acceso permitido con el perfil SUPERADMIN activo.
- Lecturas confirmadas en servidor: 4 usuarios, 13 hoteles, 2 reservas y 14 eventos
  en el momento de la consulta. Son valores observados, no cifras fijas del sistema.
- Lectura de la reserva QA 1938769436 desde la cuenta administrativa.
- Cálculos DAILY, MONTHLY y YEARLY con esa reserva: 1 reserva, S/ 175.75 y ranking
  del hotel correspondiente. Es una comprobación aislada de esa reserva, no un
  importe afirmado para todas las reservas del proyecto.
- Prueba instrumentada del menú blanco de 64 dp: opciones visibles completas y
  posición sobre la barra del sistema en Pixel_4; navegación a Usuarios y Hoteles.
- Formulario del hotel 1 cargado; nombre obligatorio validado sin guardar cambios.
- Pantalla de asignación cargó cuentas de administradores reales y permitió la
  selección, sin confirmar ni cambiar la asignación.
- Bandeja de avisos cargó los hoteles pendientes. No se verificó entrega push.
- Sesión del emulador cerrada al terminar. Credenciales solo en memoria y argumentos
  temporales; no incorporadas a Git. El A55 permanece sin cambios.
