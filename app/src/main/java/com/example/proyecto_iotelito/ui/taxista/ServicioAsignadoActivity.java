package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

/**
 * Servicio ya asignado al conductor, en espera de que inicie el recorrido
 * hacia el hotel. Muestra los datos de la solicitud aceptada.
 */
public class ServicioAsignadoActivity extends AppCompatActivity {

    private static final String EXTRA_PASAJERO_NOMBRE = "extra_pasajero_nombre";
    private static final String EXTRA_PASAJERO_INICIALES = "extra_pasajero_iniciales";
    private static final String EXTRA_HOTEL = "extra_hotel";

    public static Intent createIntent(Context context, SolicitudServicio solicitud) {
        Intent intent = new Intent(context, ServicioAsignadoActivity.class);
        intent.putExtra(EXTRA_PASAJERO_NOMBRE, solicitud.pasajeroNombre);
        intent.putExtra(EXTRA_PASAJERO_INICIALES, solicitud.pasajeroIniciales);
        intent.putExtra(EXTRA_HOTEL, solicitud.hotel);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicio_asignado);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_servicio_asignado_titulo);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.ASIGNADO);

        String nombre = valorOrDefault(getIntent().getStringExtra(EXTRA_PASAJERO_NOMBRE), "Ana García");
        String iniciales = valorOrDefault(getIntent().getStringExtra(EXTRA_PASAJERO_INICIALES), "AG");
        String hotel = valorOrDefault(getIntent().getStringExtra(EXTRA_HOTEL), "Hotel Miraflores Park");

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText(iniciales);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText(nombre);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(hotel);
        rowPasajero.findViewById(R.id.iv_chat).setOnClickListener(v ->
                startActivity(ChatPasajeroTaxistaActivity.createIntent(this, nombre, iniciales)));

        ((TextView) findViewById(R.id.tv_ruta_resumen)).setText("31 min · 19 km");

        findViewById(R.id.btn_iniciar_recorrido).setOnClickListener(v ->
                startActivity(new Intent(this, DireccionHotelActivity.class)));
    }

    private String valorOrDefault(String valor, String porDefecto) {
        return TextUtils.isEmpty(valor) ? porDefecto : valor;
    }
}
