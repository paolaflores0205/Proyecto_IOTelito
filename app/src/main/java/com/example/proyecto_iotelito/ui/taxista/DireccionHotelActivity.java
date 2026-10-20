package com.example.proyecto_iotelito.ui.taxista;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
public class DireccionHotelActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(!TaxistaFlujo.sesion(this))return;setContentView(R.layout.activity_direccion_hotel);
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        TaxistaFlujo.cargar(this,vm,e->{
            if(!"EN_CAMINO".equals(e.estado)){TaxistaFlujo.abrir(this,e);finish();return;}
            TaxistaFlujo.titulo(this,R.string.taxi_direccion_al_hotel,EstadoServicio.EN_CAMINO);
            ((TextView)findViewById(R.id.tv_direccion)).setText(e.origen);
            ((TextView)findViewById(R.id.tv_instruccion)).setText(R.string.taxista_local_mapa_demo);
            ((TextView)findViewById(R.id.tv_tiempo_distancia)).setText(e.detalle);
            TaxistaFlujo.avanzar(this,vm,R.id.btn_llegue_al_hotel,e.id,EstadoServicio.EN_TRASLADO,DireccionAeropuertoActivity.class);
        });
    }
}
