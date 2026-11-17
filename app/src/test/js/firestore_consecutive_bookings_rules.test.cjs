// Exclusivamente localhost y proyecto demo. No usa cuentas del equipo.
const assert=require('node:assert/strict');
const {call,fields,expect,write,commit,runAuthRules,countChecks}=require('./firestore_auth_rules.test.cjs');
async function seed(path,data){const r=await call('PATCH','/'+path,{fields:fields(data)},'owner');assert.ok(r.ok,await r.text());}
const booking=(id,from,to,hotelId=2)=>({id,hotelId,clientUid:'client',roomIds:[201],roomName:'Suite',roomNumber:'201',
 fechaEntrada:from,fechaSalida:to,huespedes:'1 adulto',precioTotal:100,estado:'CONFIRMADA',codigo:'CONSEC-'+id,
 cardType:'Visa',cardLast4:'4242',cardToken:'DEMO_consecutive',paymentMode:'SIMULATED'});
const create=(id,from='2026-12-11',to='2026-12-12')=>write(`reservations/${id}`,booking(id,from,to),null,['createdAt']);
const night=(id,date='2026-12-11',uid='client')=>write(`clientNights/${uid}_${date}`,{reservationId:String(id),clientUid:uid});
(async()=>{
 await runAuthRules();const start=countChecks();
 await seed('hotels/2',{id:2,active:true,hasInventory:true});
 await seed('reservations/7000',{...booking(7000,'2026-12-10','2026-12-11',1),estado:'EN_CHECKOUT'});
 await expect(await commit([create(7001),night(7001)],'client'),true,'Nueva estadía desde salida aunque checkout siga pendiente');
 for(const [index,state] of ['CONFIRMADA','ACTIVA','EN_CHECKOUT'].entries()){
  await seed('reservations/7000',{...booking(7000,'2026-12-10','2026-12-11',1),estado:state});
  await seed('clientNights/client_2026-12-10',{reservationId:'7000',clientUid:'client'});
  await expect(await commit([create(7010+index,'2026-12-10','2026-12-11'),night(7010+index,'2026-12-10')],'client'),false,'No superpone noche de '+state);
 }
 // Un bloqueo antiguo incluía erróneamente el día de salida.
 await seed('clientNights/client_2026-12-11',{reservationId:'7000',clientUid:'client'});
 await expect(await commit([create(7020),night(7020)],'client'),true,'Reasigna bloqueo antiguo en día de salida');
 await expect(await commit([create(7021),night(7021)],'client'),false,'No reemplaza la reserva vigente que acaba de entrar');
 for(const [index,state] of ['FINALIZADA','COMPLETADA','CANCELADA'].entries()){
  await seed('reservations/7000',{...booking(7000,'2026-12-10','2026-12-11',1),estado:state});
  await seed('clientNights/client_2026-12-10',{reservationId:'7000',clientUid:'client'});
  await expect(await commit([create(7030+index,'2026-12-10','2026-12-11'),night(7030+index,'2026-12-10')],'client'),true,'No bloquea al cliente por '+state);
 }
 await seed('clientNights/client_2026-12-11',{reservationId:'99999',clientUid:'client'});
 await expect(await commit([create(7040),night(7040)],'client'),true,'Reasigna bloqueo huérfano');
 await expect(await commit([create(7041),night(7041,'2026-12-12')],'client'),false,'No crea bloqueo en su propio día de salida');
 await expect(await commit([night(7040,'2026-12-11','other')],'client'),false,'No altera bloqueos ajenos');
 await expect(await call('DELETE','/clientNights/client_2026-12-11',undefined,'client'),false,'No borra bloqueos de noches');
 await seed('roomNights/1_101_2026-12-10',{reservationId:'7000'});
 await expect(await commit([write('roomNights/1_101_2026-12-10',{reservationId:'7040'})],'client'),false,'Mantiene ocupación histórica de habitaciones');
 await seed('clientNights/client_2026-12-13',{reservationId:'99999',clientUid:'client'});
 const concurrently=await Promise.all([commit([create(7050,'2026-12-13','2026-12-14'),night(7050,'2026-12-13')],'client'),commit([create(7051,'2026-12-13','2026-12-14'),night(7051,'2026-12-13')],'client')]);
 assert.deepEqual(concurrently.map(r=>r.status).sort(),[200,403]);
 console.log(`Reservas consecutivas: ${countChecks()-start} comprobaciones y concurrencia correctas.`);
})().catch(e=>{console.error(e);process.exitCode=1;});
