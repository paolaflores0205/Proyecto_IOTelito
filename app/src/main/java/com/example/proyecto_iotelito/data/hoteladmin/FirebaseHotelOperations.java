package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.Task;
import java.time.*;
import java.util.*;
import java.util.function.*;

/** Operaciones limitadas al hotel de la sesión verificada; Firestore valida el vínculo otra vez. */
public final class FirebaseHotelOperations {
    public static final ZoneId ZONE=ZoneId.of("America/Lima");
    public static final class State<T>{public final List<T> rows;public final String error;public final boolean loading;
        State(List<T> rows,String error,boolean loading){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));this.error=error;this.loading=loading;}}
    private final Context app;private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    public final int hotelId;public final String uid;
    public FirebaseHotelOperations(Context context){app=context.getApplicationContext();AuthenticatedUser u=SessionRepository.obtener(app);
        if(u==null||u.getUid()==null||u.getHotelId()==null||u.getRole()!=UserRole.HOTEL_ADMIN)throw new IllegalStateException("Sesión admin requerida");
        uid=u.getUid();hotelId=u.getHotelId();}
    private void verify(){AuthenticatedUser u=SessionRepository.obtener(app);
        if(u==null||!uid.equals(u.getUid())||!Objects.equals(hotelId,u.getHotelId())||u.getRole()!=UserRole.HOTEL_ADMIN
                ||FirebaseAuth.getInstance().getCurrentUser()==null||!uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
            throw new IllegalStateException("La sesión o el hotel asignado cambió");}
    private <T> LiveData<State<T>> observe(Query query,Function<DocumentSnapshot,T> decode){
        return new LiveData<State<T>>(new State<>(Collections.emptyList(),null,true)){
            ListenerRegistration listener;int generation;
            @Override protected void onActive(){final int ticket=++generation;
                listener=query.addSnapshotListener(MetadataChanges.INCLUDE,(snapshot,error)->{
                    if(ticket!=generation||!hasActiveObservers())return;
                    try{verify();if(error!=null)throw error;if(snapshot==null)throw new IllegalStateException();
                        if(snapshot.getMetadata().isFromCache())return;
                        List<T> rows=new ArrayList<>();for(DocumentSnapshot doc:snapshot){T value=decode.apply(doc);if(value!=null)rows.add(value);}
                        setValue(new State<>(rows,null,false));
                    }catch(Exception failure){setValue(new State<>(Collections.emptyList(),"No se pudieron consultar los datos del hotel. Revisa conexión, sesión y permisos.",false));}
                });}
            @Override protected void onInactive(){generation++;if(listener!=null)listener.remove();listener=null;setValue(new State<>(Collections.emptyList(),null,true));}
        };
    }
    public LiveData<State<ReservaAdmin>> reservations(){return observe(db.collection("reservations").whereEqualTo("hotelId",hotelId),
            doc->HotelOperationsCodec.decode(doc.getId(),doc.getData(),hotelId,LocalDate.now(ZONE)));}
    public LiveData<State<HotelOperationsCodec.Receipt>> receipts(){return observe(db.collection("hotelCheckouts").whereEqualTo("hotelId",hotelId),doc->{
        com.google.firebase.Timestamp timestamp=doc.getTimestamp("createdAt");
        if(timestamp==null)return null;
        return new HotelOperationsCodec.Receipt(timestamp.toDate().toInstant().atZone(ZONE).toLocalDateTime(),doc.getDouble("total"));});}
    public void load(int id,Consumer<ReservaAdmin> success,Consumer<String> error){
        db.collection("reservations").document(String.valueOf(id)).get(Source.SERVER).addOnSuccessListener(doc->{
            try{verify();if(!doc.exists())throw new IllegalArgumentException();ReservaAdmin row=HotelOperationsCodec.decode(doc.getId(),doc.getData(),hotelId,LocalDate.now(ZONE));
                if(row==null)throw new IllegalArgumentException();
                if(row.estado != ReservaAdmin.EstadoCheckout.FINALIZADA) { success.accept(row); return; }
                db.collection("hotelCheckouts").document(String.valueOf(id)).get(Source.SERVER).addOnSuccessListener(receipt -> {
                    try {
                        verify();
                        if(receipt.exists()) {
                            double extra=receipt.getDouble("extra");
                            row.tieneComprobante=true;
                            if(extra>0) row.consumos.add(new com.example.proyecto_iotelito.model.hoteladmin.ItemConsumo(
                                    "Cargo adicional del cierre",extra,receipt.getString("reason"),receipt.getString("observation")));
                        }
                        success.accept(row);
                    } catch(RuntimeException failure) { error.accept("No se pudo verificar el comprobante del cierre."); }
                }).addOnFailureListener(failure -> error.accept("No se pudo consultar el comprobante en el servidor."));
            }catch(RuntimeException failure){error.accept("No se encontró una reserva válida de tu hotel.");}
        }).addOnFailureListener(e->error.accept("No se pudo consultar la reserva en el servidor."));}
    public Task<Void> checkout(int id,double extra,String reason,String observation){
        verify();if(extra>0&&(reason.trim().isEmpty()||observation.trim().isEmpty()))throw new IllegalArgumentException("Completa el motivo y observación.");
        DocumentReference booking=db.collection("reservations").document(String.valueOf(id)),receipt=db.collection("hotelCheckouts").document(String.valueOf(id));
        return db.runTransaction(tx->{verify();DocumentSnapshot before=tx.get(booking),existing=tx.get(receipt);
            if(existing.exists())return null;
            if(!before.exists())throw new IllegalArgumentException("Reserva inexistente");
            ReservaAdmin row=HotelOperationsCodec.decode(before.getId(),before.getData(),hotelId,LocalDate.now(ZONE));
            if(row==null||row.estado!=ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE||!"CONFIRMADA".equals(before.getString("estado")))
                throw new IllegalStateException("La reserva no permite checkout o ya fue finalizada.");
            Map<String,Object> data=new HashMap<>();data.put("reservationId",String.valueOf(id));data.put("hotelId",hotelId);
            data.put("clientUid",before.getString("clientUid"));data.put("base",row.precioAlojamiento);data.put("extra",extra);
            data.put("total",HotelOperationsCodec.total(row.precioAlojamiento,extra));data.put("reason",extra>0?reason.trim():"");
            data.put("observation",extra>0?observation.trim():"");data.put("createdAt",FieldValue.serverTimestamp());data.put("createdByUid",uid);
            tx.set(receipt,data);tx.update(booking,"estado","COMPLETADA");return null;});
    }
}
