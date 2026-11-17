package com.example.proyecto_iotelito.data;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.data.taxista.FirebaseTaxiStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
import org.json.JSONObject;

/** Solicitudes propias, independientes de la pestaña y el estado de la reserva. */
public final class ClientTaxiRepository {
    public static final class State {
        public final List<JSONObject> rows; public final boolean loading; public final String error;
        State(List<JSONObject> rows,boolean loading,String error){this.rows=rows;this.loading=loading;this.error=error;}
    }
    private ClientTaxiRepository() { }
    public static LiveData<State> observe(Context context){
        AuthenticatedUser user=SessionRepository.obtener(context);
        String uid=user!=null&&user.getRole()==UserRole.CLIENT?user.getUid():null;
        return new LiveData<State>(new State(Collections.emptyList(),true,null)){
            ListenerRegistration listener;int generation;
            @Override protected void onActive(){
                int ticket=++generation;
                if(uid==null){setValue(new State(Collections.emptyList(),false,"Inicia sesión como cliente."));return;}
                listener=FirebaseTaxiStore.db().collection("taxiRequests").whereEqualTo("clientUid",uid)
                    .addSnapshotListener((snapshot,error)->{
                        if(ticket!=generation)return;
                        if(FirebaseAuth.getInstance().getCurrentUser()==null||!uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                            setValue(new State(Collections.emptyList(),false,"La sesión cambió."));return;
                        }
                        if(error!=null){setValue(new State(Collections.emptyList(),false,"No se pudieron cargar tus taxis. Revisa la conexión y reintenta."));return;}
                        List<JSONObject> rows=new ArrayList<>();
                        if(snapshot!=null)for(DocumentSnapshot doc:snapshot.getDocuments())rows.add(FirebaseTaxiStore.json(doc,true));
                        rows.sort((a,b)->Long.compare(b.optLong("creado"),a.optLong("creado")));
                        setValue(new State(rows,false,null));
                    });
            }
            @Override protected void onInactive(){generation++;if(listener!=null)listener.remove();listener=null;}
        };
    }
}
