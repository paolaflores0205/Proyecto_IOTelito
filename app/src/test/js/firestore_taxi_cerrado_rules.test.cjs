// Las colecciones antiguas de taxi quedan cerradas: todo lo de taxistas va por la API REST
// de la web de gestión (enunciado, restricciones tecnológicas). Solo emulador local.
const assert = require('node:assert/strict');
const { call, expect, fields, write, commit, runAuthRules, countChecks } = require('./firestore_auth_rules.test.cjs');

async function guardar(path, data) {
  const r = await call('PATCH', '/' + path, { fields: fields(data) }, 'owner');
  assert.ok(r.ok, await r.text());
}

(async () => {
  await runAuthRules();
  const start = countChecks();
  await guardar('users/driver', { uid: 'driver', role: 'TAXISTA', active: true, name: 'Conductor', email: 'driver@example.test' });
  await guardar('users/client', { uid: 'client', role: 'CLIENT', active: true, name: 'Cliente', email: 'client@example.test' });
  await guardar('users/admin', { uid: 'admin', role: 'SUPERADMIN', active: true, name: 'Admin', email: 'admin@example.test' });
  await guardar('taxistas/driver', { activo: true, disponible: true });
  await guardar('taxiRequests/1001', { id: 1001, clientUid: 'client', taxistaUid: 'driver', estado: 'ASIGNADO' });
  await guardar('taxiRequests/1001/messages/m1', { senderUid: 'client', text: 'Hola' });
  await guardar('taxiDrivers/driver', { activeRequestId: '1001' });

  for (const uid of ['driver', 'client', 'admin']) {
    await expect(await call('GET', '/taxistas/driver', undefined, uid), false, `${uid} no lee taxistas`);
    await expect(await call('GET', '/taxiRequests/1001', undefined, uid), false, `${uid} no lee taxiRequests`);
    await expect(await call('GET', '/taxiRequests/1001/messages', undefined, uid), false, `${uid} no lee el chat antiguo`);
    await expect(await call('GET', '/taxiDrivers/driver', undefined, uid), false, `${uid} no lee taxiDrivers`);
  }
  await expect(await commit([write('taxistas/driver', { disponible: false }, ['disponible'])], 'driver'), false, 'Conductor no cambia disponibilidad en Firestore');
  await expect(await commit([write('taxiRequests/2002', { id: 2002, clientUid: 'client', estado: 'SOLICITADO' })], 'client'), false, 'Cliente no crea pedidos en Firestore');
  await expect(await commit([write('taxiRequests/1001', { estado: 'EN_CAMINO' }, ['estado'])], 'driver'), false, 'Conductor no avanza viajes en Firestore');
  await expect(await commit([write('taxiDrivers/driver', { activeRequestId: '' })], 'driver'), false, 'Conductor no cambia su bloqueo');
  console.log(`Taxi cerrado en Firestore: ${countChecks() - start} comprobaciones correctas, solo emulador.`);
})().catch(e => { console.error(e); process.exitCode = 1; });
