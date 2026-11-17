import test from 'node:test';
import assert from 'node:assert/strict';
import {eventFor} from './events.mjs';
test('checkout conserva destinatario, total y la identidad estable',()=>{
 const e=eventFor('hotelCheckouts','70',{clientUid:'alice',total:1050.5});
 assert.equal(e.uid,'alice');assert.equal(e.key,'checkout:70');assert.equal(e.data.notificationId,'70');
 assert.match(e.data.body,/1,050.50/);assert.equal(e.data.type,'CHECKOUT');
});
test('taxi comparte id con Android y permite abrir chat al asignar',()=>{
 const e=eventFor('taxiRequests','90',{clientUid:'alice',reserva:70,estado:'ASIGNADO'});
 assert.equal(e.data.notificationId,String(1000000+90*4+1));assert.equal(e.data.taxiId,'90');assert.match(e.data.body,/chat/);
});
test('traslado mantiene el evento de camino para evitar duplicar',()=>{
 const data={clientUid:'alice',reserva:70};
 assert.deepEqual(eventFor('taxiRequests','90',{...data,estado:'EN_CAMINO'}),eventFor('taxiRequests','90',{...data,estado:'EN_TRASLADO'}));
});
test('no publica cancelaciones como reservas confirmadas',()=>{
 assert.equal(eventFor('reservations','70',{clientUid:'alice',estado:'CANCELADA'}),null);
 assert.equal(eventFor('reservations','70',{clientUid:'alice',estado:'CONFIRMADA'}).data.type,'RESERVA');
});
test('rechaza destinatario, identidad o importe inválidos',()=>{
 assert.equal(eventFor('hotelCheckouts','70',{total:12}),null);
 assert.equal(eventFor('hotelCheckouts','bad',{clientUid:'alice',total:12}),null);
 assert.equal(eventFor('hotelCheckouts','70',{clientUid:'alice',total:NaN}),null);
 assert.equal(eventFor('taxiRequests','90',{clientUid:'alice',reserva:70,estado:'OTRO'}),null);
});
