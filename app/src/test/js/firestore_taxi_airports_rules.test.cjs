// Solo emulador demo. No usa credenciales ni datos reales.
const assert=require('node:assert/strict');
const {call,fields,expect,write,commit,runAuthRules,countChecks}=require('./firestore_auth_rules.test.cjs');
async function seed(path,data){const r=await call('PATCH','/'+path,{fields:fields(data)},'owner');assert.ok(r.ok,await r.text());}
(async()=>{
 await runAuthRules();const start=countChecks();
 const cities=[['Lima','LIM','Aeropuerto Internacional Jorge Chávez','Callao'],
  ['Cusco','CUZ','Aeropuerto Internacional Alejandro Velasco Astete','Cusco'],
  ['Arequipa','AQP','Aeropuerto Internacional Alfredo Rodríguez Ballón','Arequipa'],
  ['Trujillo','TRU','Aeropuerto Internacional Capitán FAP Carlos Martínez de Pinillos','Trujillo'],
  ['Paracas','PIO','Aeropuerto Capitán FAP Renán Elías Olivera','Pisco']];
 for(let i=0;i<cities.length;i++){
  const [city,code,name,airportCity]=cities[i],id=6000+i,hotelId=700+i;
  await seed(`hotels/${hotelId}`,{id:hotelId,active:true,city,name:'Hotel '+city,address:'Dirección '+city});
  await seed(`reservations/${id}`,{id,hotelId,clientUid:'client'});
  const data={id,reserva:id,clientUid:'client',cliente:'client@example.test',nombre:'Cliente',hotel:'Hotel '+city,
   origen:'Dirección '+city,destino:`${name} (${code})`,hotelId,hotelCity:city,airportCode:code,airportName:name,airportCity,
   hora:'14:30',pasajeros:2,maletas:1,estado:'SOLICITADO',taxistaUid:'',taxista:'',conductor:'',finalizado:null};
  const create=changes=>write(`taxiRequests/${id}`,{...data,...changes},null,['creado']);
  await expect(await commit([create({})],'other'),false,'Solo el propietario puede solicitar');
  await expect(await commit([create({hotelId:999})],'client'),false,'Hotel debe coincidir con reserva');
  await expect(await commit([create({origen:'Dirección inventada'})],'client'),false,'Origen coincide con hotel');
  await expect(await commit([create({hotelCity:'Otra ciudad'})],'client'),false,'Ciudad de hotel verificada');
  await expect(await commit([create({airportName:'Aeropuerto inventado'})],'client'),false,'Nombre canónico');
  const partial={...data};delete partial.airportCity;
  await expect(await commit([write(`taxiRequests/${id}`,partial,null,['creado'])],'client'),false,'Metadatos incompletos rechazados');
  if(code!=='LIM')await expect(await commit([create({airportCode:'LIM',airportName:'Aeropuerto Internacional Jorge Chávez',airportCity:'Callao',destino:'Aeropuerto Internacional Jorge Chávez (LIM)'})],'client'),false,'No asigna Lima a otra ciudad');
  await expect(await commit([create({})],'client'),true,'Destino válido '+code);
  const saved=await call('GET',`/taxiRequests/${id}`,undefined,'client');assert.ok(saved.ok);
  assert.equal((await saved.json()).fields.airportCode.stringValue,code);
 }
 console.log(`Aeropuertos taxi: ${countChecks()-start} comprobaciones correctas.`);
})().catch(e=>{console.error(e);process.exitCode=1;});
