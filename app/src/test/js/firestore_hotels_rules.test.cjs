const assert = require('node:assert/strict');
const { call, expect, seed, write, commit, runAuthRules, countChecks } = require('./firestore_auth_rules.test.cjs');
const hotel = (id, origin = 'APP') => ({ id, name: `Hotel ${id}`, address: 'Av. de prueba 100', city: 'Lima', district: 'Miraflores',
  phone: '', email: '', description: 'Hotel de pruebas', active: true, latitude: -12.12, longitude: -77.03,
  photos: [], administratorUid: null, administratorName: null, rooms: 0, reservations: 0, hasInventory: false,
  pricePerNight: 0, rating: 0, reviews: 0, services: [], attractions: '', dataOrigin: origin, createdByUid: 'sa', updatedByUid: 'sa' });
const createHotel = data => write(`hotels/${data.id}`, data, null, ['createdAt', 'updatedAt']);
const counter = nextId => write('system/hotelIds', { nextId, updatedByUid: 'sa' }, null, ['updatedAt']);
const hotelChange = (id, fields) => write(`hotels/${id}`, { ...fields, updatedByUid: 'sa' },
  [...Object.keys(fields), 'updatedByUid', 'updatedAt'], ['updatedAt']);
const userLink = (uid, hotelId) => write(`users/${uid}`, { hotelId, updatedByUid: 'sa' },
  ['hotelId', 'updatedByUid', 'updatedAt'], ['updatedAt']);
