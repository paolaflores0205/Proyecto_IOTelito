package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.ui.taxi.BeneficioTaxiActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";
    private static final double MONTO_MINIMO_TAXI_GRATIS = 1000.0;

    private Reserva reserva;
    private ReservaRepository reservaRepository;
    private boolean elegibleTaxi;
    private boolean checkoutRemoto;
    private int calificacion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        reservaRepository = ReservaRepository.getInstance(this);
        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(this, reservaId, (value, hotel) -> {
        reserva = value;
        checkoutRemoto = FirebaseAuth.getInstance().getCurrentUser() != null;
        if (!puedeSolicitarSalida(reserva)) {
            abrirSolicitudODetalle();
            return;
        }

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_checkout);

        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        ((TextView) findViewById(R.id.tv_habitacion)).setText(
                getString(R.string.habitacion_con_numero, reserva.roomName, reserva.roomNumber));

        configurarEstrellas();
        if (checkoutRemoto) {
            ((TextView) findViewById(R.id.tv_comentarios_checkout)).setText(R.string.comentario_checkout_requerido);
            ((MaterialButton) findViewById(R.id.btn_confirmar_checkout)).setText(R.string.solicitar_checkout);
        }

        bindRow(R.id.row_monto_preautorizado, R.string.label_monto_preautorizado, formatMoney(reserva.precioTotal));
        bindRow(R.id.row_consumos_adicionales, R.string.label_consumos_adicionales, formatMoney(0));
        ((TextView) findViewById(R.id.tv_total_liquidar)).setText(formatMoney(reserva.precioTotal));

        elegibleTaxi = reserva.precioTotal >= MONTO_MINIMO_TAXI_GRATIS;
        MaterialCheckBox checkTaxi = findViewById(R.id.check_taxi);
        checkTaxi.setChecked(elegibleTaxi);
        checkTaxi.setEnabled(false);
        View filaTaxi = findViewById(R.id.row_taxi_cortesia);
        filaTaxi.setEnabled(false);
        filaTaxi.setAlpha(elegibleTaxi ? 1f : 0.5f);

        findViewById(R.id.btn_confirmar_checkout).setOnClickListener(v -> onConfirmarCheckout());
        findViewById(R.id.btn_cancelar_checkout).setOnClickListener(v -> finish());
        findViewById(R.id.btn_confirmar_checkout).setEnabled(false);
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.porReserva(this, reserva.id), solicitudId -> {
            if (solicitudId >= 0) {
                abrirSolicitud(solicitudId);
            } else {
                findViewById(R.id.btn_confirmar_checkout).setEnabled(true);
            }
        });
        });
    }

    private void configurarEstrellas() {
        ImageView[] estrellas = {
                findViewById(R.id.iv_star_1),
                findViewById(R.id.iv_star_2),
                findViewById(R.id.iv_star_3),
                findViewById(R.id.iv_star_4),
                findViewById(R.id.iv_star_5),
        };
        for (int i = 0; i < estrellas.length; i++) {
            int seleccion = i + 1;
            estrellas[i].setOnClickListener(v -> {
                calificacion = seleccion;
                for (int j = 0; j < estrellas.length; j++) {
                    estrellas[j].setImageResource(j < calificacion ? R.drawable.ic_star : R.drawable.ic_star_border);
                }
            });
        }
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private void onConfirmarCheckout() {
        if (calificacion == 0) {
            Toast.makeText(this, R.string.toast_calificacion_requerida, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText comentarios = findViewById(R.id.et_comentarios);
        String comentario = comentarios.getText().toString().trim();
        if (checkoutRemoto && (comentario.isBlank() || comentario.length() > 4000)) {
            comentarios.setError(getString(R.string.error_comentario_checkout));
            return;
        }
        int estrellas = calificacion;
        findViewById(R.id.btn_confirmar_checkout).setEnabled(false);
        com.example.proyecto_iotelito.data.ClientLocalTasks.run(this, this, () -> {
            Reserva actual = reservaRepository.buscarPorId(reserva.id);
            if (actual == null || !puedeSolicitarSalida(actual)
                    || TaxiRepository.porReserva(this, reserva.id) >= 0) return false;
            if (checkoutRemoto) reservaRepository.solicitarCheckout(reserva.id, estrellas, comentario);
            else reservaRepository.actualizarEstado(reserva.id, Reserva.Estado.COMPLETADA);
            return true;
        }, saved -> {
        if (!saved) {
            abrirSolicitudODetalle();
            return;
        }
        reserva.estado = checkoutRemoto ? Reserva.Estado.EN_CHECKOUT : Reserva.Estado.COMPLETADA;
        if (checkoutRemoto) Toast.makeText(this, R.string.checkout_solicitado, Toast.LENGTH_LONG).show();
        if (elegibleTaxi) {
            mostrarOfertaTaxi();
        } else {
            irACobroConfirmado();
        }
        }, () -> findViewById(R.id.btn_confirmar_checkout).setEnabled(true));
    }

    private boolean puedeSolicitarSalida(Reserva reserva) {
        return checkoutRemoto ? reserva.puedeSolicitarCheckout() : reserva.puedeHacerCheckout();
    }

    private void abrirSolicitudODetalle() {
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.porReserva(this, reserva.id), solicitudId -> {
            if (solicitudId >= 0) {
                abrirSolicitud(solicitudId);
            } else {
                startActivity(new Intent(this, DetalleReservaActivity.class)
                        .putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reserva.id));
                finish();
            }
        });
    }

    private void abrirSolicitud(int solicitudId) {
        if (isFinishing()) return;
        startActivity(new Intent(this, TaxistaAsignadoActivity.class).putExtra(TaxiRepository.EXTRA_ID, solicitudId));
        finish();
    }

    private void mostrarOfertaTaxi() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_oferta_taxi, null);
        sheet.setContentView(sheetView);
        sheet.setCanceledOnTouchOutside(false);

        sheetView.findViewById(R.id.btn_aceptar_taxi).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(this, BeneficioTaxiActivity.class);
            intent.putExtra(BeneficioTaxiActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
            finish();
        });

        sheetView.findViewById(R.id.btn_rechazar_taxi).setOnClickListener(v -> {
            sheet.dismiss();
            irACobroConfirmado();
        });

        sheet.show();
    }

    private void irACobroConfirmado() {
        if (checkoutRemoto) {
            abrirSolicitudODetalle();
            return;
        }
        Intent intent = new Intent(this, CobroConfirmadoActivity.class);
        intent.putExtra(CobroConfirmadoActivity.EXTRA_RESERVA_ID, reserva.id);
        startActivity(intent);
        finish();
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
