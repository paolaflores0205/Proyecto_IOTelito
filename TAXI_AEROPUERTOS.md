# Aeropuerto según la ubicación del hotel

## Comportamiento

La pantalla usa la ciudad del hotel cargado de Firestore, no el nombre comercial ni la ubicación del teléfono.
El catálogo actual cubre Lima/Callao (LIM), Cusco/Cuzco (CUZ), Arequipa (AQP), Trujillo (TRU) y
Paracas/Pisco (PIO). No se asigna un aeropuerto a ciudades desconocidas: se muestra un aviso y se
deshabilita Confirmar. Actualmente hay un destino por zona; se muestra su nombre sin aparentar un
selector con alternativas inexistentes. La estructura permite incorporar más destinos después.
El pasajero debe comprobar que el aeropuerto coincide con su vuelo. Esto no consulta vuelos ni
promete que un aeropuerto tenga vuelos comerciales disponibles en una fecha.

## Contrato para la integración de taxi

Se conservan los campos anteriores, incluidos id/reserva, clientUid, hotel, origen, destino y hora.
Se agregan:

| Campo | Significado |
|---|---|
| hotelId | ID del hotel de la reserva |
| hotelCity | Ciudad del hotel al solicitar |
| airportCode | Código IATA estable del destino, por ejemplo CUZ |
| airportName | Nombre canónico del aeropuerto |
| airportCity | Ciudad del aeropuerto |

`destino` contiene el nombre y código, por ejemplo `Aeropuerto Internacional Alejandro Velasco Astete (CUZ)`.
Las pantallas de cliente y taxista que ya consultan destino siguen recibiendo texto. Las solicitudes
anteriores no se reescriben ni se les inventa un código. Room migra de versión 1 a 2 sin borrar datos.

Para mapas/API usar airportCode para resolver el punto de acceso al terminal y hotelId para consultar
las coordenadas del hotel. No se agregan coordenadas inventadas ni se usa la pista como punto de
recogida. Esa resolución pertenece a la integración de rutas de la compañera. En Lima se debe usar
el acceso del terminal nuevo por Morales Duárez, no asumir el antiguo acceso de Faucett.

## Publicación manual de reglas

No se publicaron reglas ni se modificaron documentos reales desde esta implementación.
Las reglas anteriores solo aceptan Jorge Chávez, por lo que una solicitud nueva con estos campos
no podrá guardarse hasta publicar el ajuste.

1. Firebase > proyecto iot-telito > Firestore Database > Reglas.
2. Guardar un respaldo nuevo de TODO lo publicado, sin sobrescribir respaldos anteriores.
3. Buscar `match /taxiRequests/{requestId} {` y delimitar su bloque completo hasta antes de
   `match /taxiDrivers/{uid} {`. No seleccionar taxiDrivers ni el resto de reglas.
4. Comparar ese bloque con la versión del equipo. Si alguien cambió permisos de taxi mientras tanto,
   integrar esos cambios antes de sustituirlo. Si coincide con el bloque anterior conocido, reemplazarlo
   por el bloque siguiente. Conservar todas las demás reglas del proyecto.
5. Revisar y pulsar Publicar personalmente. No modificar Storage, Lambda ni el bucket para este bloque.

