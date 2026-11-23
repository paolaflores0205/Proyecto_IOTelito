package com.example.proyecto_iotelito.ui.superadmin.usuarios;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.superadmin.SuperadminFormValidation;
import com.example.proyecto_iotelito.data.taxiapi.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import org.json.*;
import java.util.*;

/** Listado y cambio de estado del contrato de taxis; no actualiza MySQL/Firebase por separado. */
public class TaxistasAdminActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL="taxista_email";
    private final List<JSONObject> rows=new ArrayList<>();
    private final DriverAdapter adapter=new DriverAdapter();
    private TextView status; private MaterialButton refresh;
    private MaterialAutoCompleteTextView filter;
    private boolean busy; private int generation; private String uid; private String state;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); setContentView(R.layout.activity_superadmin_taxistas);
        AuthenticatedUser user=SessionRepository.obtener(this);
        if(user==null || user.getRole()!=UserRole.SUPERADMIN || user.getUid()==null) { finish();return; }
        uid=user.getUid();
        ((com.google.android.material.appbar.MaterialToolbar)findViewById(R.id.toolbar)).setNavigationOnClickListener(v->finish());
        status=findViewById(R.id.tv_taxi_admin_status); refresh=findViewById(R.id.btn_taxi_admin_refresh);
        filter=findViewById(R.id.actv_taxi_admin_filter);
        String[] labels={"Todas las solicitudes","Pendientes","Aprobadas","Rechazadas"};
        String[] values={null,"PENDIENTE","APROBADO","RECHAZADO"};
        filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,labels));
        filter.setText(labels[0],false);
        filter.setOnItemClickListener((p,v,pos,id)->{state=values[pos];load();});
        refresh.setOnClickListener(v->load());
        RecyclerView list=findViewById(R.id.rv_taxi_admin);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(adapter);
        if(TaxiApis.fake()) ((TextView)findViewById(R.id.tv_taxi_admin_notice)).setText(
                "Modo de prueba: datos en memoria de esta instalación. Los conductores aparecen tras entrar con su cuenta de taxista. Los cambios no modifican cuentas reales ni se comparten con otro celular.");
        load();
    }
    private boolean sameSession() {
        return !isFinishing() && FirebaseAuth.getInstance().getCurrentUser()!=null
                && uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }
    private void busy(boolean value) {busy=value;refresh.setEnabled(!value);filter.setEnabled(!value);adapter.notifyDataSetChanged();}
    private void load() {
        if(busy || !sameSession()) return;
        busy(true);status.setText("Consultando taxistas…");int ticket=++generation;
        TaxiTasks.run(this,()->TaxiApis.get(this).listarTaxistas(state),result->{
            if(!sameSession() || ticket!=generation) return;
            rows.clear();JSONArray array=result.optJSONArray("taxistas");String email=getIntent().getStringExtra(EXTRA_EMAIL);
            if(array!=null)for(int i=0;i<array.length();i++) {
                JSONObject row=array.optJSONObject(i);
                if(row!=null && (email==null || email.equalsIgnoreCase(row.optString("correo"))))rows.add(row);
            }
            busy(false);status.setText(rows.isEmpty()?"No hay taxistas para esta consulta. Verifica su registro y vinculación en el panel web.":rows.size()+" taxistas encontrados");
        },error->{if(sameSession() && ticket==generation){rows.clear();busy(false);status.setText(error+". Pulsa Actualizar lista para reintentar.");}});
    }
    private void confirm(JSONObject row) {
        if(busy || !sameSession())return;
        boolean active=!row.optBoolean("activo");EditText reason=new EditText(this);
        reason.setHint("Motivo de desactivación");reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this)
                .setTitle(active?"Activar taxista":"Desactivar taxista")
                .setMessage("Se actualizará su habilitación mediante el servicio de taxis.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",null);
        if(!active)builder.setView(reason);
        androidx.appcompat.app.AlertDialog dialog=builder.show();
        dialog.getButton(-1).setOnClickListener(v->{
            String why=reason.getText().toString().trim();String error=active?null:SuperadminFormValidation.reason(why);
            if(error!=null){reason.setError(error);return;}
            dialog.dismiss();busy(true);status.setText("Actualizando taxista…");
            TaxiTasks.run(this,()->TaxiApis.get(this).cambiarActivoTaxista(row.optLong("id"),active,why),updated->{
                if(!sameSession())return;busy(false);load();
            },message->{if(sameSession()){busy(false);status.setText(message);}});
        });
    }
    private class DriverAdapter extends RecyclerView.Adapter<DriverHolder> {
        @Override public DriverHolder onCreateViewHolder(ViewGroup parent,int type) {
            return new DriverHolder(getLayoutInflater().inflate(R.layout.item_superadmin_taxista_api,parent,false));
        }
        @Override public int getItemCount(){return rows.size();}
        @Override public void onBindViewHolder(DriverHolder holder,int position) {
            JSONObject row=rows.get(position);boolean active=row.optBoolean("activo");
            ((TextView)holder.itemView.findViewById(R.id.tv_taxi_admin_name)).setText((row.optString("nombres")+" "+row.optString("apellidos")).trim());
            ((TextView)holder.itemView.findViewById(R.id.tv_taxi_admin_details)).setText(row.optString("correo")+"\nSolicitud: "+row.optString("estadoSolicitud")+" · "+(active?"Activo":"Inactivo"));
            MaterialButton action=holder.itemView.findViewById(R.id.btn_taxi_admin_toggle);
            action.setText(active?"Desactivar":"Activar");action.setEnabled(!busy && (active || "APROBADO".equals(row.optString("estadoSolicitud"))));
            action.setOnClickListener(v->confirm(row));
        }
    }
    private static class DriverHolder extends RecyclerView.ViewHolder {DriverHolder(View item){super(item);}}
}
