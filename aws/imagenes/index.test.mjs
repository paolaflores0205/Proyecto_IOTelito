import test from 'node:test';
import assert from 'node:assert/strict';
import { createHandler } from './index.mjs';

const id = '12345678-1234-1234-1234-123456789abc';
const reference = `s3://photos/profilePhotos/client1/${id}.jpg`;
function setup(overrides = {}) {
  const calls = [];
  const handler = createHandler({ bucket: 'photos', project: 'demo',
    verify: async () => 'client1',
    profile: async () => ({ uid: 'client1', active: true, role: 'CLIENT' }),
    upload: async (...args) => { calls.push(args); return { url: 'https://upload', fields: { policy: 'signed' } }; },
    read: async (...args) => { calls.push(args); return 'https://read'; },
    uuid: () => id, ...overrides });
  return { handler, calls };
}
function event(path = '/profile-photos/upload-url', body = { contentType: 'image/jpeg', sizeBytes: 123 }) {
  return { rawPath: path, requestContext: { http: { method: 'POST' } },
    headers: { Authorization: 'Bearer test-token' }, body: JSON.stringify(body) };
}
test('health is configuration-only and does not require a session', async () => {
  const { handler, calls } = setup();
  assert.equal((await handler({ rawPath: '/health', httpMethod: 'GET' })).statusCode, 200);
  assert.equal(calls.length, 0);
});
test('missing session never signs an upload', async () => {
  const { handler, calls } = setup(); const e = event(); e.headers = {};
  assert.equal((await handler(e)).statusCode, 401); assert.equal(calls.length, 0);
});
test('rejected token never signs', async () => {
  const { handler, calls } = setup({ verify: async () => { throw Object.assign(new Error('Invalid'), { status: 401 }); } });
  assert.equal((await handler(event())).statusCode, 401); assert.equal(calls.length, 0);
});
for (const profile of [{ uid: 'other', active: true, role: 'CLIENT' },
  { uid: 'client1', active: false, role: 'CLIENT' }, { uid: 'client1', active: true, role: 'HOTEL_ADMIN' }]) {
  test(`rejects unauthorized profile ${JSON.stringify(profile)}`, async () => {
    const { handler, calls } = setup({ profile: async () => profile });
    assert.equal((await handler(event())).statusCode, 403); assert.equal(calls.length, 0);
  });
}
test('upload creates an own UUID key, ignores caller key and returns POST fields', async () => {
  const { handler, calls } = setup();
  const result = await handler(event(undefined, { contentType: 'image/jpeg', sizeBytes: 123, key: 'hotelPhotos/other' }));
  assert.equal(result.statusCode, 200); const body = JSON.parse(result.body);
  assert.equal(body.reference, reference); assert.equal(body.method, 'POST'); assert.ok(body.fields.policy);
  assert.deepEqual(calls, [['photos', `profilePhotos/client1/${id}.jpg`]]);
});
for (const body of [{ contentType: 'image/png', sizeBytes: 1 }, { contentType: 'image/jpeg', sizeBytes: 0 },
  { contentType: 'image/jpeg', sizeBytes: 2097152 }, { contentType: 'image/jpeg', sizeBytes: '10' }]) {
  test(`rejects invalid upload ${JSON.stringify(body)}`, async () => {
    const { handler, calls } = setup();
    assert.equal((await handler(event(undefined, body))).statusCode, 400); assert.equal(calls.length, 0);
  });
}
test('read signs only own reference', async () => {
  const { handler, calls } = setup();
  assert.equal((await handler(event('/profile-photos/read-url', { reference }))).statusCode, 200);
  assert.deepEqual(calls, [['photos', `profilePhotos/client1/${id}.jpg`]]);
});
for (const bad of [reference.replace('client1', 'other'), reference.replace('photos/', 'other/'),
  reference.replace(id, '../other'), 'https://public.example/foto.jpg', reference + '/extra']) {
  test(`rejects foreign or malformed reference ${bad}`, async () => {
    const { handler, calls } = setup();
    assert.equal((await handler(event('/profile-photos/read-url', { reference: bad }))).statusCode, 403);
    assert.equal(calls.length, 0);
  });
}
test('invalid JSON returns a controlled error', async () => {
  const { handler } = setup(); const e = event(); e.body = '{';
  assert.equal((await handler(e)).statusCode, 400);
});
test('unknown route never authorizes', async () => {
  const { handler, calls } = setup(); assert.equal((await handler(event('/other'))).statusCode, 404);
  assert.equal(calls.length, 0);
});

