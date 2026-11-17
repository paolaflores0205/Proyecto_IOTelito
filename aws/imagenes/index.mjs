import { randomUUID } from 'node:crypto';
import { S3Client, GetObjectCommand, HeadObjectCommand } from '@aws-sdk/client-s3';
import { createPresignedPost } from '@aws-sdk/s3-presigned-post';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const MAX_BYTES = 2 * 1024 * 1024 - 1;
const EXPIRY_SECONDS = 120;
const keys = createRemoteJWKSet(new URL(
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'),
  { timeoutDuration: 5000 });
const s3 = new S3Client({});

class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

export async function verifyFirebaseToken(token, project) {
  let payload;
  try {
    ({ payload } = await jwtVerify(token, keys, {
      algorithms: ['RS256'], audience: project,
      issuer: `https://securetoken.google.com/${project}`,
      requiredClaims: ['exp', 'iat', 'sub', 'auth_time'],
    }));
  } catch (error) {
    if (['ERR_JWKS_TIMEOUT', 'ERR_JOSE_GENERIC'].includes(error.code)
        || error instanceof TypeError) throw new HttpError(503, 'No se pudo verificar la sesión. Reintenta.');
    throw new HttpError(401, 'Sesión inválida o vencida. Inicia sesión nuevamente.');
  }
  const now = Math.floor(Date.now() / 1000);
  if (typeof payload.sub !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(payload.sub)
      || !Number.isFinite(payload.iat) || payload.iat > now
      || !Number.isFinite(payload.auth_time) || payload.auth_time > now) {
    throw new HttpError(401, 'Sesión inválida.');
  }
  return payload.sub;
}

const ROLES = ['CLIENT', 'HOTEL_ADMIN', 'TAXISTA', 'SUPERADMIN'];
const UUID_JPG = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg';

function integer(value) {
  const parsed = value?.integerValue === undefined ? NaN : Number(value.integerValue);
  return Number.isSafeInteger(parsed) ? parsed : null;
}

async function firestoreDocument(path, token, project, denied) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents/${path}`,
    { headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(5000) });
  if (response.status === 401) throw new HttpError(401, 'Tu sesión ha vencido.');
  if ([403, 404].includes(response.status)) throw new HttpError(403, denied);
  if (!response.ok) throw new HttpError(503, 'No se pudo consultar Firestore. Reintenta.');
  const { fields = {} } = await response.json();
  return fields;
}

async function loadProfile(uid, token, project) {
  const fields = await firestoreDocument(`users/${encodeURIComponent(uid)}`, token, project,
    'No se pudo acceder a tu perfil.');
  return { uid: fields.uid?.stringValue, active: fields.active?.booleanValue,
    role: fields.role?.stringValue, hotelId: integer(fields.hotelId) };
}

async function loadHotel(hotelId, token, project) {
  const fields = await firestoreDocument(`hotels/${hotelId}`, token, project,
    'No se pudo acceder al hotel.');
  return { id: integer(fields.id), active: fields.active?.booleanValue,
    administratorUid: fields.administratorUid?.stringValue };
}

// Superadmin sube al catálogo; el administrador solo a la carpeta del hotel que tiene asignado.
async function hotelPhotoFolder(user, uid, body, token, project, hotel) {
  if (user.role === 'SUPERADMIN') return 'catalog';
  if (user.role !== 'HOTEL_ADMIN' || !Number.isSafeInteger(body.hotelId) || body.hotelId < 1
      || user.hotelId !== body.hotelId) throw new HttpError(403, 'No tienes permiso para subir fotos de este hotel.');
  const current = await hotel(body.hotelId, token, project);
  if (current.id !== body.hotelId || current.active !== true || current.administratorUid !== uid)
    throw new HttpError(403, 'No tienes permiso para subir fotos de este hotel.');
  return String(body.hotelId);
}

async function signUpload(bucket, key) {
  // POST policy enforces both JPEG declaration and the size limit at S3.
  return createPresignedPost(s3, {
    Bucket: bucket, Key: key, Expires: EXPIRY_SECONDS,
    Fields: { 'Content-Type': 'image/jpeg' },
    Conditions: [['content-length-range', 1, MAX_BYTES], ['eq', '$Content-Type', 'image/jpeg']],
  });
}

async function signRead(bucket, key) {
  try {
    const object = await s3.send(new HeadObjectCommand({ Bucket: bucket, Key: key }));
    if (object.ContentType !== 'image/jpeg' || object.ContentLength < 1 || object.ContentLength > MAX_BYTES)
      throw new HttpError(422, 'El archivo no es una foto admitida.');
  } catch (error) {
    if (error.$metadata?.httpStatusCode === 404) throw new HttpError(404, 'La foto no existe.');
    throw error;
  }
  return getSignedUrl(s3, new GetObjectCommand({ Bucket: bucket, Key: key }),
    { expiresIn: EXPIRY_SECONDS });
}

const ROUTES = ['/profile-photos/upload-url', '/profile-photos/read-url',
  '/hotel-photos/upload-url', '/hotel-photos/read-url'];

export function createHandler({ bucket, project, verify = verifyFirebaseToken,
  profile = loadProfile, hotel = loadHotel, upload = signUpload, read = signRead, uuid = randomUUID }) {
  return async (event = {}) => {
    const reply = (statusCode, body) => ({ statusCode,
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
      body: JSON.stringify(body) });
    try {
      if (!bucket || !project) throw new HttpError(500, 'Falta configurar la función.');
      const method = event.requestContext?.http?.method ?? event.httpMethod;
      const path = event.rawPath ?? event.path;
      if (method === 'GET' && path === '/health') return reply(200, { ok: true });
      if (method !== 'POST' || !ROUTES.includes(path))
        throw new HttpError(404, 'Ruta no encontrada.');
      const authorization = Object.entries(event.headers ?? {})
        .find(([name]) => name.toLowerCase() === 'authorization')?.[1];
      if (typeof authorization !== 'string' || !/^Bearer \S+$/i.test(authorization)
          || authorization.length > 16000) throw new HttpError(401, 'Inicia sesión para acceder a las fotos.');
      const token = authorization.slice(7);
      const uid = await verify(token, project);
      const user = await profile(uid, token, project);
      const hotelRoute = path.startsWith('/hotel-photos/');
      if (user.uid !== uid || user.active !== true
          || (hotelRoute ? !ROLES.includes(user.role) : user.role !== 'CLIENT'))
        throw new HttpError(403, hotelRoute ? 'Tu cuenta no está habilitada.' : 'El perfil cliente no está habilitado.');
      let body;
      try {
        if (typeof event.body !== 'string' || event.body.length > 8192) throw new Error();
        body = JSON.parse(event.isBase64Encoded ? Buffer.from(event.body, 'base64').toString('utf8') : event.body);
        if (!body || typeof body !== 'object' || Array.isArray(body)) throw new Error();
      } catch { throw new HttpError(400, 'Solicitud JSON inválida.'); }
      if (path === '/hotel-photos/read-url') {
        const pattern = new RegExp(`^s3://${bucket.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/(hotelPhotos/(?:catalog|[1-9][0-9]{0,9})/${UUID_JPG})$`);
        const match = typeof body.reference === 'string' ? pattern.exec(body.reference) : null;
        if (!match) throw new HttpError(403, 'Referencia de foto de hotel inválida.');
        return reply(200, { url: await read(bucket, match[1]), expiresIn: EXPIRY_SECONDS });
      }
      if (path.endsWith('/upload-url')) {
        if (body.contentType !== 'image/jpeg' || !Number.isInteger(body.sizeBytes)
            || body.sizeBytes < 1 || body.sizeBytes > MAX_BYTES)
          throw new HttpError(400, 'Selecciona una imagen JPEG de menos de 2 MiB.');
        const key = hotelRoute
          ? `hotelPhotos/${await hotelPhotoFolder(user, uid, body, token, project, hotel)}/${uuid()}.jpg`
          : `profilePhotos/${uid}/${uuid()}.jpg`;
        const signed = await upload(bucket, key);
        return reply(200, { reference: `s3://${bucket}/${key}`, method: 'POST',
          url: signed.url, fields: signed.fields, expiresIn: EXPIRY_SECONDS, maxBytes: MAX_BYTES });
      }
      const prefix = `s3://${bucket}/profilePhotos/${uid}/`;
      if (typeof body.reference !== 'string' || !body.reference.startsWith(prefix)
          || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$/.test(body.reference.slice(prefix.length)))
        throw new HttpError(403, 'Solo puedes consultar tu propia foto.');
      const key = body.reference.slice(`s3://${bucket}/`.length);
      return reply(200, { url: await read(bucket, key), expiresIn: EXPIRY_SECONDS });
    } catch (error) {
      // Never log tokens, signed URLs, image data or request bodies.
      if (!error.status) console.error('Image API failure', error.name, error.$metadata?.httpStatusCode ?? '');
      return reply(error.status ?? 503, { message: error.status ? error.message : 'No se pudo acceder a las imágenes. Reintenta.' });
    }
  };
}

export const handler = createHandler({ bucket: process.env.BUCKET_NAME,
  project: process.env.FIREBASE_PROJECT_ID });
