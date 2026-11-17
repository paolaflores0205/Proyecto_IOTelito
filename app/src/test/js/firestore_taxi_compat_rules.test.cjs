const assert = require('node:assert/strict');
const { call, expect, fields, write, commit, runAuthRules, countChecks } = require('./firestore_auth_rules.test.cjs');
(async () => {
  await runAuthRules(); const start = countChecks();
  for (const uid of ['driver', 'other-driver', 'inactive-driver']) {
    const r = await call('PATCH', `/taxistas/${uid}`, { fields: fields({ activo: uid !== 'inactive-driver', disponible: true }) }, 'owner');
    assert.ok(r.ok);
  }
  await expect(await call('GET', '/taxistas/driver', undefined, 'driver'), true, 'Conductor lee su ficha');
  await expect(await call('GET', '/taxistas/driver', undefined, 'client'), false, 'Cliente no lee ficha ajena');
  await expect(await commit([write('taxistas/driver', { disponible: false }, ['disponible'])], 'driver'), true, 'Conductor cambia disponibilidad');
  await expect(await commit([write('taxistas/driver', { activo: false }, ['activo'])], 'driver'), false, 'Conductor no cambia habilitación');
  await expect(await commit([write('taxistas/inactive-driver', { disponible: false }, ['disponible'])], 'inactive-driver'), false, 'Inactivo no cambia disponibilidad');
  await expect(await commit([write('taxistas/driver', { disponible: true }, ['disponible'])], 'driver'), true, 'Conductor vuelve a estar disponible');
  const request = { id: 1001, reserva: 1001, clientUid: 'client', cliente: 'client@example.test', nombre: 'Cliente',
    hotel: 'Hotel prueba', origen: 'Av. prueba', destino: 'Aeropuerto Jorge Chávez', hora: '14:30', pasajeros: 2, maletas: 1,
    estado: 'SOLICITADO', taxistaUid: '', taxista: '', conductor: '', finalizado: null };
  const create = extra => write('taxiRequests/1001', { ...request, ...extra }, null, ['creado']);
  await expect(await commit([create({})], 'other'), false, 'Solicitud exige propietario de reserva');
  await expect(await commit([create({ hora: '99:00' })], 'client'), false, 'Horario inválido rechazado');
  await expect(await commit([create({})], 'client'), true, 'Cliente solicita taxi propio');
  await expect(await call('GET', '/taxiRequests/1001', undefined, 'driver'), true, 'Conductor activo ve solicitud');
  await expect(await call('GET', '/taxiRequests/1001', undefined, 'other'), false, 'Otro cliente no ve solicitud');
  await expect(await call('GET', '/taxiRequests/1001', undefined, 'inactive-driver'), false, 'Conductor inactivo no ve solicitud');
  const accept = () => write('taxiRequests/1001', { estado: 'ASIGNADO', taxistaUid: 'driver', taxista: 'driver@example.test', conductor: 'Conductor' }, ['estado', 'taxistaUid', 'taxista', 'conductor']);
  const lock = value => write('taxiDrivers/driver', { activeRequestId: value });
  await expect(await commit([accept()], 'driver'), false, 'Asignación sin bloqueo rechazada');
  await expect(await commit([accept(), lock('1001')], 'driver'), true, 'Asignación y bloqueo atómicos');
  await expect(await call('GET', '/taxiRequests/1001', undefined, 'other-driver'), false, 'Otro conductor no lee servicio asignado');
  await expect(await commit([write('taxiRequests/1001', { estado: 'EN_TRASLADO' }, ['estado'])], 'driver'), false, 'No salta pasos de servicio');
  await expect(await commit([write('taxiRequests/1001', { estado: 'EN_CAMINO' }, ['estado'])], 'driver'), true, 'Avanza a en camino');
  await expect(await commit([write('taxiRequests/1001', { estado: 'EN_TRASLADO' }, ['estado'])], 'driver'), true, 'Avanza a en traslado');
  const finish = () => write('taxiRequests/1001', { estado: 'FINALIZADO' }, ['estado', 'finalizado'], ['finalizado']);
  await expect(await commit([finish()], 'driver'), false, 'Finalización exige liberar bloqueo');
  await expect(await commit([finish(), lock('')], 'driver'), true, 'Finaliza y libera conductor juntos');
  await expect(await call('DELETE', '/taxiRequests/1001', undefined, 'driver'), false, 'No borra servicio');
  // Formato con aeropuerto según la ciudad del hotel (el formato anterior sigue aceptado arriba).
  const hotelDoc = { id: 1, name: 'Hotel de prueba', active: true, hasInventory: true, city: 'Lima', address: 'Av. prueba' };
  assert.ok((await call('PATCH', '/hotels/1', { fields: fields(hotelDoc) }, 'owner')).ok);
  assert.ok((await call('PATCH', '/reservations/1002', { fields: fields({ id: 1002, clientUid: 'client', hotelId: 1 }) }, 'owner')).ok);
  const lima = { hotelId: 1, hotelCity: 'Lima', airportCode: 'LIM', airportName: 'Aeropuerto Internacional Jorge Chávez',
    airportCity: 'Callao', destino: 'Aeropuerto Internacional Jorge Chávez (LIM)' };
  const airport = extra => write('taxiRequests/1002', { ...request, id: 1002, reserva: 1002, hotel: hotelDoc.name,
    origen: hotelDoc.address, ...lima, ...extra }, null, ['creado']);
  await expect(await commit([airport({ airportCode: 'CUZ', airportName: 'Aeropuerto Internacional Alejandro Velasco Astete',
    airportCity: 'Cusco', destino: 'Aeropuerto Internacional Alejandro Velasco Astete (CUZ)' })], 'client'), false,
    'Aeropuerto debe corresponder a la ciudad del hotel');
  await expect(await commit([airport({ airportName: 'Otro aeropuerto', destino: 'Otro aeropuerto (LIM)' })], 'client'), false,
    'Nombre de aeropuerto debe ser el oficial');
  await expect(await commit([airport({ destino: 'Aeropuerto Jorge Chávez' })], 'client'), false, 'Destino debe coincidir con el aeropuerto');
  await expect(await commit([airport({ hotel: 'Otro hotel' })], 'client'), false, 'Hotel debe coincidir con la reserva');
  await expect(await commit([airport({ hotelCity: 'Cusco' })], 'client'), false, 'Ciudad debe coincidir con el hotel');
  await expect(await commit([write('taxiRequests/1002', { ...request, id: 1002, reserva: 1002, hotelId: 1 }, null, ['creado'])], 'client'),
    false, 'Formato nuevo exige todos los campos del aeropuerto');
  await expect(await commit([airport({})], 'other'), false, 'Formato nuevo exige propietario de reserva');
  await expect(await commit([airport({})], 'client'), true, 'Cliente solicita taxi al aeropuerto de su ciudad');
  console.log(`Compatibilidad taxi: ${countChecks() - start} comprobaciones correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
