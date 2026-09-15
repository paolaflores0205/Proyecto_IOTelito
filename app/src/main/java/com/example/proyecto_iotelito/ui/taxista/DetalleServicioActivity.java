package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Detalle de una solicitud de servicio antes de aceptarla: pasajero,
 * origen/destino y acciones de aceptar o rechazar. Contenido de ejemplo;
 * llega desde la lista de Solicitudes.
 */
public class DetalleServicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_servicio);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_detalle_servicio_titulo);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.SOLICITADO);

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText("AG");
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText("Ana García");
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText("Hotel Miraflores Park · 400");

        ((TextView) findViewById(R.id.tv_origen)).setText("Hotel Miraflores Park");
        ((TextView) findViewById(R.id.tv_destino)).setText("Av. Malecón de la Reserva 1035, Miraflores");
        ((TextView) findViewById(R.id.tv_distancia_tiempo)).setText("14 min · 6.2 km");

        findViewById(R.id.btn_rechazar).setOnClickListener(v -> finish());
        findViewById(R.id.btn_aceptar_servicio).setOnClickListener(v ->
                startActivity(new Intent(this, ServicioAsignadoActivity.class)));
    }
}
