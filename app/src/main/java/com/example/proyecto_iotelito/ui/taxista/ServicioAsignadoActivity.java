package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Servicio ya asignado al conductor, en espera de que inicie el recorrido
 * hacia el hotel. Contenido de ejemplo; llega tras aceptar una solicitud.
 */
public class ServicioAsignadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicio_asignado);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_servicio_asignado_titulo);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.ASIGNADO);

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText("AG");
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText("Ana García");
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText("Hotel Miraflores Park · 400");
        rowPasajero.findViewById(R.id.iv_chat).setOnClickListener(v ->
                startActivity(ChatPasajeroTaxistaActivity.createIntent(this, "Ana García", "AG")));

        ((TextView) findViewById(R.id.tv_ruta_resumen)).setText("31 min · 19 km");

        findViewById(R.id.btn_iniciar_recorrido).setOnClickListener(v ->
                startActivity(new Intent(this, DireccionHotelActivity.class)));
    }
}
