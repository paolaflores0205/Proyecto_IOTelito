const { call, expect, write, commit, runAuthRules, countChecks } = require('./firestore_auth_rules.test.cjs');
(async () => {
  await runAuthRules(); const start = countChecks();
  const event = (id, extra = {}) => write(`logs/${id}`, { actorUid: 'sa', actorRole: 'SUPERADMIN', action: 'HOTEL_ACTIVATED',
    entityType: 'hotels', entityId: '1', ...extra }, null, ['createdAt']);
  const hotel = () => write('hotels/1', { active: true, updatedByUid: 'sa' }, ['active', 'updatedByUid', 'updatedAt'], ['updatedAt']);
  await expect(await call('GET', '/reservations', undefined, 'sa'), true, 'Superadmin consulta reportes globales');
  await expect(await call('GET', '/reservations', undefined, 'client'), false, 'Cliente no lista todas las reservas');
  await expect(await call('GET', '/reservations', undefined, 'admin'), false, 'Hotel-admin no recibe consulta global');
  await expect(await call('GET', '/reservations', undefined, 'disabled-sa'), false, 'Superadmin desactivado no consulta reportes');
  await expect(await commit([event('standalone')], 'sa'), false, 'Log sin operación atómica rechazado');
  await expect(await commit([hotel(), event('created')], 'sa'), true, 'Operación y log se confirman juntos');
  await expect(await call('GET', '/logs', undefined, 'sa'), true, 'Superadmin lista eventos');
  await expect(await call('GET', '/logs', undefined, 'client'), false, 'Cliente no lee eventos administrativos');
  await expect(await call('GET', '/logs', undefined, 'disabled-sa'), false, 'Inactivo no lee eventos');
  await expect(await commit([hotel(), event('spoofed', { actorUid: 'other' })], 'sa'), false, 'No falsifica autor');
  await expect(await commit([hotel(), event('wrong-action', { action: 'DELETE_ALL' })], 'sa'), false, 'Acción desconocida rechazada');
  await expect(await commit([hotel(), event('wrong-target', { entityId: 'missing' })], 'sa'), false, 'No asocia operación ajena');
  await expect(await commit([hotel(), event('client')], 'client'), false, 'Cliente no crea eventos');
  await expect(await commit([write('logs/created', { action: 'HOTEL_UPDATED' }, ['action'])], 'sa'), false, 'Evento inmutable');
  await expect(await call('DELETE', '/logs/created', undefined, 'sa'), false, 'No elimina historial');
  console.log(`Reportes/logs: ${countChecks() - start} comprobaciones correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
