package com.example.proyecto_iotelito.ui.taxista;
import android.content.*;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
public class DetalleServicioActivity extends AppCompatActivity {
    public static Intent createIntent(Context c,int id){return TaxistaFlujo.intent(c,DetalleServicioActivity.class,id);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state); if(!TaxistaFlujo.sesion(this))return;
        setContentView(R.layout.activity_detalle_servicio);
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        TaxistaFlujo.cargar(this,vm,e->{
            if(!"SOLICITADO".equals(e.estado)){TaxistaFlujo.abrir(this,e);finish();return;}
            TaxistaFlujo.titulo(this,R.string.taxi_detalle_servicio_titulo,EstadoServicio.SOLICITADO);
            TaxistaFlujo.pasajero(this,e);
            ((TextView)findViewById(R.id.tv_origen)).setText(e.origen);
            ((TextView)findViewById(R.id.tv_destino)).setText(e.destino);
            ((TextView)findViewById(R.id.tv_distancia_tiempo)).setText(e.detalle);
            TaxistaFlujo.fila(this,R.id.row_tarifa,R.string.taxi_label_tarifa,e.cliente?getString(R.string.taxista_local_sin_tarifa):getString(R.string.taxi_tarifa_monto,e.tarifa));
            findViewById(R.id.btn_rechazar).setOnClickListener(v->finish());
            android.view.View btn=findViewById(R.id.btn_aceptar_servicio);
            btn.setOnClickListener(v->{
                if(!com.example.proyecto_iotelito.data.taxista.ConductorPreferencias.disponible(this,vm.repo.cuenta)){
                    Toast.makeText(this,R.string.taxista_local_no_disponible,Toast.LENGTH_LONG).show();return;
                }
                btn.setEnabled(false);vm.repo.ejecutar(this,()->vm.repo.aceptar(e.id),ok->{
                btn.setEnabled(true);
                if(ok){startActivity(TaxistaFlujo.intent(this,ServicioAsignadoActivity.class,e.id));finish();}
                else Toast.makeText(this,R.string.taxista_local_ocupado,Toast.LENGTH_LONG).show();
            });});
            vm.repo.error.observe(this,error->{if(error!=null)btn.setEnabled(true);});
        });
    }
}
