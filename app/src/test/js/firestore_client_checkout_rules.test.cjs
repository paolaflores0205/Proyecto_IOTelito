// Solo localhost y proyecto demo. No usa credenciales reales.
const assert = require('node:assert/strict');
const {call,fields,expect,write,commit,runAuthRules,countChecks} = require('./firestore_auth_rules.test.cjs');
const card = {cardType:'Visa',cardLast4:'4242',cardToken:'DEMO_prueba',paymentMode:'SIMULATED'};
const booking = id => ({id,hotelId:1,clientUid:'client',roomIds:[101],roomName:'Doble',roomNumber:'101',
  fechaEntrada:'2025-01-01',fechaSalida:'2025-01-02',precioTotal:600,estado:'CONFIRMADA',codigo:`CHECKOUT-${id}`,...card});
async function put(path,data) {const r=await call('PATCH',`/${path}`,{fields:fields(data)},'owner');assert.ok(r.ok,await r.text());}
const request = (id, changes={}) => {
  const data={estado:'EN_CHECKOUT',rating:4,review:'Buena estadía\nPersonal atento',...changes};
  return write(`reservations/${id}`,data,[...Object.keys(data),'checkoutRequestedAt'],['checkoutRequestedAt']);
};
(async()=>{
 await runAuthRules(); const start=countChecks();
 await put('users/admin',{uid:'admin',role:'HOTEL_ADMIN',active:true,hotelId:1});
 await put('hotels/1',{id:1,active:true,administratorUid:'admin',hasInventory:true});
 for(let id=5001;id<=5008;id++) await put(`reservations/${id}`,booking(id));
 await expect(await commit([request(5001)],'other'),false,'No solicita checkout ajeno');
 await expect(await commit([request(5001)],'admin'),false,'Admin no suplanta solicitud del cliente');
 for(const rating of [0,6,1.5]) await expect(await commit([request(5001,{rating})],'client'),false,'Estrellas válidas y enteras');
 for(const review of ['', '  \n\t', 'x'.repeat(4001)]) await expect(await commit([request(5001,{review})],'client'),false,'Observaciones obligatorias y acotadas');
 await expect(await commit([request(5001,{precioTotal:0})],'client'),false,'No altera el alojamiento');
 await expect(await commit([request(5001,{cardLast4:'1234'})],'client'),false,'No cambia tarjeta ya registrada');
 await expect(await commit([request(5001,{cardToken:'TOKEN_REAL'})],'client'),false,'Solo tokens ficticios');
 await expect(await commit([request(5001,{numeroTarjeta:'4242424242424242'})],'client'),false,'No guarda PAN completo');
 await expect(await commit([request(5001,{cvv:'123'})],'client'),false,'No guarda CVV');
 await expect(await commit([write('reservations/5001',{estado:'FINALIZADA'},['estado'])],'client'),false,'No acredita su propio cobro');
 await expect(await commit([request(5001)],'client'),true,'Solicita con valoración y observaciones multilinea');
 await expect(await commit([request(5001,{rating:1})],'client'),false,'No reescribe valoración al repetir');
 await expect(await commit([write('reservations/5001',{estado:'FINALIZADA'},['estado'])],'client'),false,'Pendiente sigue sin poder finalizar');
 await expect(await commit([write('reservations/5001',{estado:'FINALIZADA'},['estado'])],'admin'),false,'Admin necesita comprobante atómico');
 const receipt={reservationId:'5001',hotelId:1,clientUid:'client',base:600,extra:40,total:640,
   reason:'Daño verificado',observation:'Detalle del cargo de demostración',createdByUid:'admin',cardType:'Visa',cardLast4:'4242',paymentMode:'SIMULATED'};
 await expect(await commit([write('hotelCheckouts/5001',receipt,null,['createdAt']),
   write('reservations/5001',{estado:'FINALIZADA'},['estado'])],'admin'),true,'Recorrido completo: cliente solicita y administrador cobra');
 const ownReceipt=await call('GET','/hotelCheckouts/5001',undefined,'client');
 assert.ok(ownReceipt.ok);const decoded=await ownReceipt.json();assert.equal(decoded.fields.total.integerValue,'640');
 assert.ok(decoded.fields.createdAt.timestampValue);assert.equal(decoded.fields.cardLast4.stringValue,'4242');
 await expect(await call('GET','/hotelCheckouts/5001',undefined,'other'),false,'Otro cliente no lee el cobro');
 await expect(await commit([request(5001)],'client'),false,'No reabre la reserva cobrada');
 await put('reservations/5002',{...booking(5002),fechaEntrada:'2099-01-01',fechaSalida:'2099-01-02'});
 await expect(await commit([request(5002)],'client'),false,'No solicita antes de salida');
 const old=booking(5003);for(const k of Object.keys(card))delete old[k];await put('reservations/5003',old);
 await expect(await commit([request(5003)],'client'),false,'Reserva anterior necesita registrar tarjeta');
 await expect(await commit([request(5003,card)],'client'),true,'Reserva anterior registra tarjeta simulada al solicitar');
 await put('reservations/5004',{...booking(5004),estado:'CANCELADA'});
 await expect(await commit([request(5004)],'client'),false,'No solicita cancelada');
 await put('reservations/5005',{...booking(5005),estado:'ACTIVA'});
 await expect(await commit([request(5005)],'client'),true,'Estadía activa también puede solicitar');
 await expect(await commit([write('reservations/6001',{...booking(6001),cardToken:'LIVE_123'},null,['createdAt'])],'client'),false,'Reserva nueva solo usa tarjeta de demostración');
 await expect(await commit([write('reservations/6001',{...booking(6001),pan:'4242424242424242'},null,['createdAt'])],'client'),false,'Reserva nueva no admite número completo');
 await expect(await commit([write('reservations/6001',{...booking(6001),cvv:'123'},null,['createdAt'])],'client'),false,'Reserva nueva no admite CVV');
 await expect(await commit([write('reservations/6001',booking(6001),null,['createdAt'])],'client'),true,'Reserva con tarjeta enmascarada válida');
 console.log(`Checkout cliente: ${countChecks()-start} comprobaciones correctas.`);
})().catch(e=>{console.error(e);process.exitCode=1;});