const link = (id, uid) => hotelChange(id, { administratorUid: uid, administratorName: uid === null ? null : `Usuario ${uid}` });
async function get(path, uid = 'sa') { const response = await call('GET', `/${path}`, undefined, uid); assert.ok(response.ok, await response.clone().text()); return response.json(); }
(async () => {
  await runAuthRules();
  const start = countChecks();
  await expect(await commit([createHotel(hotel(1000000))], 'sa'), false, 'Alta sin contador atómico');
  await expect(await commit([counter(1000001)], 'sa'), false, 'No consume contador sin crear hotel');
  await expect(await commit([createHotel(hotel(1000000)), counter(1000001)], 'client'), false, 'Cliente no crea hotel');
  await expect(await commit([createHotel(hotel(1000000)), counter(1000001)], 'sa'), true, 'Alta y contador se confirman juntos');
  assert.equal((await get('system/hotelIds')).fields.nextId.integerValue, '1000001');
  await expect(await call('GET', '/system/hotelIds', undefined, 'client'), false, 'Contador privado a superadmin');
  await expect(await commit([createHotel(hotel(1000005)), counter(1000006)], 'sa'), false, 'No salta IDs del contador');
  await expect(await commit([createHotel({ ...hotel(1000001), latitude: 91 }), counter(1000002)], 'sa'), false, 'Coordenadas inválidas');
  await expect(await commit([createHotel({ ...hotel(1000001), city: '' }), counter(1000002)], 'sa'), false, 'Ciudad obligatoria');
  await expect(await commit([createHotel({ ...hotel(1000001), administratorUid: 'client' }), counter(1000002)], 'sa'), false, 'Alta no introduce una asignación falsa');
  await expect(await commit([hotelChange(1000000, { name: 'Nombre actualizado', description: 'Nueva descripción' })], 'sa'), true, 'Editar hotel sin sobrescribir campos protegidos');
  assert.equal((await get('hotels/1000000')).fields.name.stringValue, 'Nombre actualizado');
  await expect(await commit([hotelChange(1000000, { id: 5 })], 'sa'), false, 'ID inmutable');
  await expect(await commit([hotelChange(1000000, { reservations: 900 })], 'sa'), false, 'No inventa estadísticas al editar');
  await expect(await commit([hotelChange(1000000, { name: 'Ataque' })], 'client'), false, 'Cliente no edita hoteles');
  await expect(await commit([hotelChange(1000000, { name: 'Ataque' })], 'disabled-sa'), false, 'Superadmin inactivo no edita');
  await expect(await commit([hotelChange(1000000, { createdByUid: 'other' })], 'sa'), false, 'Autor de creación inmutable');
  const demo = { ...hotel(2, 'LOCAL_IMPORT'), latitude: null, longitude: null, rooms: 1, hasInventory: true, pricePerNight: 100 };
  const room = { id: 201, hotelId: 2, tipo: 'Simple', nombreCodigo: '201', areaM2: 20, adultos: 1, ninos: 0,
    precioNoche: 100, descripcion: 'Habitación de prueba', disponible: true };
  await expect(await commit([createHotel(demo), write('hotels/2/rooms/201', room)], 'sa'), true, 'Importación conserva ID e inventario sin coordenadas inventadas');
  await expect(await commit([createHotel({ ...demo, name: 'Sobrescritura' })], 'sa'), false, 'No sobrescribe un hotel importado existente');
  await expect(await commit([write('hotels/2/rooms/201', { ...room, precioNoche: 1 })], 'sa'), false, 'Importación no sobrescribe habitaciones');
  await expect(await commit([write('hotels/999/rooms/201', { ...room, hotelId: 999 })], 'sa'), false, 'No crea habitación huérfana');
  await expect(await commit([write('hotels/2/rooms/202', { ...room, id: 202 })], 'client'), false, 'Cliente no modifica inventario');
  await expect(await call('GET', '/hotels/2/rooms/201', undefined, 'client'), true, 'Cliente lee el inventario compartido');
  await expect(await commit([link(1000000, 'admin')], 'sa'), false, 'No asigna solo del lado del hotel');
  await expect(await commit([userLink('admin', 1000000)], 'sa'), false, 'No asigna solo del lado del usuario');
  await expect(await commit([link(1000000, 'admin'), userLink('admin', 1000000)], 'sa'), true, 'Asignación bidireccional atómica');
  await expect(await commit([link(2, 'admin'), userLink('admin', 2)], 'sa'), false, 'No asigna el mismo administrador a otro hotel');
  await seed('admin2', 'HOTEL_ADMIN'); await seed('disabled-admin', 'HOTEL_ADMIN', false);
  await expect(await commit([link(2, 'disabled-admin'), userLink('disabled-admin', 2)], 'sa'), false, 'No asigna administrador inactivo');
  await expect(await commit([link(2, 'client'), userLink('client', 2)], 'sa'), false, 'No asigna un cliente como administrador');
  await expect(await commit([link(1000000, 'admin2'), userLink('admin2', 1000000)], 'sa'), false, 'No deja al administrador anterior enlazado');
  await expect(await commit([link(1000000, 'admin2'), userLink('admin2', 1000000), userLink('admin', null)], 'sa'), true, 'Reemplazo libera al administrador anterior');
  assert.equal((await get('users/admin')).fields.hotelId.nullValue, null);
  assert.equal((await get('users/admin2')).fields.hotelId.integerValue, '1000000');
  await expect(await commit([link(2, 'admin'), userLink('admin', 2)], 'sa'), true, 'Administrador liberado puede asignarse a otro hotel');
  await expect(await commit([link(1000000, null)], 'sa'), false, 'No desvincula a medias');
  await expect(await commit([link(1000000, null), userLink('admin2', null)], 'sa'), true, 'Desvinculación atómica');
  await expect(await commit([userLink('admin2', 2)], 'admin2'), false, 'Administrador no se autoasigna');
  await expect(await commit([hotelChange(2, { active: false })], 'sa'), true, 'Desactivar hotel');
  await expect(await commit([write('reservations/2001', { id: 2001, clientUid: 'client', hotelId: 2, roomIds: [201],
    estado: 'CONFIRMADA', fechaEntrada: '2026-12-20', fechaSalida: '2026-12-21' })], 'client'), false, 'No reserva en hotel desactivado');
  await expect(await call('DELETE', '/hotels/2', undefined, 'sa'), false, 'No borra hotel con referencias históricas');
  const candidates = [1000001, 1000001].map(id => commit([createHotel(hotel(id)), counter(id + 1)], 'sa'));
  const concurrent = await Promise.all(candidates);
  assert.deepEqual(concurrent.map(r => r.status).sort(), [200, 403], 'Dos altas con mismo ID no sobrescriben el hotel');
  const low = [3, 4];
  for (const id of low) await expect(await commit([createHotel(hotel(id, 'LOCAL_IMPORT'))], 'sa'), true, `Hotel ${id} para concurrencia`);
  await seed('admin3', 'HOTEL_ADMIN');
  const raced = await Promise.all(low.map(id => commit([link(id, 'admin3'), userLink('admin3', id)], 'sa')));
  assert.deepEqual(raced.map(r => r.status).sort(), [200, 403], 'Dos asignaciones concurrentes no duplican administrador');
  console.log(`Hoteles/asignaciones: ${countChecks() - start} comprobaciones de permisos + 2 de concurrencia correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
