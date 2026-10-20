package com.example.proyecto_iotelito.ui.taxista;
import android.app.Activity;
import android.content.*;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.taxista.*;
import com.example.proyecto_iotelito.data.taxista.local.*;
import com.example.proyecto_iotelito.model.auth.*;
import java.util.function.Consumer;

final class TaxistaFlujo {
    static final String ID=TaxiRepository.EXTRA_ID;
    static TaxistaViewModel model(AppCompatActivity a) {
        TaxistaViewModel vm=new ViewModelProvider(a).get(TaxistaViewModel.class);
        vm.repo.error.observe(a,error->{if(error!=null) Toast.makeText(a,error,Toast.LENGTH_LONG).show();});
        return vm;
    }
    static boolean sesion(Activity a) {
        AuthenticatedUser u=SessionRepository.obtener(a);
        if(u!=null&&u.getRole()==UserRole.TAXISTA) return true;
        a.startActivity(new Intent(a,com.example.proyecto_iotelito.ui.auth.LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); a.finish(); return false;
    }
    static Intent intent(Context c,Class<?> clase,int id) {return new Intent(c,clase).putExtra(ID,id);}
    static void cargar(AppCompatActivity a,TaxistaViewModel vm,Consumer<ServicioConductorEntity> done) {
        if(!(a instanceof DetalleServicioActivity))navegacion(a);
        int id=a.getIntent().getIntExtra(ID,0);
        vm.repo.ejecutar(a,()->vm.repo.buscar(id),e->{if(a.isFinishing())return; if(e==null){a.finish();return;} done.accept(e);});
    }
    static void navegacion(AppCompatActivity a){
        android.view.ViewGroup content=a.findViewById(android.R.id.content);
        if(a.findViewById(R.id.bottom_nav)!=null||content.getChildCount()==0)return;
        View root=content.getChildAt(0);content.removeView(root);
        android.widget.LinearLayout shell=new android.widget.LinearLayout(a);
        shell.setOrientation(android.widget.LinearLayout.VERTICAL);
        shell.addView(root,new android.widget.LinearLayout.LayoutParams(-1,0,1f));
        com.google.android.material.bottomnavigation.BottomNavigationView nav=
                (com.google.android.material.bottomnavigation.BottomNavigationView)a.getLayoutInflater()
                        .inflate(R.layout.view_taxista_service_nav,shell,false);
        shell.addView(nav);content.addView(shell,new android.view.ViewGroup.LayoutParams(-1,-1));
        nav.setSelectedItemId(R.id.nav_taxi_inicio);
        nav.setOnItemSelectedListener(item->{
            Intent i=new Intent(a,TaxistaMainActivity.class).putExtra(TaxistaMainActivity.EXTRA_OPEN_TAB,item.getItemId())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            a.startActivity(i);a.finish();return true;
        });
        // Inicio ya está seleccionado, pero volver a tocarlo también debe navegar.
        nav.setOnItemReselectedListener(item->{
            a.startActivity(new Intent(a,TaxistaMainActivity.class).putExtra(TaxistaMainActivity.EXTRA_OPEN_TAB,item.getItemId())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));a.finish();
        });
    }
    static void abrir(Context c,ServicioConductorEntity e) {
        Class<?> clase;
        switch(e.estado) {
            case "ASIGNADO": clase=ServicioAsignadoActivity.class; break;
            case "EN_CAMINO": clase=DireccionHotelActivity.class; break;
            case "EN_TRASLADO": clase=DireccionAeropuertoActivity.class; break;
            case "FINALIZADO": clase=ServicioFinalizadoActivity.class; break;
            default: clase=DetalleServicioActivity.class;
        }
        c.startActivity(intent(c,clase,e.id));
    }
    static void titulo(AppCompatActivity a,int text,EstadoServicio estado) {
        View header=a.findViewById(R.id.sub_header);
        header.findViewById(R.id.iv_back).setOnClickListener(v->a.finish());
        ((TextView)header.findViewById(R.id.tv_title)).setText(text);
        EstadoServicioUi.pintarPill(header.findViewById(R.id.tv_estado_pill),estado);
    }
    static void pasajero(AppCompatActivity a,ServicioConductorEntity e) {
        View row=a.findViewById(R.id.row_pasajero); if(row==null)return;
        ((TextView)row.findViewById(R.id.tv_avatar_inicial)).setText(e.iniciales);
        ((TextView)row.findViewById(R.id.tv_pasajero_nombre)).setText(e.nombre);
        ((TextView)row.findViewById(R.id.tv_pasajero_detalle)).setText(e.hotel);
        row.findViewById(R.id.iv_chat).setOnClickListener(v->a.startActivity(
                ChatPasajeroTaxistaActivity.createIntent(a,e.nombre,e.iniciales).putExtra(ID,e.id)));
    }
    static void avanzar(AppCompatActivity a,TaxistaViewModel vm,int button,int id,EstadoServicio estado,Class<?> siguiente) {
        View btn=a.findViewById(button); btn.setOnClickListener(v->{
            btn.setEnabled(false);
            vm.repo.ejecutar(a,()->vm.repo.avanzar(id,estado),ok->{
                btn.setEnabled(true); if(ok&&!a.isFinishing()){a.startActivity(intent(a,siguiente,id)); a.finish();}
                else Toast.makeText(a,R.string.taxista_local_transicion,Toast.LENGTH_LONG).show();
            });
        });
        vm.repo.error.observe(a,error->{if(error!=null)btn.setEnabled(true);});
    }
    static String ruta(Context c,ServicioConductorEntity e) {
        return e.distancia!=null&&!e.distancia.isEmpty()&&e.duracion!=null&&!e.duracion.isEmpty()
                ?e.distancia+" · "+e.duracion:c.getString(R.string.taxista_local_ruta_simulada);
    }
    static void fila(Activity a,int id,int etiqueta,String valor) {
        View row=a.findViewById(id);
        ((TextView)row.findViewById(R.id.tv_label)).setText(etiqueta);
        ((TextView)row.findViewById(R.id.tv_value)).setText(valor==null||valor.isEmpty()?a.getString(R.string.taxista_local_sin_dato):valor);
    }
}
