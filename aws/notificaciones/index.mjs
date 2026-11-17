import { createHash, randomUUID } from 'node:crypto';
import { SecretsManagerClient, GetSecretValueCommand } from '@aws-sdk/client-secrets-manager';
import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, FieldPath, Timestamp } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { eventFor } from './events.mjs';
let services;
async function connect() {
  if(services)return services;
  if(!process.env.FIREBASE_SECRET_ARN || !process.env.FIREBASE_PROJECT_ID)throw Error('Falta configurar secreto/proyecto');
  const secret=await new SecretsManagerClient({}).send(new GetSecretValueCommand({SecretId:process.env.FIREBASE_SECRET_ARN}));
  const account=JSON.parse(secret.SecretString);
  if(account.project_id!==process.env.FIREBASE_PROJECT_ID)throw Error('El secreto pertenece a otro proyecto');
  const app=initializeApp({credential:cert(account),projectId:account.project_id});
  services={db:getFirestore(app),messaging:getMessaging(app)};
  return services;
}

export async function deliver(db,messaging,event,device) {
  const {token,binding}=device.data();
  if(typeof token!=='string'||typeof binding!=='string'||device.id!==binding)return false;
  const id=createHash('sha256').update(`${event.uid}:${event.key}:${binding}`).digest('hex');
  const ref=db.collection('pushDeliveries').doc(id),lease=randomUUID(),now=Date.now();
  const claimed=await db.runTransaction(async tx=>{
    const old=await tx.get(ref),data=old.data();
    if(data?.sent || (data?.leaseUntil?.toMillis()>now))return false;
    tx.set(ref,{uid:event.uid,event:event.key,binding,lease,leaseUntil:Timestamp.fromMillis(now+120000),sent:false});
    return true;
  });
  if(!claimed)return false;
  try {
    // Solo datos: Android comprueba UID y sesión antes de mostrar cualquier contenido.
    await messaging.send({token,data:{...event.data,binding},android:{priority:'high',ttl:86400000}});
    await db.runTransaction(async tx=>{
      const current=await tx.get(ref);
      if(current.data()?.lease===lease)tx.update(ref,{sent:true,sentAt:Timestamp.now(),leaseUntil:Timestamp.fromMillis(0)});
    });
    return true;
  } catch(error) {
    const invalid=['messaging/registration-token-not-registered','messaging/invalid-registration-token'].includes(error.code);
    if(invalid)await db.runTransaction(async tx=>{
      const current=await tx.get(device.ref);
      if(current.data()?.token===token)tx.delete(device.ref);
    });
    await db.runTransaction(async tx=>{
      const current=await tx.get(ref);
      if(current.data()?.lease===lease)tx.update(ref,{sent:invalid,leaseUntil:Timestamp.fromMillis(0),lastError:String(error.code||'send-failed')});
    });
    // No registrar tokens, claves, correos ni contenido en CloudWatch.
    if(!invalid)throw Error('Envío FCM pendiente de reintento');
    return false;
  }
}

// EventBridge rate(1 minute). Escala de demostración: tres colecciones paginadas.
export async function handler(event,context) {
  if(event?.requestContext)throw Error('Solo EventBridge o prueba manual; no crear endpoint público');
  const start=Date.parse(process.env.PUSH_START_AT||'');
  if(!Number.isFinite(start))throw Error('PUSH_START_AT debe ser una fecha ISO de activación');
  const {db,messaging}=await connect();let scanned=0,sent=0;
  const max=Number(process.env.MAX_DOCUMENTS||2000),profiles=new Map();
  if(!Number.isInteger(max)||max<1||max>10000)throw Error('MAX_DOCUMENTS inválido');
  for(const collection of ['hotelCheckouts','taxiRequests','reservations']) {
    let cursor;
    for(;;) {
      let query=db.collection(collection).orderBy(FieldPath.documentId()).limit(100);
      if(cursor)query=query.startAfter(cursor);
      const page=await query.get();if(page.empty)break;
      for(const doc of page.docs) {
        if(++scanned>max)throw Error('Límite de lectura alcanzado; revisar escala antes de aumentar MAX_DOCUMENTS');
        if(context?.getRemainingTimeInMillis && context.getRemainingTimeInMillis()<15000)throw Error('Tiempo insuficiente; se reintentará');
        const data=doc.data();
        const when=collection==='taxiRequests'?doc.updateTime:data.createdAt;
        if(!when?.toMillis || when.toMillis()<start)continue;
        const item=eventFor(collection,doc.id,data);if(!item)continue;
        if(!profiles.has(item.uid)) {
          const user=await db.collection('users').doc(item.uid).get(),profile=user.data();
          const devices=profile?.uid===item.uid&&profile?.active===true&&profile?.role==='CLIENT'
            ?(await user.ref.collection('notificationDevices').get()).docs:[];
          profiles.set(item.uid,devices);
        }
        for(const device of profiles.get(item.uid))if(await deliver(db,messaging,item,device))sent++;
      }
      cursor=page.docs.at(-1);
    }
  }
  console.log(JSON.stringify({scanned,sent}));return {ok:true,scanned,sent};
}