// Fotos de hotel: superadmin al catálogo, admin solo a su hotel asignado, lectura para cuentas activas.
const admin = { uid: 'admin1', active: true, role: 'HOTEL_ADMIN', hotelId: 7 };
const assigned = { id: 7, active: true, administratorUid: 'admin1' };
function hotelSetup(user, current = assigned) {
  return setup({ verify: async () => user.uid, profile: async () => user, hotel: async () => current });
}
const hotelUpload = (extra = {}) => event('/hotel-photos/upload-url', { contentType: 'image/jpeg', sizeBytes: 500, ...extra });
test('superadmin uploads hotel photos to the catalog folder', async () => {
  const { handler, calls } = hotelSetup({ uid: 'sa1', active: true, role: 'SUPERADMIN' });
  const result = await handler(hotelUpload({ hotelId: 7 }));
  assert.equal(result.statusCode, 200);
  assert.equal(JSON.parse(result.body).reference, `s3://photos/hotelPhotos/catalog/${id}.jpg`);
  assert.deepEqual(calls, [['photos', `hotelPhotos/catalog/${id}.jpg`]]);
});
test('assigned hotel admin uploads only to own hotel folder', async () => {
  const { handler, calls } = hotelSetup(admin);
  const result = await handler(hotelUpload({ hotelId: 7, key: 'hotelPhotos/catalog/x.jpg' }));
  assert.equal(result.statusCode, 200);
  assert.equal(JSON.parse(result.body).reference, `s3://photos/hotelPhotos/7/${id}.jpg`);
  assert.deepEqual(calls, [['photos', `hotelPhotos/7/${id}.jpg`]]);
});
for (const [name, user, current, body] of [
  ['other hotel id', admin, assigned, { hotelId: 8 }],
  ['missing hotel id', admin, assigned, {}],
  ['string hotel id', admin, assigned, { hotelId: '7' }],
  ['hotel assigned to someone else', admin, { ...assigned, administratorUid: 'admin2' }, { hotelId: 7 }],
  ['inactive hotel', admin, { ...assigned, active: false }, { hotelId: 7 }],
  ['client', { uid: 'client1', active: true, role: 'CLIENT' }, assigned, { hotelId: 7 }],
  ['taxista', { uid: 't1', active: true, role: 'TAXISTA' }, assigned, { hotelId: 7 }],
  ['inactive superadmin', { uid: 'sa1', active: false, role: 'SUPERADMIN' }, assigned, {}],
]) {
  test(`hotel upload rejects ${name}`, async () => {
    const { handler, calls } = hotelSetup(user, current);
    assert.equal((await handler(hotelUpload(body))).statusCode, 403); assert.equal(calls.length, 0);
  });
}
test('hotel upload keeps JPEG and size limits', async () => {
  const { handler, calls } = hotelSetup(admin);
  assert.equal((await handler(hotelUpload({ hotelId: 7, contentType: 'image/png' }))).statusCode, 400);
  assert.equal((await handler(hotelUpload({ hotelId: 7, sizeBytes: 2097152 }))).statusCode, 400);
  assert.equal(calls.length, 0);
});
for (const role of ['CLIENT', 'TAXISTA', 'HOTEL_ADMIN', 'SUPERADMIN']) {
  test(`active ${role} reads hotel photos`, async () => {
    const { handler, calls } = hotelSetup({ uid: 'u1', active: true, role, hotelId: null });
    for (const folder of ['catalog', '12']) {
      const ref = `s3://photos/hotelPhotos/${folder}/${id}.jpg`;
      assert.equal((await handler(event('/hotel-photos/read-url', { reference: ref }))).statusCode, 200);
    }
    assert.deepEqual(calls, [['photos', `hotelPhotos/catalog/${id}.jpg`], ['photos', `hotelPhotos/12/${id}.jpg`]]);
  });
}
for (const bad of [`s3://photos/profilePhotos/client1/${id}.jpg`, `s3://other/hotelPhotos/catalog/${id}.jpg`,
  `s3://photos/hotelPhotos/0/${id}.jpg`, `s3://photos/hotelPhotos/../${id}.jpg`, `s3://photos/hotelPhotos/7/x.jpg`,
  `s3://photos/hotelPhotos/7/${id}.jpg/extra`, `s3://photosXhotelPhotos/7/${id}.jpg`]) {
  test(`hotel read rejects ${bad}`, async () => {
    const { handler, calls } = hotelSetup(admin);
    assert.equal((await handler(event('/hotel-photos/read-url', { reference: bad }))).statusCode, 403);
    assert.equal(calls.length, 0);
  });
}
test('inactive account cannot read hotel photos', async () => {
  const { handler, calls } = hotelSetup({ ...admin, active: false });
  const ref = `s3://photos/hotelPhotos/7/${id}.jpg`;
  assert.equal((await handler(event('/hotel-photos/read-url', { reference: ref }))).statusCode, 403);
  assert.equal(calls.length, 0);
});
test('profile routes still require CLIENT', async () => {
  const { handler, calls } = hotelSetup({ uid: 'sa1', active: true, role: 'SUPERADMIN' });
  assert.equal((await handler(event())).statusCode, 403); assert.equal(calls.length, 0);
});
