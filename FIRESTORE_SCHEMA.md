# Contrato Firestore para el módulo cliente

Base: `(default)` del proyecto `iot-telito`. Las claves de documento de hotel
conservan los IDs numéricos usados por la aplicación Android: `hotels/1`, etc.

## Datos que el cliente consulta

La creación y edición de hoteles y habitaciones corresponde al módulo de
administración del equipo. El cliente solo los lee. Mientras ese módulo no
publique datos en Firestore, el catálogo del cliente conserva su fuente local
para que la búsqueda siga funcionando.

`hotels/{hotelId}`:

| Campo | Tipo | Uso |
|---|---|---|
| `id` | número entero | ID del hotel, igual al documento |
| `name`, `city`, `address`, `description` | texto | Búsqueda y detalle |
| `active`, `hasInventory` | booleano | Visibilidad y botón de reserva |
| `pricePerNight`, `rating` | número | Filtros y tarjeta |
| `reviews` | número entero | Detalle |
| `services` | arreglo de textos | Servicios del hotel |

`hotels/{hotelId}/rooms/{roomId}` (contrato a coordinar): `id`, `hotelId`,
`tipo`, `nombreCodigo`, `areaM2`, `adultos`, `ninos`, `precioNoche`,
`descripcion`, `disponible`. El ID de habitación representa una unidad física,
no solo un tipo. La consulta de disponibilidad por fechas vendrá con reservas.

## Datos que el cliente crea y consulta

`reservations/{reservationId}`: `id`, `clientUid`, `hotelId`, `roomIds`,
`roomName`, `roomNumber`, `fechaEntrada`, `fechaSalida`, `huespedes`,
`precioTotal`, `estado`, `codigo`, `createdAt`. Fechas ISO `AAAA-MM-DD`;
el intervalo es `[fechaEntrada, fechaSalida)`, por lo que el día de salida ya
puede reservarse como entrada de otra estadía.

`roomNights/{hotelId}_{roomId}_{fecha}` y `clientNights/{uid}_{fecha}` son
bloqueos por noche creados en la misma transacción que la reserva. Una reserva
se rechaza si ya existe cualquier bloqueo de habitación o de cliente para las
noches solicitadas. Los bloqueos históricos se conservan; el checkout no los
elimina.
Las imágenes del catálogo siguen siendo recursos locales de demostración.

`firestore.rules` permite leer el catálogo con una cuenta autenticada y crear
reservas propias, pero mantiene cerradas las escrituras de hoteles. Antes de que el módulo
administrador escriba en Firestore, el equipo tendrá que integrar sus reglas
de rol con estas reglas compartidas.
