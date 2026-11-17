import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID, createHash } from 'node:crypto';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { deliver } from './index.mjs';
import { eventFor } from './events.mjs';

const emulator=process.env.FIRESTORE_EMULATOR_HOST;
if(emulator && emulator!=='127.0.0.1:8180')throw Error('Solo se permite el emulador local de pruebas');
const app=emulator?initializeApp({projectId:'demo-iotelito-auth'},'push-tests'):null;
const db=app?getFirestore(app):null;
async function fixture() {
 const uid='push-test-'+randomUUID(),binding=randomUUID();
 const ref=db.collection('users').doc(uid).collection('notificationDevices').doc(binding);
 await ref.set({binding,token:'test-token'});
 const event=eventFor('hotelCheckouts','70',{clientUid:uid,total:1000});
 const receipt=db.collection('pushDeliveries').doc(createHash('sha256').update(`${uid}:${event.key}:${binding}`).digest('hex'));
 return {ref,event,receipt,device:await ref.get()};
}
test('dos ejecuciones concurrentes y un reintento solo envían una vez',{skip:!emulator},async()=>{
 const f=await fixture();let sends=0;
 try {
   const messaging={send:async message=>{sends++;assert.equal(message.data.uid,f.event.uid);assert.equal(message.notification,undefined);}};
   await Promise.all([deliver(db,messaging,f.event,f.device),deliver(db,messaging,f.event,f.device)]);
   await deliver(db,messaging,f.event,f.device);
   assert.equal(sends,1);assert.equal((await f.receipt.get()).data().sent,true);
 } finally {await f.ref.delete();await f.receipt.delete();}
});
test('un fallo temporal no consume el evento y permite reintentar',{skip:!emulator},async()=>{
 const f=await fixture();let calls=0;
 try {
   const messaging={send:async()=>{if(++calls===1)throw Object.assign(Error('transitorio'),{code:'messaging/server-unavailable'});}};
   await assert.rejects(deliver(db,messaging,f.event,f.device),/reintento/);
   assert.equal((await f.receipt.get()).data().sent,false);
   assert.equal(await deliver(db,messaging,f.event,f.device),true);assert.equal(calls,2);
 } finally {await f.ref.delete();await f.receipt.delete();}
});
test('un token revocado se elimina y no entra en bucle',{skip:!emulator},async()=>{
 const f=await fixture();
 try {
   const messaging={send:async()=>{throw Object.assign(Error('revocado'),{code:'messaging/registration-token-not-registered'});}};
   assert.equal(await deliver(db,messaging,f.event,f.device),false);
   assert.equal((await f.ref.get()).exists,false);assert.equal((await f.receipt.get()).data().sent,true);
 } finally {await f.ref.delete();await f.receipt.delete();}
});
