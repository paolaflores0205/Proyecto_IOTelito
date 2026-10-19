package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxistaSampleData;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

/**
 * Detalle de una solicitud de servicio antes de aceptarla: pasajero,
 * origen/destino y acciones de aceptar o rechazar. Recibe el id de la
 * solicitud elegida en la lista de Solicitudes y carga sus datos estáticos.
 */
public class DetalleServicioActivity extends AppCompatActivity {

    private static final String EXTRA_SOLICITUD_ID = "extra_solicitud_id";

    public static Intent createIntent(Context context, int solicitudId) {
        Intent intent = new Intent(context, DetalleServicioActivity.class);
        intent.putExtra(EXTRA_SOLICITUD_ID, solicitudId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_servicio);

        int solicitudId = getIntent().getIntExtra(EXTRA_SOLICITUD_ID, -1);
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.buscar(this, solicitudId),
                pedido -> pintarSolicitud(solicitudId, pedido));
    }

    private void pintarSolicitud(int solicitudId, org.json.JSONObject pedido) {
        SolicitudServicio solicitud = pedido == null ? TaxistaSampleData.solicitudPorId(solicitudId)
                : com.example.proyecto_iotelito.data.TaxiRepository.convertir(pedido);
        if (solicitud == null) {
            finish();
            return;
        }
        if (pedido != null && !com.example.proyecto_iotelito.data.TaxiRepository.Estado.SOLICITADO.name()
                .equals(pedido.optString("estado"))) {
            com.example.proyecto_iotelito.model.auth.AuthenticatedUser user =
                    com.example.proyecto_iotelito.data.SessionRepository.obtener(this);
            if (user != null && user.getEmail().equals(pedido.optString("taxista"))
                    && !com.example.proyecto_iotelito.data.TaxiRepository.Estado.FINALIZADO.name()
                    .equals(pedido.optString("estado"))) {
                Intent destino = com.example.proyecto_iotelito.data.TaxiRepository.Estado.ASIGNADO.name()
                        .equals(pedido.optString("estado")) ? ServicioAsignadoActivity.createIntent(this, solicitud)
                        : new Intent(this, DireccionHotelActivity.class)
                        .putExtra(com.example.proyecto_iotelito.data.TaxiRepository.EXTRA_ID, solicitud.id);
                startActivity(destino);
            }
            finish();
            return;
        }

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_detalle_servicio_titulo);
        EstadoServicioUi.pintarPill(subHeader.findViewById(R.id.tv_estado_pill), EstadoServicio.SOLICITADO);

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText(solicitud.pasajeroIniciales);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText(solicitud.pasajeroNombre);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(solicitud.hotel);
        rowPasajero.findViewById(R.id.iv_chat).setOnClickListener(v -> startActivity(
                ChatPasajeroTaxistaActivity.createIntent(this, solicitud.pasajeroNombre, solicitud.pasajeroIniciales)));

        ((TextView) findViewById(R.id.tv_origen)).setText(solicitud.hotel);
        ((TextView) findViewById(R.id.tv_destino)).setText(solicitud.destino);
        ((TextView) findViewById(R.id.tv_distancia_tiempo)).setText(solicitud.distanciaTiempo);

        View rowTarifa = findViewById(R.id.row_tarifa);
        ((TextView) rowTarifa.findViewById(R.id.tv_label)).setText(R.string.taxi_label_tarifa);
        ((TextView) rowTarifa.findViewById(R.id.tv_value)).setText(
                getString(R.string.taxi_tarifa_monto, solicitud.tarifa));

        findViewById(R.id.btn_rechazar).setOnClickListener(v -> finish());
        findViewById(R.id.btn_aceptar_servicio).setOnClickListener(v -> {
            // Al aceptar, el pedido deja de estar disponible para los demás taxistas.
            if (pedido != null) {
                TaxiRepository.ejecutar(this, this,
                        () -> TaxiRepository.avanzar(this, solicitud.id, TaxiRepository.Estado.ASIGNADO), aceptada -> {
                            if (aceptada) abrirServicio(solicitud);
                            else finish();
                        });
            } else {
                TaxistaSampleData.aceptarSolicitud(solicitud.id);
                abrirServicio(solicitud);
            }
        });
    }

    private void abrirServicio(SolicitudServicio solicitud) {
        if (isFinishing()) return;
        startActivity(ServicioAsignadoActivity.createIntent(this, solicitud));
        finish();
    }
}
