// Ejecutar solo contra el emulador local demo-iotelito-auth en el puerto 8180.
const assert = require('node:assert/strict');
const { call, fields, expect, seed, write, commit, runAuthRules, countChecks } = require('./firestore_auth_rules.test.cjs');
async function put(path, data) {
  const r = await call('PATCH', `/${path}`, { fields: fields(data) }, 'owner');
  assert.ok(r.ok, await r.text());
}
const change = (id, data, actor = 'admin') => write(`hotels/${id}`, { ...data, updatedByUid: actor },
  [...Object.keys(data), 'updatedByUid', 'updatedAt'], ['updatedAt']);
const room = (id = 201, hotelId = 1) => ({ id, hotelId, tipo: 'Doble', nombreCodigo: `Hab ${id}`, areaM2: 20,
  adultos: 2, ninos: 0, precioNoche: 150.5, descripcion: 'Prueba', disponible: true, archived: false });
const summary = (revision, count = 1) => change(1, { inventoryRevision: revision, rooms: count,
  hasInventory: count > 0, pricePerNight: count > 0 ? 150.5 : 0 });
const service = (id = 301, hotelId = 1) => ({ id, hotelId, nombre: 'Desayuno', descripcion: 'Buffet', precio: 42.5,
  unidad: 'persona', activo: true, archived: false });
(async () => {
  await runAuthRules(); const start = countChecks();
  await put('users/admin', { uid: 'admin', role: 'HOTEL_ADMIN', active: true, hotelId: 1, name: 'Admin', email: 'admin@example.test' });
  await seed('admin2', 'HOTEL_ADMIN');
  await put('hotels/1', { id: 1, name: 'Hotel 1', address: 'Direccion', city: 'Lima', district: 'Miraflores',
    phone: '123', email: 'hotel@example.test', description: '', attractions: '', photos: [], active: true,
    administratorUid: 'admin', administratorName: 'Admin', rooms: 0, hasInventory: false, pricePerNight: 0 });
  await put('hotels/2', { id: 2, name: 'Otro hotel', active: true, administratorUid: 'admin2' });
  await expect(await commit([change(1, { name: 'Hotel editado', description: 'Descripcion' })], 'admin'), true, 'Admin edita su hotel');
  await expect(await commit([change(2, { name: 'Ataque' })], 'admin'), false, 'Admin no edita otro hotel');
  await expect(await commit([change(1, { name: 'Ataque' }, 'client')], 'client'), false, 'Cliente no edita hotel');
  for (const [field, value] of [['active', false], ['administratorUid', 'admin2'], ['id', 2], ['reservations', 999], ['services', ['Spa']]])
    await expect(await commit([change(1, { [field]: value })], 'admin'), false, `Campo protegido ${field}`);
  await expect(await commit([change(1, { name: '' })], 'admin'), false, 'Nombre vacio');
  await expect(await commit([write('hotels/1/rooms/201', room())], 'admin'), false, 'Habitacion requiere resumen atomico');
  await expect(await commit([write('hotels/1/rooms/201', room()), summary(1)], 'admin'), true, 'Alta de habitacion y resumen');
  await expect(await call('GET', '/hotels/1/rooms/201', undefined, 'client'), true, 'Cliente lee inventario compartido');
  await expect(await commit([write('hotels/1/rooms/202', { ...room(202), precioNoche: -1 }), summary(2, 2)], 'admin'), false, 'Precio negativo');
  await expect(await commit([write('hotels/1/rooms/202', { ...room(202), adultos: 0 }), summary(2, 2)], 'admin'), false, 'Capacidad invalida');
  await expect(await commit([write('hotels/1/rooms/202', room(202, 2)), summary(2, 2)], 'admin'), false, 'Hotel del registro inmutable');
  await expect(await commit([write('hotels/1/rooms/201', { ...room(), precioNoche: 180.75 }), summary(2)], 'admin'), true, 'Editar precio conserva ID');
  await expect(await commit([write('hotels/1/rooms/201', { ...room(), archived: true, disponible: true }), summary(3)], 'admin'), false, 'Retirada no puede seguir disponible');
  await expect(await commit([write('hotels/1/rooms/201', { ...room(), archived: true, disponible: false }), summary(3, 0)], 'admin'), true, 'Retirada conserva documento historico');
  await expect(await call('DELETE', '/hotels/1/rooms/201', undefined, 'admin'), false, 'No borra referencias historicas');
  await expect(await commit([write('hotels/1/rooms/201', room()), summary(4)], 'admin'), false, 'No reutiliza habitacion retirada');
  const concurrent = await Promise.all([210, 211].map(id => commit([
    write(`hotels/1/rooms/${id}`, room(id)), summary(4)
  ], 'admin')));
  assert.deepEqual(concurrent.map(response => response.status).sort(), [200, 403],
    'Dos dispositivos no pueden confirmar resumen con la misma revision');
  await expect(await commit([write('hotels/1/services/301', service())], 'admin'), true, 'Alta servicio propio');
  await expect(await commit([write('hotels/1/services/301', { ...service(), precio: 50.75 })], 'admin'), true, 'Editar servicio');
  await expect(await commit([write('hotels/1/services/302', service(302))], 'client'), false, 'Cliente no crea servicio');
  await expect(await commit([write('hotels/2/services/302', service(302, 2))], 'admin'), false, 'Otro hotel no autorizado');
  await expect(await commit([write('hotels/1/services/302', { ...service(302), precio: -10 })], 'admin'), false, 'Servicio precio negativo');
  await expect(await commit([write('hotels/1/services/301', { ...service(), archived: true, activo: false })], 'admin'), true, 'Retirar servicio');
  await expect(await commit([write('hotels/1/services/301', service())], 'admin'), false, 'No reutiliza servicio retirado');
  await expect(await call('DELETE', '/hotels/1/services/301', undefined, 'admin'), false, 'No borra servicio');
  await put('users/admin', { uid: 'admin', role: 'HOTEL_ADMIN', active: false, hotelId: 1 });
  await expect(await commit([change(1, { name: 'Inactivo' })], 'admin'), false, 'Admin inactivo sin permisos');
  await put('users/admin', { uid: 'admin', role: 'HOTEL_ADMIN', active: true, hotelId: 2 });
  await expect(await commit([write('hotels/1/services/303', service(303))], 'admin'), false, 'Asignacion debe coincidir en usuario y hotel');
  await put('users/admin', { uid: 'admin', role: 'HOTEL_ADMIN', active: true, hotelId: 1 });
  await put('hotels/1', { id: 1, active: false, administratorUid: 'admin' });
  await expect(await commit([write('hotels/1/services/303', service(303))], 'admin'), false, 'Hotel inactivo no permite administrar');
  console.log(`Hotel admin: ${countChecks() - start} comprobaciones + concurrencia correctas, solo emulador.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
