# Revisión de superadmin y Firebase

Fecha: 16/11/2026. Revisión de código, requisitos y configuración aportada por el usuario; no certifica una ejecución completa contra Firebase real.

Actualización posterior: por instrucción del usuario se integró origin/feat/hotel-admin (de319b9) y se implementaron los puntos 1–3 de ramas, reportes y carga. Los hallazgos siguientes describen la revisión inicial; su resolución y las pruebas posteriores están en INTEGRACION_SUPERADMIN_16_11.md. La infraestructura AWS, el alta de administradores y la habilitación operativa de taxistas siguen pendientes.

## Fuentes y alcance

- Plan de proyecto de la carpeta Proyecto, especialmente páginas 5 a 9.
- Código local de superadmin en e97fe2a, coincidente con origin/superadmin al consultar GitHub.
- Reglas de Firestore pegadas por el usuario y tres capturas de Firebase Console.
- Ramas remotas origin/ultima_03_11 y origin/feat/hotel-admin, consultadas sin fusionarlas.
- Documento RESULTADOS_PRUEBAS_REALES_03_11.md: resultados históricos, no una prueba del estado actual.

No se publicaron reglas, cambiaron roles/asignaciones, crearon cuentas ni modificaron datos de Firebase. No se hicieron merges. Los archivos locales de Android Studio permanecen intactos.

## Qué muestran las capturas

Existen clientNights, hotelCheckouts, hotels, logs, reservations, roomNights, system, taxiDrivers, taxiRequests, taxistas y users. Los hoteles conservan IDs documentales numéricos en texto y subcolecciones rooms/services, compatibles con el diseño del proyecto.

En hotels/1 se observan active=true, administratorUid=null y administratorName=null. Esto representa un hotel sin administrador; no demuestra por sí mismo un error. Si algún users/{uid} de administrador todavía tiene hotelId=1, esa relación sí sería inconsistente y el acceso administrativo debe ser rechazado.

La lista de Authentication muestra cuentas, incluidas cuentas QA. Authentication no demuestra que sus documentos users existan, tengan UID coincidente y contengan rol/estado válidos. No eliminar las cuentas QA sin revisar las referencias de sus reservas y comprobantes.

La consola muestra plan Spark. El usuario aclaró después que el equipo mantendrá Authentication y Firestore y sustituirá Firebase Storage por imágenes en S3 mediante Lambda y API Gateway. No se requiere habilitar Firebase Storage para esa arquitectura; el servicio AWS aún debe verificarse e integrarse.

### Capturas adicionales de perfiles y reserva

- Superadmin: role=SUPERADMIN, active=true, hotelId=null y uid coincidente con el ID del documento. Los campos mostrados son compatibles con RemoteUserProfile; no necesita hotel asignado.
- Administrador: role=HOTEL_ADMIN, active=true y hotelId=9 numérico. No está asignado al hotel 1. Queda comprobar que hotels/9.administratorUid coincida con su UID y que el hotel esté activo; no cambiar la asignación a 1 solo por las capturas anteriores.
- Reserva 1137728651: hotelId=10, estado=CONFIRMADA, fechas ISO 2026-11-19 a 2026-11-20, precioTotal=260, roomIds=[1001], roomName y roomNumber presentes. Estos campos mostrados son compatibles con el lector actual. No demuestra compatibilidad con reservas que ya pasaron a EN_CHECKOUT/FINALIZADA ni confirma disponibilidad del inventario. La reserva de hotel 10 no corresponde a la cuenta administradora del hotel 9.
- En estas capturas no se ven campos cardType/cardLast4/cardToken/paymentMode; verificar el documento completo y la migración de reservas antiguas antes de probar checkout con las reglas nuevas. No inventar datos de pago para completar un documento.

## Hallazgos prioritarios

### 1. Estados remotos de reservas incompatibles con superadmin

Las reglas aportadas usan CONFIRMADA/ACTIVA -> EN_CHECKOUT -> FINALIZADA. El enum actual de model/Reserva.java solo contiene CONFIRMADA, PENDIENTE_PAGO, COMPLETADA y CANCELADA.

FirebaseAdminReadRepository.reservations usa Reserva.Estado.valueOf con el texto remoto. Al recibir ACTIVA, EN_CHECKOUT o FINALIZADA lanza una excepción, que observe captura y cuenta como registro inválido. La reserva no entra en los cálculos. Por eso un reporte puede quedar incompleto precisamente después del checkout.

Acción: definir un contrato compartido de estados con compatibilidad para COMPLETADA histórica; adaptar lectores, filtros y cálculo sin reescribir documentos antiguos a ciegas. Probar una reserva por estado y transiciones reales del cliente/admin.

### 2. Reglas publicadas y ramas diferentes

Las reglas pegadas difieren de firestore.rules de superadmin: incluyen el nuevo checkout, comprobantes, tarjeta simulada y mensajes. Reemplazarlas por el archivo local antiguo retiraría esos permisos y validaciones.

