package com.example.proyecto_iotelito.ui.taxista;
import android.content.*;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;
public class ServicioAsignadoActivity extends AppCompatActivity {
    public static Intent createIntent(Context c,SolicitudServicio s){return TaxistaFlujo.intent(c,ServicioAsignadoActivity.class,s.id);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(!TaxistaFlujo.sesion(this))return;setContentView(R.layout.activity_servicio_asignado);
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        TaxistaFlujo.cargar(this,vm,e->{
            if(!"ASIGNADO".equals(e.estado)){TaxistaFlujo.abrir(this,e);finish();return;}
            TaxistaFlujo.titulo(this,R.string.taxi_servicio_asignado_titulo,EstadoServicio.ASIGNADO);
            TaxistaFlujo.pasajero(this,e);
            ((TextView)findViewById(R.id.tv_ruta_resumen)).setText(TaxistaFlujo.ruta(this,e));
            TaxistaFlujo.avanzar(this,vm,R.id.btn_iniciar_recorrido,e.id,EstadoServicio.EN_CAMINO,DireccionHotelActivity.class);
        });
    }
}
