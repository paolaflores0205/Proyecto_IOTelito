# Reservas consecutivas y noches del cliente

## Regla funcional

Se permiten varias reservas en distintos hoteles si no se superponen las noches de reservas
vigentes. Una estadía del 16 al 17 y otra del 17 al 19 son compatibles, incluso con el primer
checkout pendiente. La salida no ocupa una noche. Una reserva CONFIRMADA, ACTIVA, EN_CHECKOUT
(o PENDIENTE_PAGO anterior) sí bloquea sus noches; una FINALIZADA, COMPLETADA o CANCELADA no
mantiene un bloqueo personal activo.

Antes de confirmar se verifica el documento que originó cada bloqueo existente. Solo se puede
reasignar un bloqueo propio si su reserva ya no existe, está cerrada/cancelada o esa noche queda
fuera de su rango real. No se borran reservas ni se reabren comprobantes. Los documentos de
ocupación de habitaciones (roomNights) se conservan: esto no permite ocupar una habitación que
ya está reservada, aunque la prueba use fechas anteriores a hoy.

## Activación manual en Firestore

1. Guardar una copia nueva de las reglas publicadas como firestore-publicadas-antes-reservas-consecutivas.txt.
2. En Firestore > Reglas buscar `match /clientNights/{nightId} {`.
3. Comparar el bloque con el equipo y reemplazar SOLO ese bloque hasta antes de
   `match /roomNights/{nightId} {` por el siguiente. Mantener el bloque roomNights y todas las demás
   reglas. Si hubo cambios del equipo en clientNights desde el último respaldo, integrarlos primero.
4. Revisar y publicar personalmente. No cambiar datos, roles, fechas ni eliminar bloqueos a mano.

```text
    match /clientNights/{nightId} {
      function validClientNight() {
        let next = getAfter(/databases/$(database)/documents/reservations/$(request.resource.data.reservationId)).data;
        let date = nightId.split(request.auth.uid + '_')[1];
        return request.resource.data.keys().hasOnly(['clientUid','reservationId'])
          && request.resource.data.clientUid == request.auth.uid
          && next.clientUid == request.auth.uid
          && next.estado in ['CONFIRMADA','ACTIVA']
          && date >= next.fechaEntrada && date < next.fechaSalida;
      }
      function reusableNight() {
        let path = /databases/$(database)/documents/reservations/$(resource.data.reservationId);
        let date = nightId.split(request.auth.uid + '_')[1];
        return !exists(path) || (get(path).data.clientUid == request.auth.uid
          && (get(path).data.estado in ['FINALIZADA','COMPLETADA','CANCELADA']
            || date < get(path).data.fechaEntrada || date >= get(path).data.fechaSalida));
      }
      allow get: if activeUser()
        && nightId.matches(request.auth.uid + '_.*');
      allow list: if false;
      allow create: if activeUser() && profile().role == 'CLIENT'
        && nightId.matches(request.auth.uid + '_.*')
        && validClientNight();
      allow update: if activeUser() && profile().role == 'CLIENT'
        && nightId.matches(request.auth.uid + '_.*') && resource.data.clientUid == request.auth.uid
        && validClientNight() && reusableNight();
      allow delete: if false;
    }
```

## Prueba con la app

- Reserva A del 16 al 17; reserva B del 17 al 19: permitida si la habitación elegida está disponible.
- Repetir con A en checkout pendiente: también debe permitir B.
- Intentar B del 16 al 18 con A vigente: rechazada con las fechas y la noche que coincide.
- Reserva A finalizada: no debe bloquear personalmente una reserva nueva. Las habitaciones siguen
  respetando sus noches ya ocupadas; cambiar de habitación/hotel o elegir noches libres si corresponde.
- No se completan automáticamente las reservas vencidas que todavía no hicieron checkout.

La tarjeta sigue siendo obligatoria al confirmar; checkout usa la misma tarjeta guardada.
Las fechas pasadas siguen habilitadas temporalmente para la demostración.
