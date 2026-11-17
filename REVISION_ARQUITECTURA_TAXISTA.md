# Revisión del proyecto y arquitectura de taxistas

Fecha: 16 de noviembre de 2026.

## Alcance y límites

Revisión de código y documentación guardados en este repositorio. No se consultaron las bases reales, no se ejecutaron pruebas nuevas, no se publicaron reglas y no se cambiaron ramas ni código. Las referencias origin/* son las disponibles localmente; no se hizo fetch para comprobar si GitHub tiene cambios posteriores.

- Checkout abierto: `feat/hotel-admin`, commit `eadbc00`. Corresponde al avance local con Room.
- Integración Firebase revisada: `origin/ultima_03_11`, commit `8e90c52`.
- Web revisada: `origin/web`, commit `96a287e`.
- Enunciado original: PDF conservado en `origin/web`, extraído sin cambios a `tmp/revision_arquitectura/plan_proyecto.pdf`.

## Conclusión

El problema principal es la integración pendiente entre la web de taxistas y la app. El taxista se registra y aprueba en MySQL, pero su acceso operativo se decide con documentos independientes en Firestore. No se encontró un enlace entre esas dos identidades ni una API de dominio taxi consumida por Android.

Usar un único proyecto Firebase para los roles no constituye por sí mismo un error. Tampoco hace falta trasladar clientes y reservas a MySQL. Sí hace falta cumplir el límite de integración exigido por el enunciado: las operaciones relacionadas con taxistas deben pasar por la API REST de la web.

## Lo que exige el enunciado

- Páginas 5 y 6: aplicación móvil principal y web independiente de registro/gestión de taxistas; la web debe proporcionar API REST.
- Página 6: registro, aprobación, información, estado y valoración promedio; la API expone taxistas disponibles, datos del asignado, estado del servicio y ubicación.
- Páginas 8 y 9: el conductor utiliza la app después de ser aprobado; asignación exclusiva, GPS durante el servicio y finalización mediante QR.
- Página 9: el cliente obtiene la información del taxista mediante la API de gestión.
- Página 10: persistencia NoSQL para la app principal; ninguna conexión directa móvil a la base de la web. Toda consulta o actualización relacionada con taxistas se realiza mediante API REST.

El PDF no impone explícitamente MySQL como motor. MySQL es la elección que ya tiene la web del equipo.

## Estado por área

| Área | Evidencia revisada | Estado y límite |
|---|---|---|
| App abierta | Repositorios de la rama actual | Persistencia local; no representa el avance Firebase posterior. |
| Identidad Firebase | `users/{uid}`, login integrado | Los cuatro roles se resuelven con perfil y estado activo; taxista además consulta `taxistas/{uid}`. |
| Cliente | Contratos y repositorios de la integración | Catálogo, inventario, reservas y solicitudes remotas implementados. No se certifica su estado actual en la nube. |
| Superadmin móvil | Guías de integración y pruebas previas | Usuarios, hoteles, asignaciones, reportes y logs con integración Firestore. No sustituye la gestión web de taxistas. |
| Administrador | `FIREBASE_ADMIN_OPERACIONES_03_11.md` y llamadas de sus pantallas | Inventario y perfil remotos; también existen adaptaciones posteriores para reservas, chat y cierre administrativo. La guía de operaciones deja publicación y comprobación real pendientes. No es correcto afirmar que todo sigue local ni que todo está validado en producción. |
| Taxista móvil | `FirebaseTaxiStore`, `ConductorRepository`, `DisponibilidadRepository` | Solicitudes, aceptación, cinco estados, disponibilidad e historial sobre Firestore. Asignación y bloqueo de un viaje activo se actualizan con transacciones. Acceso directo a Firestore, sin API de la web. |
| Web | Spring Boot, JPA, conector MySQL, modelos y controladores | Registro, listados, aprobación/rechazo y disponibilidad implementados. No se encontró la API REST de integración de dominio taxi; los controladores sirven principalmente vistas HTML. |
| Perfil taxista | `PerfilTaxistaFragment` en integración Firebase | DNI, teléfono, modelo, placa, color y 4.8 siguen escritos como ejemplos en la interfaz. Nombre/correo de sesión no equivalen a perfil completo integrado. |
| Calificación | `CalificarTaxiActivity` | Selecciona estrellas, pero Enviar navega al inicio; no persiste la valoración. La web tiene campo de promedio, sin flujo revisado que lo calcule desde estas calificaciones. |
| QR | `FinalizarServicioActivity` | Valida un código visible `TAXI-{id}` de demostración. No es lectura/validación real del QR. |
| Chat taxista y avisos | Repositorio y contrato taxi | Mensajes y avisos locales; no se encontró integración FCM para avisos remotos con app cerrada. |
| Seguimiento | Contratos y guías taxi | GPS/ubicación real periódica todavía se documenta pendiente. |

Los informes de la rama registran pruebas previas del recorrido Firebase cliente → taxista → finalizado. Son evidencia histórica del equipo, no pruebas repetidas en esta revisión ni validación de la API web.

## Riesgos concretos

1. **Dos aprobaciones independientes.** `TaxistaService.aprobarTaxista` activa el usuario MySQL. La app verifica `users.active` y `taxistas.activo` en Firestore. Aprobar o rechazar en una parte no actualiza la otra.
2. **Disponibilidad duplicada.** Hay `Taxista.disponible` en MySQL y `taxistas/{uid}.disponible` en Firestore sin mecanismo de coordinación encontrado. Además, aprobar en la web pone disponible=true, mezclando aprobación con decisión de trabajar.
3. **Falta de identidad compartida.** La entidad web usa ID numérico; la app usa UID Firebase. La entidad revisada no contiene `firebaseUid`. El correo no debería ser la única relación permanente.
4. **Incumplimiento de la vía REST.** Android consulta perfiles y modifica operaciones taxi directamente en Firestore. Añadir solo un endpoint de listado no cubriría el resto del requisito.
5. **Gestión web aún demostrativa.** El controlador superadmin obtiene un correo administrativo fijo como usuario actual y no se encontró configuración de autenticación/autorización web en el árbol revisado. Debe resolverse antes de exponer aprobaciones por API.
6. **Contraseña web provisional.** El registro guarda una cadena UUID como password sin aplicar un hash en ese flujo. Antes de habilitar autenticación propia, debe eliminarse ese esquema o sustituirse por un mecanismo seguro. No duplicar contraseñas si se conserva Firebase Auth.

## Reparto propuesto

Esta es una propuesta de arquitectura, no una descripción de lo que ya está implementado.

| Dato | Autoridad propuesta |
|---|---|
| Credenciales e identidad autenticada | Firebase Authentication. |
| Clientes, hoteles y reservas | Firestore, conservando el avance del equipo. |
| Expediente del taxista, documento, licencia, vehículo y aprobación | MySQL, administrado por la web/API. |
| Solicitud, asignación, estados e historial del servicio | Puede conservarse el modelo Firestore existente, con operaciones y consultas taxi mediadas por la API. |
| Disponibilidad y ubicación | Un solo dueño operativo detrás de la API; no dos booleanos editables independientes. |
| Valoraciones individuales y promedio | Flujo único por API, ligado al viaje finalizado; la web obtiene su promedio de esas valoraciones. |
| Copia local | Room como caché/almacenamiento auxiliar, sin decidir aprobación remota. |

La API puede consultar MySQL y Firestore. Tener datos en dos motores no obliga a que cliente y conductor operen sobre copias independientes del mismo viaje.

## Flujo objetivo

1. Taxista se registra en la web; MySQL conserva su expediente como pendiente.
2. Superadmin aprueba. El backend vincula de forma controlada ese registro con un UID Firebase y habilita el acceso. Si falla una parte, queda pendiente de reintento y no aparece aprobado en la app por accidente.
3. Taxista inicia sesión con Firebase Auth. Android envía su ID token a la API. El backend verifica identidad, rol y aprobación; no confía en un ID enviado libremente por el teléfono.
4. Cliente solicita taxi para una reserva propia. Se conserva la reserva en Firestore y la solicitud mantiene su relación con ella. La creación y lectura del dominio taxi pueden canalizarse también por la API para respetar el requisito completo.
5. Taxista obtiene pedidos y acepta por API. El backend valida aprobación y disponibilidad y realiza la asignación exclusiva junto con el bloqueo de viaje activo.
6. Estados, ubicación, QR y valoración se procesan por API. Cliente y hotel reciben el mismo viaje y perfil autorizado, sin copiar toda la base de clientes a MySQL.

Ejemplo de relación: reserva Firestore 123 → solicitud 123 → taxista MySQL 7, vinculado al UID Firebase del conductor. Esa relación basta para unir ambos dominios.

## Secuencia de implementación recomendada

1. Elegir la rama Firebase como base de trabajo y conservar el avance local actual. No cambiar de rama ni mezclar sin revisar las diferencias.
2. Acordar identidad `firebaseUid ↔ taxistaId`, dueño de disponibilidad/ubicación y contrato REST.
3. Implementar autenticación/autorización de la API y el vínculo de registro/aprobación/rechazo.
4. Conectar perfil real y habilitación de Android; retirar documentos taxista creados manualmente como único mecanismo de alta.
5. Trasladar a la API el acceso a solicitudes, aceptación y transiciones manteniendo el modelo de viaje existente donde sea útil.
6. Integrar GPS, QR y calificaciones persistentes; actualizar perfil y reportes desde datos reales.
7. Verificar registro web → aprobación → login app → pedido cliente → aceptación exclusiva → recorrido → QR → valoración. Probar también rechazo/desactivación, API caída y reintentos.

No existe una transacción automática que abarque MySQL y Firestore. El diseño debe tratar los fallos parciales con operaciones idempotentes, reintentos y una política explícita ante desactivaciones concurrentes. No duplicar el viaje como dos registros editables independientes.

## Referencias de implementación

- `origin/ultima_03_11:TAXI_FIREBASE_CONTRATO.md`
- `origin/ultima_03_11:TAXISTA_FIREBASE.md`
- `origin/ultima_03_11:app/src/main/java/com/example/proyecto_iotelito/data/taxista/FirebaseTaxiStore.java`
- `origin/ultima_03_11:app/src/main/java/com/example/proyecto_iotelito/data/taxista/DisponibilidadRepository.java`
- `origin/ultima_03_11:app/src/main/java/com/example/proyecto_iotelito/ui/taxista/PerfilTaxistaFragment.java`
- `origin/ultima_03_11:app/src/main/java/com/example/proyecto_iotelito/ui/taxi/CalificarTaxiActivity.java`
- `origin/ultima_03_11:FIREBASE_ADMIN_OPERACIONES_03_11.md`
- `origin/ultima_03_11:RESULTADOS_PRUEBAS_REALES_03_11.md`
- `origin/web:src/main/java/com/iotelito/taxiweb/service/TaxistaService.java`
- `origin/web:src/main/java/com/iotelito/taxiweb/controller/SuperadminController.java`
- `origin/web:src/main/java/com/iotelito/taxiweb/model/Taxista.java`

Documentación oficial contrastada: [verificación de ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens) y [transacciones Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions). El backend puede verificar tokens Firebase y efectuar operaciones transaccionales Firestore; esa transacción no incluye MySQL.
