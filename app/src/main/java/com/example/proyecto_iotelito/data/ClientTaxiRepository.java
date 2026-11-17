package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.data.taxiapi.*;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Mis taxis consulta la API para las reservas propias; no mezcla viajes antiguos de Firestore. */
public final class ClientTaxiRepository {
    public static final class State {
        public final List<JSONObject> rows; public final boolean loading; public final String error;
        State(List<JSONObject> rows, boolean loading, String error) { this.rows=rows; this.loading=loading; this.error=error; }
    }
    private static final java.util.concurrent.ExecutorService IO = Executors.newSingleThreadExecutor();
    private ClientTaxiRepository() { }
    public static LiveData<State> observe(Context context) {
        Context app = context.getApplicationContext();
        AuthenticatedUser account = SessionRepository.obtener(app);
        String uid = account != null && account.getRole()==UserRole.CLIENT ? account.getUid() : null;
        return new LiveData<State>(new State(Collections.emptyList(), true, null)) {
            final Handler main = new Handler(Looper.getMainLooper());
            boolean active; int generation;
            boolean sameSession() { return uid != null && FirebaseAuth.getInstance().getCurrentUser()!=null
                    && uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()); }
            final Runnable tick = new Runnable() {
                public void run() {
                    if (!active) return;
                    int ticket=generation;
                    IO.execute(() -> {
                        List<JSONObject> rows = new ArrayList<>(); String error=null;
                        try {
                            if (!sameSession()) throw new IllegalStateException("La sesión cambió.");
                            TaxiApi api=TaxiApis.get(app);
                            for (Reserva reservation : ReservaRepository.getInstance(app).obtenerTodas()) {
                                if (!sameSession()) throw new IllegalStateException("La sesión cambió.");
                                try {
                                    JSONObject service=api.verServicio(reservation.id);
                                    if(service!=null) rows.add(TaxiCodec.legacy(service, account.getEmail(), true));
                                } catch(TaxiApiException missing) { if(missing.status!=404) throw missing; }
                            }
                            rows.sort((a,b)->Long.compare(b.optLong("creado"),a.optLong("creado")));
                        } catch(Exception failure) { error=TaxiTasks.message(failure); }
                        final String problem=error;
                        main.post(() -> {
                            if(!active || ticket!=generation) return;
                            if(!sameSession()) { setValue(new State(Collections.emptyList(),false,"La sesión cambió.")); return; }
                            setValue(new State(problem==null?rows:Collections.emptyList(),false,problem));
                            main.postDelayed(this,10000);
                        });
                    });
                }
            };
            @Override protected void onActive() { active=true;generation++;main.post(tick); }
            @Override protected void onInactive() { active=false;generation++;main.removeCallbacks(tick); }
        };
    }
}
