package com.example.proyecto_iotelito.ui.taxista;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
public class DireccionAeropuertoActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(!TaxistaFlujo.sesion(this))return;setContentView(R.layout.activity_direccion_aeropuerto);
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        TaxistaFlujo.cargar(this,vm,e->{
            if(!"EN_TRASLADO".equals(e.estado)){TaxistaFlujo.abrir(this,e);finish();return;}
            TaxistaFlujo.titulo(this,R.string.taxi_direccion_al_aeropuerto,EstadoServicio.EN_TRASLADO);
            ((TextView)findViewById(R.id.tv_direccion)).setText(e.destino);
            ((TextView)findViewById(R.id.tv_tiempo_distancia)).setText(TaxistaFlujo.ruta(this,e));
            TaxistaFlujo.pasajero(this,e);
            findViewById(R.id.btn_llegue_al_destino).setOnClickListener(v->startActivity(TaxistaFlujo.intent(this,FinalizarServicioActivity.class,e.id)));
        });
    }
}
