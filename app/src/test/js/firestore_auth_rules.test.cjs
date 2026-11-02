// Pruebas REST exclusivamente contra el emulador local; sin credenciales reales.
const assert = require('node:assert/strict');
const project = 'demo-iotelito-auth';
const root = `http://127.0.0.1:8180/v1/projects/${project}/databases/(default)/documents`;
let checks = 0;
function token(uid) {
  const now = Math.floor(Date.now() / 1000);
  const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ iss: `https://securetoken.google.com/${project}`,
    aud: project, auth_time: now, sub: uid, user_id: uid, iat: now, exp: now + 3600,
    email: `${uid}@example.test`, firebase: { sign_in_provider: 'password' } })}.`;
}
function field(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return { integerValue: String(value) };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(field) } };
  return { stringValue: value };
}
const fields = data => Object.fromEntries(Object.entries(data).map(([k, v]) => [k, field(v)]));
async function call(method, path, data, uid) {
  return fetch(root + path, { method, headers: { 'Content-Type': 'application/json',
    ...(uid ? { Authorization: `Bearer ${uid === 'owner' ? 'owner' : token(uid)}` } : {}) },
    ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
}
async function expect(response, allowed, label) {
  const body = await response.text();
  assert.equal(response.ok, allowed, `${label}: HTTP ${response.status}: ${body}`);
  if (!allowed) assert.equal(response.status, 403, `${label}: debe denegar por permisos`);
  checks++;
}
async function seed(uid, role, active = true) {
  const response = await call('PATCH', `/users/${uid}`, { fields: fields({ uid, role, active,
    name: `Usuario ${uid}`, email: `${uid}@example.test` }) }, 'owner');
  assert.ok(response.ok, await response.text());
}
function write(path, data, updates, timestamps = []) {
  return { update: { name: `projects/${project}/databases/(default)/documents/${path}`, fields: fields(data) },
    ...(updates ? { updateMask: { fieldPaths: updates } } : {}),
    ...(timestamps.length ? { updateTransforms: timestamps.map(fieldPath => ({ fieldPath, setToServerValue: 'REQUEST_TIME' })) } : {}) };
}
async function commit(writes, uid) { return call('POST', ':commit', { writes }, uid); }
async function status(target, active, actor, extra = {}) {
  const data = { active, statusReason: active ? '' : 'Prueba de desactivación', updatedByUid: actor, ...extra };
  return commit([write(`users/${target}`, data, [...Object.keys(data), 'updatedAt'], ['updatedAt'])], actor);
}
(async () => {
  // El borrado solo afecta al proyecto demo del emulador efímero.
  const cleared = await fetch(`http://127.0.0.1:8180/emulator/v1/projects/${project}/databases/(default)/documents`, { method: 'DELETE' });
  assert.ok(cleared.ok, 'Inicia el emulador en 127.0.0.1:8180 antes de ejecutar estas pruebas.');
  await seed('sa', 'SUPERADMIN'); await seed('client', 'CLIENT'); await seed('other', 'CLIENT');
  await seed('admin', 'HOTEL_ADMIN'); await seed('disabled', 'CLIENT', false);
  await seed('disabled-sa', 'SUPERADMIN', false);
  await seed('sa2', 'SUPERADMIN');
  await seed('invalid-role', 'UNKNOWN');
  const mismatched = await call('PATCH', '/users/mismatched', { fields: fields({ uid: 'another-uid',
    role: 'SUPERADMIN', active: true, name: 'Perfil incorrecto', email: 'mismatched@example.test' }) }, 'owner');
  assert.ok(mismatched.ok);
  const hotel = await call('PATCH', '/hotels/1', { fields: fields({ id: 1, name: 'Hotel de prueba' }) }, 'owner');
  assert.ok(hotel.ok);
  await expect(await call('GET', '/users/sa', undefined), false, 'Sin sesión no puede leer usuarios');
  await expect(await call('GET', '/users/client', undefined, 'client'), true, 'Perfil propio');
  await expect(await call('GET', '/users/other', undefined, 'client'), false, 'Perfil de otro cliente');
  await expect(await call('GET', '/users', undefined, 'client'), false, 'Cliente no lista usuarios');
  await expect(await call('GET', '/users', undefined, 'admin'), false, 'Admin de hotel no lista usuarios');
  await expect(await call('GET', '/users', undefined, 'sa'), true, 'Superadmin activo lista usuarios');
  await expect(await call('GET', '/users', undefined, 'disabled-sa'), false, 'Superadmin inactivo no lista');
  await expect(await call('GET', '/users/disabled', undefined, 'disabled'), true, 'Inactivo puede consultar su propio estado');
  await expect(await call('GET', '/hotels/1', undefined, 'disabled'), false, 'Inactivo no lee catálogo remoto');
  await expect(await call('GET', '/hotels/1', undefined, 'client'), true, 'Cliente activo lee catálogo');
  await expect(await call('GET', '/hotels/1', undefined, 'missing-profile'), false, 'Sin perfil no lee catálogo');
  await expect(await call('GET', '/hotels/1', undefined, 'invalid-role'), false, 'Rol desconocido no obtiene permisos');
  await expect(await call('GET', '/users', undefined, 'mismatched'), false, 'UID inconsistente no obtiene permisos superadmin');
  await expect(await status('other', false, 'client'), false, 'Cliente no desactiva a otro');
  await expect(await status('other', false, 'sa'), true, 'Superadmin desactiva usuario');
  await expect(await call('GET', '/hotels/1', undefined, 'other'), false, 'Sesión existente pierde permiso tras desactivación');
  await expect(await status('other', true, 'sa'), true, 'Superadmin reactiva usuario');
  await expect(await call('GET', '/hotels/1', undefined, 'other'), true, 'Reactivado recupera acceso');
  await expect(await status('sa', false, 'sa'), false, 'No puede desactivarse a sí mismo');
  await expect(await status('sa', false, 'admin'), false, 'Otro rol no modifica superadmin');
  await expect(await status('sa2', false, 'sa'), false, 'No desactiva otra cuenta superadmin');
  await expect(await status('client', false, 'disabled-sa'), false, 'Superadmin inactivo no modifica estados');
  await expect(await status('other', false, 'sa', { role: 'SUPERADMIN' }), false, 'Cambio de estado no permite cambiar rol');
  await expect(await commit([write('users/client', { role: 'SUPERADMIN' }, ['role'])], 'client'), false, 'No autopromoción');
  await expect(await commit([write('users/disabled', { active: true }, ['active'])], 'disabled'), false, 'No autoreactivación');
  const profile = uid => ({ uid, name: 'Nueva cuenta', email: `${uid}@example.test`, role: 'CLIENT', active: true, hotelId: null });
  await expect(await commit([write('users/new', profile('new'), null, ['createdAt', 'updatedAt'])], 'new'), true, 'Registro público crea solo CLIENT');
  await expect(await commit([write('users/escalate', { ...profile('escalate'), role: 'SUPERADMIN' }, null, ['createdAt', 'updatedAt'])], 'escalate'), false, 'Registro no crea SUPERADMIN');
  await expect(await commit([write('users/wrong', profile('wrong'), null, ['createdAt', 'updatedAt'])], 'client'), false, 'No crea perfil de otro UID');
  await expect(await commit([write('users/assigned', { ...profile('assigned'), hotelId: 1 }, null, ['createdAt', 'updatedAt'])], 'assigned'), false, 'No autoasignación de hotel');
  await expect(await commit([write('users/new', { name: 'Nombre actualizado' }, ['name', 'updatedAt'], ['updatedAt'])], 'new'), true, 'Actualizar nombre propio');
  await expect(await commit([write('users/new', { uid: 'sa' }, ['uid', 'updatedAt'], ['updatedAt'])], 'new'), false, 'UID inmutable');
  await expect(await call('DELETE', '/users/other', undefined, 'sa'), false, 'No borrar perfiles desde la app');
  const missing = await call('GET', '/reservations/absent', undefined, 'client');
  assert.equal(missing.status, 404, 'Una reserva ausente debe poder comprobarse sin error de permisos'); checks++;
  const reservation = { id: 1001, clientUid: 'client', hotelId: 1, roomIds: [101], roomName: 'Simple',
    roomNumber: '101', fechaEntrada: '2026-12-10', fechaSalida: '2026-12-11', huespedes: '1 adulto',
    precioTotal: 100, estado: 'CONFIRMADA', codigo: 'TEST-RULES' };
  await expect(await commit([
    write('reservations/1001', reservation, null, ['createdAt']),
    write('roomNights/1_101_2026-12-10', { reservationId: '1001' }),
    write('clientNights/client_2026-12-10', { reservationId: '1001', clientUid: 'client' })
  ], 'client'), true, 'Conserva transacción de reserva y bloqueos del cliente');
  await expect(await call('GET', '/reservations/1001', undefined, 'client'), true, 'Reserva propia');
  await expect(await call('GET', '/reservations/1001', undefined, 'other'), false, 'No lee reserva ajena');
  await expect(await commit([write('reservations/1001', { estado: 'COMPLETADA' }, ['estado'])], 'client'), true, 'Conserva checkout propio');
  await expect(await commit([write('reservations/1001', { precioTotal: 0 }, ['precioTotal'])], 'client'), false, 'No altera precio de reserva existente');
  console.log(`Reglas de autenticación/usuarios: ${checks} comprobaciones correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
