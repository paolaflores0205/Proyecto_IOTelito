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
  console.log(`Compatibilidad taxi: ${countChecks() - start} comprobaciones correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
