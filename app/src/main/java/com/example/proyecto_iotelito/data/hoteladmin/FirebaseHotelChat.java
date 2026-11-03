package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Un hilo por reserva. Ambos participantes leen exactamente la misma colección. */
public final class FirebaseHotelChat {
    private final Context app;
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final String uid;
    private final UserRole role;
    public FirebaseHotelChat(Context context) {
        app=context.getApplicationContext();
        AuthenticatedUser user=SessionRepository.obtener(app);
        if(user==null||user.getUid()==null||(user.getRole()!=UserRole.CLIENT&&user.getRole()!=UserRole.HOTEL_ADMIN))
            throw new IllegalStateException("Inicia sesión como cliente o administrador");
        uid=user.getUid();role=user.getRole();
    }
    private void verify(){
        AuthenticatedUser user=SessionRepository.obtener(app);
        if(user==null||!uid.equals(user.getUid())||role!=user.getRole()||FirebaseAuth.getInstance().getCurrentUser()==null
                ||!uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))throw new IllegalStateException("La sesión cambió");
    }
    public LiveData<FirebaseHotelOperations.State<DocumentSnapshot>> bookings(){
        AuthenticatedUser user=SessionRepository.obtener(app);
        Query query=role==UserRole.CLIENT?db.collection("reservations").whereEqualTo("clientUid",uid):
                db.collection("reservations").whereEqualTo("hotelId",user.getHotelId());
        return observe(query);
    }
    private LiveData<FirebaseHotelOperations.State<DocumentSnapshot>> observe(Query query){
        return new LiveData<FirebaseHotelOperations.State<DocumentSnapshot>>(new FirebaseHotelOperations.State<>(Collections.emptyList(),null,true)){
            ListenerRegistration listener;int generation;
            @Override protected void onActive(){final int ticket=++generation;listener=query.addSnapshotListener(MetadataChanges.INCLUDE,(snapshot,error)->{
                if(ticket!=generation||!hasActiveObservers())return;
                try{verify();if(error!=null)throw error;if(snapshot==null)throw new IllegalStateException();if(snapshot.getMetadata().isFromCache())return;
                    setValue(new FirebaseHotelOperations.State<>(snapshot.getDocuments(),null,false));
                }catch(Exception e){setValue(new FirebaseHotelOperations.State<>(Collections.emptyList(),"No se pudieron consultar los mensajes. Revisa la conexión o vuelve a iniciar sesión.",false));}
            });}
            @Override protected void onInactive(){generation++;if(listener!=null)listener.remove();listener=null;setValue(new FirebaseHotelOperations.State<>(Collections.emptyList(),null,true));}
        };
    }
    public LiveData<FirebaseHotelOperations.State<DocumentSnapshot>> messages(int booking){
        if(booking<=0)throw new IllegalArgumentException("Selecciona una reserva");
        return observe(db.collection("reservations").document(String.valueOf(booking)).collection("messages").orderBy("createdAt"));
    }
    public static Mensaje decode(DocumentSnapshot doc){
        com.google.firebase.Timestamp time=doc.getTimestamp("createdAt");
        String formatted=time==null?"":time.toDate().toInstant().atZone(FirebaseHotelOperations.ZONE).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        return new Mensaje(doc.getString("text"),"CLIENT".equals(doc.getString("senderRole")),formatted);
    }
    public Task<Void> send(int booking,String text){
        verify();text=text.trim();if(booking<=0||text.isEmpty()||text.length()>4000)throw new IllegalArgumentException("Escribe entre 1 y 4000 caracteres");
        Map<String,Object> data=new HashMap<>();data.put("senderUid",uid);data.put("senderRole",role.name());
        data.put("text",text);data.put("createdAt",FieldValue.serverTimestamp());
        return db.collection("reservations").document(String.valueOf(booking)).collection("messages").document().set(data);
    }
}
