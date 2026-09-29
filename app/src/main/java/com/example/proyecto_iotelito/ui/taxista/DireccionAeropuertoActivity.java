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
 * Traslado en curso con el pasajero a bordo, rumbo al aeropuerto.
 * Contenido de ejemplo; llega tras confirmar la llegada al hotel.
 */
public class DireccionAeropuertoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion_aeropuerto);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_direccion_al_aeropuerto);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.EN_TRASLADO);

        SolicitudServicio servicio = TaxistaSampleData.servicioActivo();
        ((TextView) findViewById(R.id.tv_direccion)).setText(servicio.destino);
        ((TextView) findViewById(R.id.tv_tiempo_distancia)).setText(
                getString(R.string.taxi_tiempo_distancia, servicio.viajeDuracion, servicio.viajeDistancia));

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText(servicio.pasajeroIniciales);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText(servicio.pasajeroNombre);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(
                getString(R.string.taxi_hotel_un_pasajero, servicio.hotel));
        rowPasajero.findViewById(R.id.iv_chat).setOnClickListener(v ->
                startActivity(ChatPasajeroTaxistaActivity.createIntent(this, servicio.pasajeroNombre, servicio.pasajeroIniciales)));

        findViewById(R.id.btn_llegue_al_destino).setOnClickListener(v ->
                startActivity(new Intent(this, FinalizarServicioActivity.class)));
    }
}
