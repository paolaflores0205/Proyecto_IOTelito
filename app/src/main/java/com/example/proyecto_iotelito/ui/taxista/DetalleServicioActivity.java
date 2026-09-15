package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Detalle de una solicitud de servicio antes de aceptarla: pasajero,
 * origen/destino y acciones de aceptar o rechazar. Contenido de ejemplo;
 * llega desde la lista de Solicitudes, una tarjeta distinta por solicitud.
 * Aceptar siempre continúa con el mismo flujo de ejemplo (Ana García).
 */
public class DetalleServicioActivity extends AppCompatActivity {

    private static final String EXTRA_ORIGEN = "extra_origen";
    private static final String EXTRA_DESTINO = "extra_destino";
    private static final String EXTRA_PASAJERO_NOMBRE = "extra_pasajero_nombre";
    private static final String EXTRA_PASAJERO_INICIALES = "extra_pasajero_iniciales";
    private static final String EXTRA_DISTANCIA_TIEMPO = "extra_distancia_tiempo";

    public static Intent createIntent(Context context, String origen, String destino,
                                       String pasajeroNombre, String iniciales, String distanciaTiempo) {
        Intent intent = new Intent(context, DetalleServicioActivity.class);
        intent.putExtra(EXTRA_ORIGEN, origen);
        intent.putExtra(EXTRA_DESTINO, destino);
        intent.putExtra(EXTRA_PASAJERO_NOMBRE, pasajeroNombre);
        intent.putExtra(EXTRA_PASAJERO_INICIALES, iniciales);
        intent.putExtra(EXTRA_DISTANCIA_TIEMPO, distanciaTiempo);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_servicio);

        String origen = valorOrDefault(getIntent().getStringExtra(EXTRA_ORIGEN), "Hotel Miraflores Park");
        String destino = valorOrDefault(getIntent().getStringExtra(EXTRA_DESTINO),
                "Av. Malecón de la Reserva 1035, Miraflores");
        String pasajeroNombre = valorOrDefault(getIntent().getStringExtra(EXTRA_PASAJERO_NOMBRE), "Ana García");
        String iniciales = valorOrDefault(getIntent().getStringExtra(EXTRA_PASAJERO_INICIALES), "AG");
        String distanciaTiempo = valorOrDefault(getIntent().getStringExtra(EXTRA_DISTANCIA_TIEMPO), "14 min · 6.2 km");

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_detalle_servicio_titulo);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.SOLICITADO);

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText(iniciales);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText(pasajeroNombre);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(origen);
        rowPasajero.findViewById(R.id.iv_chat).setOnClickListener(v ->
                startActivity(ChatPasajeroTaxistaActivity.createIntent(this, pasajeroNombre, iniciales)));

        ((TextView) findViewById(R.id.tv_origen)).setText(origen);
        ((TextView) findViewById(R.id.tv_destino)).setText(destino);
        ((TextView) findViewById(R.id.tv_distancia_tiempo)).setText(distanciaTiempo);

        findViewById(R.id.btn_rechazar).setOnClickListener(v -> finish());
        findViewById(R.id.btn_aceptar_servicio).setOnClickListener(v ->
                startActivity(new Intent(this, ServicioAsignadoActivity.class)));
    }

    private String valorOrDefault(String valor, String porDefecto) {
        return TextUtils.isEmpty(valor) ? porDefecto : valor;
    }
}
