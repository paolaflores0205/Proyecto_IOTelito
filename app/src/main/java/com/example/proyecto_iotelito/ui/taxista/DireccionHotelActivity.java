package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxistaSampleData;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

/**
 * Recorrido en curso hacia el hotel de recojo. Contenido de ejemplo;
 * llega tras iniciar el recorrido de un servicio asignado.
 */
public class DireccionHotelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion_hotel);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_direccion_al_hotel);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.EN_CAMINO);

        SolicitudServicio servicio = TaxistaSampleData.servicioActivo();
        ((TextView) findViewById(R.id.tv_direccion)).setText(servicio.hotel);
        ((TextView) findViewById(R.id.tv_instruccion)).setText("Gira a la derecha en 30m");
        ((TextView) findViewById(R.id.tv_tiempo_distancia)).setText(servicio.distanciaTiempo);

        findViewById(R.id.btn_llegue_al_hotel).setOnClickListener(v ->
                startActivity(new Intent(this, DireccionAeropuertoActivity.class)));
    }
}