origin/feat/hotel-admin tiene cuatro commits posteriores no presentes en superadmin. Su punta consultada es de319b9. Incluye operaciones de administrador y cambios de fotos/servicios; no todo está cubierto por las reglas pegadas. En concreto, estas no contienen managedPhoto/validGallery, la actualización administrativa de photos ni fotoUrl en servicios presentes en esa rama.

Acción: preparar una versión integrada de código y reglas, revisar diferencias y ejecutar pruebas antes de cualquier despliegue. Conservar checkout, mensajes, bloqueos y contratos del cliente/taxista. Un archivo local no prueba qué está actualmente publicado.

### 3. Activación de taxista dividida entre dos perfiles

FirebaseUsersRepository.setActive modifica users/{uid}.active, motivo y auditoría. No modifica taxistas/{uid}.activo. El login normal revisa users y luego el perfil operativo taxistas; SessionRepository también observa users para cerrar una sesión desactivada.

Sin embargo, taxiDriver() en las reglas aportadas solo verifica taxistas.activo. Las operaciones de disponibilidad usan ese perfil operativo. Por tanto, el cierre de sesión de la interfaz no equivale a revocar esas operaciones en el servidor. Además, reactivar users no habilita a un conductor cuyo taxistas.activo siga falso.

Acción: acordar una única decisión de habilitación entre el módulo móvil, el backend web y las reglas; exigir identidad/rol/estado correctos en las operaciones de taxi. Separar aprobación inicial del taxista y disponibilidad temporal para aceptar viajes.

### 4. Caché de hoteles presentada como error

FirebaseHotelsRepository.observe trata isFromCache como error y vacía la lista, aunque el SDK pueda estar esperando el snapshot del servidor. Esto puede producir un mensaje transitorio de conexión al abrir Hoteles y afectar a los contadores/reportes.

origin/ultima_03_11 y origin/feat/hotel-admin ya incluyen una corrección con SnapshotStatus para distinguir ERROR, WAITING_FOR_SERVER y READY. Ese cambio aún no está en superadmin.

Acción: incorporar la corrección revisada, mantener el último resultado confirmado durante la espera y distinguir carga, error y lista vacía. Revisar el mismo comportamiento de Usuarios y de las lecturas administrativas.

### 5. Fotos compartidas: migración acordada a AWS

El código actual de superadmin todavía usa Firebase Storage: HotelCloudPhotos sube archivos a hotelPhotos/{uid}/{uuid}.jpg y guarda referencias gs:// en Firestore. Esto debe adaptarse al servicio de imágenes AWS acordado por el equipo, no resolverse activando Firebase Storage.

El avance nuevo del administrador también usa Firebase Storage mediante hotelAdminPhotos/{hotelId}/{uid}/{photo}.jpg. La sustitución debe coordinarse entre ambos roles y sus lectores, evitando mantener dos mecanismos incompatibles.

Arquitectura objetivo: Authentication conserva identidades; Firestore conserva datos de hoteles y referencias/metadatos de sus fotos; el backend API Gateway/Lambda autoriza las operaciones de imágenes y S3 almacena los archivos. Pendiente acordar endpoint, autenticación, carga, lectura, eliminación y formato de respuesta. No colocar claves AWS en Android ni guardar como referencia permanente una URL firmada que caduca; definir una clave estable y cómo obtener el acceso de lectura.

El requisito funcional de fotografías compartidas y mínimo de cuatro en el módulo administrador se mantiene aunque cambie el proveedor. No se modificó facturación ni se desplegaron recursos AWS.

### 6. Alta de administradores requerida, no resuelta por una asignación

El plan exige que el administrador del sistema registre administradores de hotel. En superadmin se encontró listado/estado/asignación de cuentas existentes; no un flujo que cree una cuenta HOTEL_ADMIN en Authentication y su perfil correspondiente.

RegisterActivity crea cuentas de cliente; las reglas de users permiten creación pública únicamente CLIENT. No se deben abrir esas reglas para permitir autopromoción de roles ni colocar credenciales de servicio dentro de Android.

Acción: implementar alta/invitación administrativa mediante un backend autorizado, conservando la sesión del superadmin, creando el perfil y registrando la operación. La provisión manual por consola sirve para preparar pruebas, pero no es el flujo de producto solicitado.

## Matriz de requisitos de superadmin

| Requisito del proyecto | Situación encontrada | Aceptación pendiente |
|---|---|---|
| Acceso según rol | Auth + users/{uid}, rol/active y guard de superadmin | Login válido, perfil ausente, rol incorrecto, sesión revocada |
| Gestión de usuarios | Listado, detalle y estado remotos | Conteo con perfiles reales, búsquedas/filtros y cambios entre dos sesiones |
| Registrar administradores | No se encontró alta HOTEL_ADMIN desde superadmin | Crear cuenta/perfil sin cerrar sesión del actor y sin permitir elevación pública |
| Registrar y gestionar hoteles | Alta/edición/estado e IDs transaccionales | Guardado/lectura en otro teléfono, validaciones y contador concurrente |
| Asignar administrador | Transacción actualiza ambos documentos | Asignar, sustituir, desvincular, impedir doble asignación y rechazar inactivos |
| Activar/desactivar cliente/admin/taxista | users.active y logs implementados | Completar coherencia operativa del taxista y probar autorización en servidor |
| Reportes de reservas por hotel | Filtros diario/mensual/anual y cálculo remoto | Compatibilidad de estados, reservas omitidas, varias habitaciones y cambios en vivo |
| Reportes de ventas | Valor de estadías prorrateado por noches | Acordar criterio de venta frente a cobro final; no confundir precioTotal con comprobante de checkout |
| Logs de eventos | Eventos de gestión de usuarios/hoteles en transacción | Verificación de actor/fecha/entidad y definición del alcance de eventos de otros roles |
| Aprobar y gestionar taxistas en web | No se encontró API REST integrada en Android revisado | Coordinar con responsable web: alta, aprobación, cambios de estado y contrato API |

