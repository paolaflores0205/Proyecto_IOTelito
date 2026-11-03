# Solicitudes y viajes remotos del taxista

## Alcance e integración con cliente

La integración usa el proyecto Firebase existente. `taxiRequests/{id}` representa
una solicitud por reserva; `id` es el mismo entero global de la reserva remota en
`reservations/{id}`. No se crean contadores globales ni se cambian las firmas de
los métodos que utilizan las pantallas del cliente.

Los pedidos que el cliente ya tenía guardados en Room siguen leyéndose desde esa
base en su instalación original. Consultar o volver a solicitar taxi desde esa
reserva recupera el pedido previo. No se suben ni se borran automáticamente. Para
cuentas Firebase sin pedido local previo, las solicitudes nuevas usan Firestore.

El cambio compartido está en cuatro métodos de `TaxiRepository`: crear, buscar,
observar y buscar por reserva. Si la cuenta está autenticada en Firebase, utilizan
`data/taxista/FirebaseTaxiStore`. El almacenamiento Room y los recorridos de las
cuentas locales de demostración conservan su implementación anterior.

Las cuentas Firebase de taxista cargan solicitudes reales y sus propios viajes desde
Firestore. Las listas ya no se rellenan con ejemplos locales. Room conserva una copia
para los detalles, mensajes y avisos locales. El historial remoto se recupera también
en una instalación nueva: no depende de que el viaje exista previamente en Room.

## Documentos

`taxiRequests/{id}`:

| Campo | Tipo | Contenido |
|---|---|---|
| id, reserva | integer | ID de la reserva; igual al ID del documento |
| clientUid | string | UID del cliente autenticado |
| cliente, nombre | string | Correo y nombre del cliente |
| hotel, origen, destino | string | Datos existentes de la solicitud |
| hora | string | Hora local de recojo, HH:mm |
| pasajeros, maletas | integer | Cantidades seleccionadas por cliente |
| estado | string | SOLICITADO, ASIGNADO, EN_CAMINO, EN_TRASLADO o FINALIZADO |
| creado | timestamp | Hora del servidor al crear |
| taxistaUid | string | Vacío al crear; UID del conductor al aceptar |
| taxista, conductor | string | Correo y nombre del conductor al aceptar |
| finalizado | null/timestamp | Hora del servidor al finalizar |

`taxiDrivers/{UID}` contiene `activeRequestId`: ID del viaje activo como string,
o string vacío después de finalizar. No debe crearse a mano para las cuentas de prueba.
La app lo crea al aceptar la primera solicitud.

Aceptar actualiza solicitud y bloqueo en la misma transacción. Las reglas impiden
que otro conductor tome el mismo pedido o que una cuenta tome dos servicios activos,
incluso desde dispositivos diferentes. Finalizar actualiza el viaje y libera el bloqueo
de manera atómica. Apagar disponibilidad impide nuevas aceptaciones; permite continuar
el viaje ya asignado. Se exige que la cuenta siga activa.

El cliente mantiene sus cuatro pasos actuales. Para esa pantalla, EN_TRASLADO se
muestra como EN_CAMINO; Firestore y el panel taxista conservan el estado completo.

## Publicar reglas

En el archivo `firestore.rules`, la sección que comienza en `// Integración de taxi`
incluye la función `taxiDriver` y los bloques `taxiRequests` y `taxiDrivers`.
Agregar esa sección dentro de `/databases/{database}/documents`, antes del bloque
final de denegación. Conservar los bloques de hoteles, reservas, bloqueos de noches
y `taxistas` que ya tiene el equipo. Pulsar Publicar en Firebase Console.

El archivo local contiene las reglas integradas, pero no se publica automáticamente.
Si otro integrante cambió las reglas remotas, integrar únicamente la sección nueva.
Las consultas usan un filtro de igualdad por consulta y no requieren índices compuestos.

## Prueba completa con cliente y taxista

1. Publicar reglas y ejecutar la app actualizada en ambos dispositivos o emuladores.
2. Cliente entra con su cuenta Firebase. Desde una reserva remota propia, sigue el
   recorrido existente de beneficio y solicitud de taxi. Una reserva local de ejemplo
   no se publica automáticamente en Firebase.
