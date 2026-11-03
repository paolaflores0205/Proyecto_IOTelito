package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.json.JSONObject;

/** Puente remoto de taxi. Las pantallas y los datos locales anteriores siguen siendo compatibles. */
public final class FirebaseTaxiStore {
    private FirebaseTaxiStore() { }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static DocumentReference pedido(int id) { return db().collection("taxiRequests").document(String.valueOf(id)); }
    public static boolean usaFirebase(Context context) {
        FirebaseUser firebase = FirebaseAuth.getInstance().getCurrentUser();
        com.example.proyecto_iotelito.model.auth.AuthenticatedUser session=SessionRepository.obtener(context);
        return firebase!=null && session!=null && session.getEmail().equalsIgnoreCase(firebase.getEmail())
                && (session.getRole()==UserRole.CLIENT || session.getRole()==UserRole.TAXISTA);
    }
    public static String uid() {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null)throw new IllegalStateException("Sesión Firebase requerida");
        return user.getUid();
    }
    public static boolean mismaSesion(String uid) {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        return user!=null && uid.equals(user.getUid());
    }
    public static <T> T esperar(Task<T> task) {
        try { return Tasks.await(task); }
        catch(InterruptedException error) {Thread.currentThread().interrupt();throw new IllegalStateException("Operación interrumpida",error);}
        catch(ExecutionException error) {throw new IllegalStateException("No se pudo completar la operación de taxi en Firebase",error.getCause());}
    }

    public static int porReserva(int reservaId) {
        String uid=uid();
        DocumentSnapshot data=esperar(pedido(reservaId).get(Source.SERVER));
        if(!mismaSesion(uid))throw new IllegalStateException("La sesión cambió");
        return data.exists() && uid.equals(data.getString("clientUid")) ? reservaId : -1;
    }

    public static int crear(Reserva reserva,String nombre,String hotel,String origen,String hora,int pasajeros,int maletas) {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        String uid=uid();
        // Una reserva remota tiene un ID global; una solicitud usa ese mismo documento.
        Map<String,Object> data=new HashMap<>();
        data.put("id",reserva.id);data.put("reserva",reserva.id);data.put("clientUid",uid);
        data.put("cliente",user.getEmail());data.put("nombre",nombre);data.put("hotel",hotel);
        data.put("origen",origen);data.put("destino","Aeropuerto Jorge Chávez");data.put("hora",hora);
        data.put("pasajeros",pasajeros);data.put("maletas",maletas);data.put("estado","SOLICITADO");
        data.put("creado",FieldValue.serverTimestamp());data.put("taxistaUid","");
        data.put("taxista","");data.put("conductor","");data.put("finalizado",null);
        return esperar(db().runTransaction(transaction->{
            if(!mismaSesion(uid))throw new IllegalStateException("La sesión cambió");
            DocumentReference ref=pedido(reserva.id);
            DocumentSnapshot existing=transaction.get(ref);
            if(existing.exists()) {
                if(!uid.equals(existing.getString("clientUid")))throw new IllegalStateException("Reserva de otra cuenta");
                return reserva.id;
            }
            transaction.set(ref,data);
            return reserva.id;
        }));
    }

    public static JSONObject buscar(int id,boolean cliente) {
        String uid=uid();
        DocumentSnapshot data=esperar(pedido(id).get(Source.SERVER));
        if(!mismaSesion(uid))throw new IllegalStateException("La sesión cambió");
        return json(data,cliente);
    }

    public static JSONObject json(DocumentSnapshot data,boolean cliente) {
        if(!data.exists())return null;
        Map<String,Object> fields=new HashMap<>(data.getData());
        for(String campo:new String[]{"creado","finalizado"}) {
            Object value=fields.get(campo);
            fields.put(campo,value instanceof Timestamp ? ((Timestamp)value).toDate().getTime() : 0L);
        }
        if(cliente)fields.put("estado",TaxiRemoteState.paraCliente(data.getString("estado")));
        return new JSONObject(fields);
    }

    public static LiveData<JSONObject> observar(int id) {
        String uid=uid();
        return new MutableLiveData<JSONObject>() {
            private ListenerRegistration listener;
            @Override protected void onActive() {
                listener=pedido(id).addSnapshotListener((data,error)->{
                    if(!mismaSesion(uid)) {setValue(null);return;}
                    if(error!=null) {Log.e("FirebaseTaxi","No se pudo observar la solicitud",error);return;}
                    if(data!=null)setValue(json(data,true));
                });
            }
            @Override protected void onInactive() {if(listener!=null){listener.remove();listener=null;}}
        };
    }

    /** Devuelve false si otro taxista ganó o si esta cuenta ya tiene otro servicio activo. */
    public static boolean avanzar(String uid,int id,String siguiente,String email,String nombre) {
        return esperar(db().runTransaction(transaction->{
            if(!mismaSesion(uid))throw new IllegalStateException("La sesión cambió");
            DocumentReference ref=pedido(id);
            DocumentReference lock=db().collection("taxiDrivers").document(uid);
            DocumentSnapshot data=transaction.get(ref);
            DocumentSnapshot driver=transaction.get(lock);
            DocumentSnapshot perfil=transaction.get(db().collection("taxistas").document(uid));
            if(!data.exists()||!Boolean.TRUE.equals(perfil.getBoolean("activo")))return false;
            String estado=data.getString("estado"), asignado=data.getString("taxistaUid");
            String active=driver.exists()?driver.getString("activeRequestId"):"";
            // Reintento tras guardar en servidor pero antes de actualizar Room.
            if(siguiente.equals(estado)&&uid.equals(asignado))return true;
            if(!TaxiRemoteState.permite(estado,siguiente))return false;
            Map<String,Object> changes=new HashMap<>();changes.put("estado",siguiente);
            if("ASIGNADO".equals(siguiente)) {
                if(!Boolean.TRUE.equals(perfil.getBoolean("disponible"))
                        ||(active!=null&&!active.isEmpty()))return false;
                changes.put("taxistaUid",uid);changes.put("taxista",email);changes.put("conductor",nombre);
                transaction.set(lock,Collections.singletonMap("activeRequestId",String.valueOf(id)));
            } else {
                if(!uid.equals(asignado)||!String.valueOf(id).equals(active))return false;
                if("FINALIZADO".equals(siguiente)) {
                    changes.put("finalizado",FieldValue.serverTimestamp());
                    transaction.set(lock,Collections.singletonMap("activeRequestId",""));
                }
            }
            transaction.update(ref,changes);
            return true;
        }));
    }
}