```text
    match /taxiRequests/{requestId} {
      function airportForCity(city, code) {
        let value = city.lower().trim();
        return (code == 'LIM' && value in ['lima','lima metropolitana','callao'])
          || (code == 'CUZ' && value in ['cusco','cuzco'])
          || (code == 'AQP' && value == 'arequipa')
          || (code == 'TRU' && value == 'trujillo')
          || (code == 'PIO' && value in ['paracas','pisco']);
      }
      function validAirport() {
        let data = request.resource.data;
        let booking = get(/databases/$(database)/documents/reservations/$(requestId)).data;
        let hotel = get(/databases/$(database)/documents/hotels/$(string(booking.hotelId))).data;
        return data.hotelId == booking.hotelId && hotel.id == data.hotelId && hotel.active == true
          && data.hotelCity == hotel.city && data.hotel == hotel.name && data.origen == hotel.address
          && airportForCity(hotel.city, data.airportCode)
          && data.destino == data.airportName + ' (' + data.airportCode + ')'
          && ((data.airportCode == 'LIM' && data.airportName == 'Aeropuerto Internacional Jorge Chávez' && data.airportCity == 'Callao')
            || (data.airportCode == 'CUZ' && data.airportName == 'Aeropuerto Internacional Alejandro Velasco Astete' && data.airportCity == 'Cusco')
            || (data.airportCode == 'AQP' && data.airportName == 'Aeropuerto Internacional Alfredo Rodríguez Ballón' && data.airportCity == 'Arequipa')
            || (data.airportCode == 'TRU' && data.airportName == 'Aeropuerto Internacional Capitán FAP Carlos Martínez de Pinillos' && data.airportCity == 'Trujillo')
            || (data.airportCode == 'PIO' && data.airportName == 'Aeropuerto Capitán FAP Renán Elías Olivera' && data.airportCity == 'Pisco'));
      }
      function validDestination() {
        let data = request.resource.data;
        let keys = ['hotelId','hotelCity','airportCode','airportName','airportCity'];
        // Compatibilidad con las versiones anteriores de las pantallas de taxi.
        return (!data.keys().hasAny(keys) && data.destino == 'Aeropuerto Jorge Chávez')
          || (data.keys().hasAll(keys) && validAirport());
      }
      function ownsReservation() {
        return exists(/databases/$(database)/documents/reservations/$(requestId))
          && get(/databases/$(database)/documents/reservations/$(requestId)).data.clientUid == request.auth.uid;
      }
      function driverLockAfter() {
        return getAfter(/databases/$(database)/documents/taxiDrivers/$(request.auth.uid)).data.activeRequestId;
      }
      allow get: if request.auth != null && (
        (resource == null && (ownsReservation() || taxiDriver()))
        || (resource != null && (resource.data.clientUid == request.auth.uid
          || (taxiDriver() && (resource.data.estado == 'SOLICITADO'
            || resource.data.taxistaUid == request.auth.uid)))));
      allow list: if request.auth != null && (resource.data.clientUid == request.auth.uid
        || (taxiDriver() && (resource.data.estado == 'SOLICITADO'
          || resource.data.taxistaUid == request.auth.uid)));
      allow create: if request.auth != null && ownsReservation()
        && request.resource.data.keys().hasOnly(['id','reserva','clientUid','cliente','nombre','hotel',
             'origen','destino','hora','pasajeros','maletas','estado','creado','taxistaUid','taxista','conductor','finalizado',
             'hotelId','hotelCity','airportCode','airportName','airportCity'])
        && request.resource.data.id is int && request.resource.data.id > 0
        && requestId == string(request.resource.data.id)
        && request.resource.data.reserva == request.resource.data.id
        && request.resource.data.clientUid == request.auth.uid
        && request.resource.data.cliente == request.auth.token.email
        && request.resource.data.nombre is string && request.resource.data.nombre.size() > 0
        && request.resource.data.hotel is string && request.resource.data.origen is string
        && validDestination()
        && request.resource.data.hora is string
        && request.resource.data.hora.matches('([01][0-9]|2[0-3]):[0-5][0-9]')
        && request.resource.data.pasajeros is int && request.resource.data.pasajeros >= 1
        && request.resource.data.maletas is int && request.resource.data.maletas >= 0
        && request.resource.data.estado == 'SOLICITADO'
        && request.resource.data.taxistaUid == '' && request.resource.data.taxista == ''
        && request.resource.data.conductor == '' && request.resource.data.finalizado == null
        && request.resource.data.creado == request.time;
      allow update: if taxiDriver() && (
        (resource.data.estado == 'SOLICITADO' && resource.data.taxistaUid == ''
          && request.resource.data.estado == 'ASIGNADO'
          && get(/databases/$(database)/documents/taxistas/$(request.auth.uid)).data.disponible == true
          && request.resource.data.taxistaUid == request.auth.uid
          && request.resource.data.taxista == request.auth.token.email
          && request.resource.data.conductor is string && request.resource.data.conductor.size() > 0
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['estado','taxistaUid','taxista','conductor'])
          && driverLockAfter() == requestId)
        || (resource.data.taxistaUid == request.auth.uid
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['estado'])
          && ((resource.data.estado == 'ASIGNADO' && request.resource.data.estado == 'EN_CAMINO')
            || (resource.data.estado == 'EN_CAMINO' && request.resource.data.estado == 'EN_TRASLADO'))
          && driverLockAfter() == requestId)
        || (resource.data.taxistaUid == request.auth.uid && resource.data.estado == 'EN_TRASLADO'
          && request.resource.data.estado == 'FINALIZADO'
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['estado','finalizado'])
          && request.resource.data.finalizado == request.time && driverLockAfter() == ''));
      allow delete: if false;
    }
```

## Comprobación en la app

- Abrir una reserva propia en Lima y comprobar destino LIM.
- Abrir una en Cusco o Arequipa y comprobar CUZ o AQP; no debe mostrar Jorge Chávez.
- En Paracas, el destino configurado es Pisco (PIO); confirmar que sea el aeropuerto del vuelo.
- Rotar o recrear la pantalla: conserva hora, pasajeros, equipaje y aeropuerto.
- Solicitar taxi: verificar en taxiRequests que el hotelId corresponda a la reserva y que destino y
  airportCode/airportName/airportCity correspondan al aeropuerto mostrado.
- Volver a abrir una solicitud antigua: conserva su destino original; no crea otra por cambiar la ciudad.
- Ciudad sin catálogo: aviso visible y Confirmar deshabilitado.
- La creación se valida contra el hotel actual del servidor. Si cambiaron su ciudad o dirección mientras
  la pantalla estaba abierta, volver a cargarla antes de solicitar.

## Fuentes del catálogo

Verificación: compilación del APK de la app y del APK de pruebas; 110 pruebas JVM correctas,
39 comprobaciones de aeropuertos y 21 de compatibilidad taxi en el emulador Firestore. Prueba
TaxiAirportMigrationTest ejecutada correctamente en el dispositivo conectado, con base temporal
aislada: conserva una solicitud versión 1 y persiste la identificación del nuevo destino. No se
ha creado una solicitud nueva en el Firebase real ni publicado reglas desde esta implementación.

- Nombres y códigos: [Listado del MTC](https://cdn.www.gob.pe/uploads/document/file/7419112/6322184-listado-de-aeropuertos%282%29.pdf).
- Pisco/Paracas: [Operador Aeropuertos del Perú](https://adp.com.pe/es/sede/pisco).
- Acceso al nuevo terminal de Lima: [MTC — Vamos al aeropuerto](https://www.gob.pe/institucion/mtc/campa%C3%B1as/91158-vamos-al-aeropuerto).

Estos destinos se pueden ampliar sin cambiar el contrato; cada ampliación requiere actualizar el
catálogo de la app y la lista permitida de las reglas de Firestore.
