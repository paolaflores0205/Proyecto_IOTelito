package com.example.proyecto_iotelito.notification;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.proyecto_iotelito.data.NotificacionRepository;
import com.example.proyecto_iotelito.model.NotificacionCliente;
import com.example.proyecto_iotelito.data.taxista.local.AvisoConductorEntity;
import com.example.proyecto_iotelito.data.taxista.local.ConductorDatabase;
import com.example.proyecto_iotelito.ui.taxista.TaxistaNotificaciones;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Collections;
import java.util.Map;

/** Mensajes de datos: se comprueba el destinatario antes de mostrar cualquier contenido. */
public class ClientMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) { ClientPushRegistration.refresh(this,token); }
    @Override public void onMessageReceived(RemoteMessage message) {
        Map<String,String> data=message.getData();
        String uid=data.get("uid"),binding=data.get("binding");
        if(!ClientPushRegistration.matches(this,uid,binding))return;
        try {
            int id=Integer.parseInt(data.get("notificationId")),reservation=Integer.parseInt(data.get("reservationId"));
            if(reservation<=0)return;
            String body=data.get("body"),title=data.get("title");
            if(body==null||title==null||body.length()>1000||title.length()>200)return;
            if ("OFERTA".equals(data.get("taxiEvent"))) {
                mostrarOferta(uid,binding,reservation,title,body);
                return;
            }
            if (ClientPushRegistration.esTaxista(this)) return;
            NotificacionCliente.Tipo type=NotificacionCliente.Tipo.valueOf(data.get("type"));
            if(type!=NotificacionCliente.Tipo.CHECKOUT&&type!=NotificacionCliente.Tipo.TAXI&&type!=NotificacionCliente.Tipo.RESERVA)return;
            NotificacionRepository.forOwner(this,uid).guardarSiNoExiste(id,type,body,reservation);
            if(!ClientPushRegistration.matches(this,uid,binding))return;
            if(type==NotificacionCliente.Tipo.CHECKOUT){
                ClientCheckoutAlerts.showForOwner(this,uid,Collections.singletonList(new NotificacionCliente(
                    id,type,body,System.currentTimeMillis(),false,reservation)));
            }else{
                int taxi=type==NotificacionCliente.Tipo.TAXI?Integer.parseInt(data.get("taxiId")):-1;
                ClienteNotificationManager.mostrarPush(this,uid,id,type,reservation,taxi,title,body);
            }
        }catch(IllegalArgumentException|NullPointerException ignored){
            android.util.Log.w("ClientPush","Mensaje descartado por formato inválido.");
        }
    }

    private void mostrarOferta(String uid,String binding,int reserva,String titulo,String texto) {
        synchronized (ClientPushRegistration.class) {
            if (!ClientPushRegistration.esTaxista(this) || !ClientPushRegistration.matches(this,uid,binding)) return;
            var usuario = FirebaseAuth.getInstance().getCurrentUser();
            if (usuario == null || usuario.getEmail() == null || !uid.equals(usuario.getUid())) return;
            AvisoConductorEntity aviso = new AvisoConductorEntity();
            aviso.cuenta = usuario.getEmail();
            aviso.clave = "solicitud:" + reserva;
            aviso.servicio = reserva;
            aviso.tipo = "solicitud";
            aviso.titulo = titulo;
            aviso.texto = texto;
            aviso.fecha = System.currentTimeMillis();
            var dao = ConductorDatabase.get(this).dao();
            dao.aviso(aviso);
            if (dao.pendientes(aviso.cuenta).stream().anyMatch(row -> row.clave.equals(aviso.clave))
                    && TaxistaNotificaciones.publicar(this,aviso)) {
                dao.publicado(aviso.cuenta,aviso.clave);
            }
        }
    }
}