3. Comprobar que se crea `taxiRequests/{idReserva}` en Firestore. Volver a abrir la
   misma solicitud debe recuperar ese documento, sin crear otro.
4. Taxista entra con su cuenta Firebase, pone Disponible y abre Solicitudes.
5. Aceptar el pedido. En Firestore comprobar estado ASIGNADO, taxistaUid correcto
   y el mismo ID en `taxiDrivers/{UID}.activeRequestId`.
6. Verificar que el cliente ve conductor asignado. Si se usa un solo teléfono,
   cerrar sesión y cambiar de cuenta; la solicitud permanece en Firestore.
7. Taxista inicia recorrido y pasa por EN_CAMINO y EN_TRASLADO utilizando las
   pantallas existentes. En Finalizar, usar el código de demostración TAXI-{id}
   que muestra la pantalla y confirmar. El lector QR real sigue pendiente.
8. Comprobar FINALIZADO, timestamp finalizado y bloqueo vacío. El servicio debe
   aparecer en Historial y el cliente debe ver Finalizado.
9. Reabrir con la cuenta taxista: el historial debe reaparecer desde Firebase.
10. Con otra cuenta taxista activa y disponible, intentar aceptar el mismo pedido:
    solo una cuenta debe obtenerlo. Una cuenta con viaje activo no puede aceptar otro.

## Prueba independiente del taxista desde la consola

Para comprobar tu módulo sin pedir a otro integrante que cree una reserva, se puede
crear un documento administrativo de demostración en Firestore. La consola permite
crear este dato como administrador; las reglas de la app no permiten que un cliente
cree pedidos sin reserva propia.

En `taxiRequests`, crear un documento con ID `2000000001` (si ya existe, usar otro
entero libre menor de 2147483647). Usar los mismos números para id y reserva:

| Campo | Tipo en consola | Valor de prueba |
|---|---|---|
| id, reserva | number | 2000000001 |
| clientUid | string | cliente-demo-taxista |
| cliente | string | cliente-demo@example.test |
| nombre | string | Pasajero de prueba |
| hotel | string | Hotel de prueba |
| origen | string | Av. de prueba 123 |
| destino | string | Aeropuerto Jorge Chávez |
| hora | string | 23:30 |
| pasajeros | number | 2 |
| maletas | number | 1 |
| estado | string | SOLICITADO |
| creado | timestamp | Fecha y hora actuales |
| taxistaUid, taxista, conductor | string | Dejar vacío cada campo |
| finalizado | null | null |

Probar el recorrido del conductor con ese documento. Esos datos no representan una
reserva ni un cliente real. No permiten verificar la pantalla cliente; para esa parte
usar el recorrido completo anterior. Los avisos en segundo plano y el chat entre
teléfonos requieren otra integración: este avance no añade FCM ni mensajes remotos.

## Pruebas automatizadas

- `bash gradlew :app:assembleDebug :app:testDebugUnitTest`: compilación y pruebas Java.
- `python3 tools/test_taxi_firestore_rules.py --jar /ruta/cloud-firestore-emulator.jar --java /ruta/java`:
  pruebas de reglas contra el emulador oficial (Java 21+). Usa un proyecto demo,
  puerto local temporal y datos ficticios; detiene el proceso al terminar.

Se verifican creación con reserva propia, lectura por cuenta, consultas permitidas,
disponibilidad, restricciones de campos, pasos del viaje, liberación de bloqueo,
dos conductores disputando un pedido y dos pedidos disputados por el mismo conductor.
Estas pruebas no sustituyen la comprobación visual en Android ni publican reglas.

La revisión de compatibilidad comprueba también el acceso del cliente sin documento
taxista y conserva los permisos previos de hoteles y reservas. Los pedidos locales
anteriores del cliente se mantienen accesibles en su instalación original y se
comprueba que un pedido de otra cuenta no se use como sustituto del remoto.

Limitación de la batería Android heredada: `ClientTaxiNavigationTest.java`, línea 53,
llama a `agregarReserva` sin el argumento `List<Integer> roomIds`. Esa incompatibilidad
ya existe en el commit de partida de esta rama; impide compilar `assembleDebugAndroidTest`.
No se cambió ese test ni el repositorio de reservas durante el avance taxista. Por
ello, la revisión no equivale a ejecutar todos los recorridos visuales de los roles.
