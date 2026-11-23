// Solo emulador demo en localhost. Nunca usa el proyecto del equipo.
const assert=require('node:assert/strict');
const {call,fields,expect,write,commit,runAuthRules,countChecks}=require('./firestore_auth_rules.test.cjs');
const profile=uid=>({uid,name:'Ana Torres',firstNames:'Ana',lastNames:'Torres',email:`${uid}@example.test`,
  documentType:'DNI',documentNumber:'12345678',birthDate:'1990-03-15',phone:'999888777',address:'Av. Principal 123',role:'CLIENT',active:true,hotelId:null});
const update=(uid,data)=>write(`users/${uid}`,data,[...Object.keys(data),'updatedAt'],['updatedAt']);
(async()=>{
 await runAuthRules();const start=countChecks();
 await expect(await commit([write('users/profile',profile('profile'),null,['createdAt','updatedAt'])],'profile'),true,'Registro completo');
 for(const data of [{documentType:'Pasaporte'},{documentNumber:'87654321'},{birthDate:'1991-01-01'},{email:'otro@example.test'},
   {role:'SUPERADMIN'},{active:false},{hotelId:1},{uid:'other'}])
   await expect(await commit([update('profile',data)],'profile'),false,'Identidad, rol y estado inmutables');
 await expect(await commit([update('profile',{firstNames:'Ana Maria',lastNames:'Torres Diaz',name:'Ana Maria Torres Diaz',phone:'988777666',address:'Av. Nueva 456'})],'profile'),true,'Edita datos permitidos');
 await expect(await commit([update('profile',{name:'Nombre inconsistente'})],'profile'),false,'Nombre coherente con nombres y apellidos');
 await expect(await commit([update('profile',{firstNames:'Ana2',lastNames:'Torres',name:'Ana2 Torres'})],'profile'),false,'Nombres solo con letras');
 await expect(await commit([update('profile',{firstNames:'José Ñ.',lastNames:"O'Neil-Ruiz",name:"José Ñ. O'Neil-Ruiz"})],'profile'),true,'Nombres con tildes, ñ, apóstrofo y guion');
 await expect(await commit([update('profile',{phone:'abc'})],'profile'),false,'Telefono validado');
 await expect(await commit([update('profile',{photoUrl:'gs://bucket/profilePhotos/profile/abc-123.jpg'})],'profile'),true,'Ruta de foto propia');
 const s3='s3://iot-telito-imagenes/profilePhotos/profile/12345678-1234-1234-1234-123456789abc.jpg';
 await expect(await commit([update('profile',{photoUrl:s3})],'profile'),true,'Referencia S3 propia');
 const saved=await call('GET','/users/profile',undefined,'profile');assert.ok(saved.ok);
 const savedFields=(await saved.json()).fields;
 assert.equal(savedFields.documentType.stringValue,'DNI');
 assert.equal(savedFields.documentNumber.stringValue,'12345678');
 assert.equal(savedFields.birthDate.stringValue,'1990-03-15');
 for(const missing of ['documentType','documentNumber','birthDate']){
   const uid='missing'+missing;const data=profile(uid);delete data[missing];
   await expect(await commit([write(`users/${uid}`,data,null,['createdAt','updatedAt'])],uid),false,'Registro sin identidad rechazado');
 }
 for(const photoUrl of [s3.replace('/profile/','/other/'),s3.replace('iot-telito-imagenes','otro-bucket'),
   s3.replace('12345678-1234-1234-1234-123456789abc','../other/foto'),s3+'?token=public',s3.replace('.jpg','.png')])
   await expect(await commit([update('profile',{photoUrl})],'profile'),false,'S3 ajeno o mal formado rechazado');
 await expect(await commit([update('profile',{photoUrl:'gs://bucket/profilePhotos/other/abc.jpg'})],'profile'),false,'No referencia foto ajena');
 await expect(await commit([update('profile',{photoUrl:'https://example.test/foto.jpg'})],'profile'),false,'No publica enlaces externos');
 await expect(await commit([update('profile',{phone:'999888777'})],'other'),false,'No edita perfil ajeno');
 for(const [uid,changes] of [['passport',{documentType:'Pasaporte',documentNumber:'AB123456'}],['ce',{documentType:'Carnet de Extranjería',documentNumber:'001234567'}]])
   await expect(await commit([write(`users/${uid}`,{...profile(uid),...changes},null,['createdAt','updatedAt'])],uid),true,'Registro de documento alternativo');
 for(const [uid,changes] of [['badDoc',{documentNumber:'123'}],['minor',{birthDate:'2020-01-01'}],['future',{birthDate:'2099-01-01'}],['invalidDate',{birthDate:'1990-02-30'}],['badPhoto',{photoUrl:'gs://bucket/profilePhotos/other/a.jpg'}]])
   await expect(await commit([write(`users/${uid}`,{...profile(uid),...changes},null,['createdAt','updatedAt'])],uid),false,'Registro invalido rechazado');
 const old={uid:'oldprofile',name:'Cuenta anterior',email:'oldprofile@example.test',role:'CLIENT',active:true};
 const seed=await call('PATCH','/users/oldprofile',{fields:fields(old)},'owner');assert.ok(seed.ok);
 await expect(await commit([update('oldprofile',{firstNames:'Cuenta',lastNames:'Anterior',name:'Cuenta Anterior',phone:'999888777',address:'Av. Antigua 123'})],'oldprofile'),true,'Cuenta anterior puede editar contactos sin inventar identidad');
 await expect(await commit([update('oldprofile',{documentType:'DNI',documentNumber:'12345678',birthDate:'1990-03-15'})],'oldprofile'),false,'No completa identidad antigua por su cuenta');
 console.log(`Perfil cliente: ${countChecks()-start} comprobaciones correctas.`);
})().catch(error=>{console.error(error);process.exitCode=1;});