El reporte actual etiqueta sus importes como valor de estadías; no se encontró lectura de hotelCheckouts en FirebaseAdminReadRepository. Si se presenta como dinero cobrado, debe usar los comprobantes y distinguir alojamiento, extras y cancelaciones.

## Dependencias del proyecto completo

Esta revisión de superadmin no certifica los otros módulos ni sustituye la entrega final. El plan también exige:

- Cliente: registro con datos personales, consulta, reservas sin superposición de cliente/habitación, tarjeta simulada, checkout con valoración/observación, alerta de cobro e historial.
- Administrador: ubicación y lugares cercanos, al menos cuatro fotografías del hotel, habitaciones con capacidad/área, servicios con precio e imágenes, avisos de checkout, cobro con extras justificados, reporte de servicios y chat privado persistente.
- Taxi: alta/aprobación en web, datos personales/vehículo/fotos, asignación exclusiva, estados del viaje, ubicación automática durante el servicio, QR para finalizar y valoración.
- Integración web: API REST para disponibilidad, datos del conductor, estado del servicio y ubicación; Firebase por sí solo no demuestra que exista esa API.
- Entrega del sistema: despliegue, código y esquema de datos, arquitectura, costos/OPEX y manuales de instalación/usuario.

Los mocks y ejemplos locales no demuestran cumplimiento remoto. Las referencias a imágenes empaquetadas no prueban que una foto nueva esté compartida.

## Orden de trabajo propuesto

1. Consolidar la matriz de requisitos y el contrato de estados/campos/permisos con las ramas remotas.
2. Preparar la integración manteniendo menú inferior, fondo blanco, Material Design y catálogo existentes.
3. Corregir compatibilidad de reportes y caché, sin renumerar hoteles ni perder reservas antiguas.
4. Alinear activación/aprobación de taxistas con el responsable de la web/API.
5. Implementar el alta segura de administradores y su asignación.
6. Integrar el contrato de imágenes AWS en superadmin y administrador, conservar referencias compatibles en Firestore y verificar su lectura desde cliente. Revisar las restricciones gs:// de las reglas nuevas antes de adoptar referencias S3.
7. Ejecutar pruebas locales de reglas y luego recorridos reales con registros identificables de prueba.
8. Publicar únicamente la versión de reglas que corresponda al código integrado, después de revisarla; conservar el texto aportado como referencia de la configuración actual.

## Recorridos de aceptación posteriores

- Usuarios: una cuenta Auth sin users no aparece como perfil válido; un cliente registrado por la app aparece una sola vez. Desactivar y reactivar una cuenta de prueba y verificar acceso en un segundo dispositivo.
- Hoteles: alta válida, campos inválidos, dirección escrita y pin del mapa, edición, activación, desactivación y visualización por el cliente.
- Asignaciones: comprobar users.hotelId y hotels.administratorUid en cada cambio; impedir doble hotel por administrador y acceso a hotel ajeno.
- Fotos: subir JPEG, abrir la galería desde otro teléfono y comprobar rechazo por rol, tamaño y tipo; cumplir el mínimo de cuatro fotos del módulo administrador.
- Reportes: hotel específico/todos y día/mes/año; incluir confirmadas, en checkout, finalizadas y canceladas; contrastar cantidades, importes y ocupación con documentos conocidos.
- Logs: cada operación de gestión realizada por los repositorios debe registrar actor, acción, entidad y hora del servidor junto al cambio.
- Taxi: aprobación, activación/desactivación y disponibilidad son decisiones diferentes; comprobar su efecto tanto por la app como por peticiones al backend/reglas.
- Interacción entre roles: el admin ve el hotel asignado y el cliente el mismo catálogo; los cambios nuevos de checkout/chat no deben romper reportes de superadmin.

## Información de consola aún necesaria

1. hotels/9: active y administratorUid, para contrastarlos con el perfil administrador ya recibido.
2. Una reserva EN_CHECKOUT o FINALIZADA y, si existe, su hotelCheckouts; ya se recibió un ejemplo CONFIRMADA.
3. Contrato del servicio de imágenes API Gateway/Lambda/S3: URL base, métodos, autenticación y ejemplos de respuesta sin secretos. No hace falta enviar configuración de Firebase Storage.
4. Contrato o URL de la API REST de taxistas, si el responsable web ya la tiene.

No se necesitan contraseñas ni claves privadas para continuar la revisión estática. Las pruebas con cuentas se prepararán después, con el alcance de escritura definido.
