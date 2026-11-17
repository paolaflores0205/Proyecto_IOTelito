// Contrato con Android. Sin credenciales ni llamadas remotas.
export function eventFor(collection, id, data) {
  const reservation = Number(collection === 'taxiRequests' ? data.reserva : id);
  if (!Number.isSafeInteger(reservation) || reservation <= 0 || reservation > 2147483647 || typeof data.clientUid!=='string' || !data.clientUid) return null;
  let type, notificationId, title, body, key, taxiId = '';
  if (collection === 'hotelCheckouts') {
    if (!Number.isFinite(data.total) || data.total < 0) return null;
    type='CHECKOUT'; notificationId=reservation;
    title='Checkout aprobado por el administrador';
    body=`El administrador aprobó tu checkout. Total del cobro simulado: S/ ${data.total.toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2})}.`;
    key=`checkout:${id}`;
  } else if (collection === 'taxiRequests') {
    const states=['SOLICITADO','ASIGNADO','EN_CAMINO','FINALIZADO'];
    const state=data.estado==='EN_TRASLADO'?'EN_CAMINO':data.estado;
    const index=states.indexOf(state);
    if(index<0 || !Number.isSafeInteger(Number(id)) || Number(id)<=0)return null;
    type='TAXI'; taxiId=String(id); notificationId=(1000000+Number(id)*4+index)|0;
    title=['Taxi solicitado','Taxista asignado','Taxi en camino','Viaje finalizado'][index];
    body=index===1?'Un conductor aceptó tu solicitud. Ya puedes consultar el seguimiento y abrir el chat.':title;
    key=`taxi:${id}:${state}`;
  } else if (collection === 'reservations' && data.estado === 'CONFIRMADA') {
    type='RESERVA';notificationId=(10000+reservation)|0;
    title='Reserva confirmada';body='Tu reserva está confirmada. Consulta sus detalles en Mis reservas.';key=`booking:${id}`;
  } else return null;
  return {key, uid:data.clientUid, data:{uid:data.clientUid,type,notificationId:String(notificationId),reservationId:String(reservation),taxiId,title,body}};
}
