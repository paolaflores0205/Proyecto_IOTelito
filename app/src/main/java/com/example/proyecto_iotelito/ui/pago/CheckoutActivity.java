package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";
    private static final double MONTO_MINIMO_TAXI_GRATIS = 1000.0;

    private Reserva reserva;
    private ReservaRepository reservaRepository;
    private boolean elegibleTaxi;
    private int calificacion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        if (savedInstanceState != null) calificacion = savedInstanceState.getInt("rating", 0);

        reservaRepository = ReservaRepository.getInstance(this);
        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(this, reservaId, (value, hotel) -> {
        reserva = value;
        if (!reserva.puedeHacerCheckout()) {
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
            estrellas[i].setImageResource(i < calificacion ? R.drawable.ic_star : R.drawable.ic_star_border);
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

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("rating", calificacion);
    }

    private void onConfirmarCheckout() {
        if (!com.example.proyecto_iotelito.data.ConexionCliente.disponible(this)) {
            Toast.makeText(this, R.string.cliente_sin_conexion, Toast.LENGTH_LONG).show(); return;
        }
        String comentario = ((android.widget.EditText)findViewById(R.id.et_comentarios)).getText().toString().trim();
        try { com.example.proyecto_iotelito.data.CheckoutPolicy.validarOpinion(calificacion, comentario); }
        catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); return;
        }
        if (reserva.tarjeta == null) { solicitarTarjeta(comentario); return; }
        enviarCheckout(comentario, reserva.tarjeta);
    }

    private void solicitarTarjeta(String comentario) {
        View form = getLayoutInflater().inflate(R.layout.dialog_tarjeta_prueba, null);
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Registrar tarjeta de prueba")
                .setView(form).setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton("Solicitar checkout", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                com.example.proyecto_iotelito.data.TarjetaSimulada tarjeta = com.example.proyecto_iotelito.data.TarjetaSimulada.validar(
                        ((android.widget.EditText)form.findViewById(R.id.et_numero_tarjeta)).getText().toString(),
                        ((android.widget.EditText)form.findViewById(R.id.et_vencimiento)).getText().toString(),
                        ((android.widget.EditText)form.findViewById(R.id.et_cvv)).getText().toString(),
                        java.time.YearMonth.now(java.time.ZoneId.of("America/Lima")));
                dialog.dismiss(); enviarCheckout(comentario, tarjeta);
            } catch (IllegalArgumentException error) {
                TextView label = form.findViewById(R.id.tv_error_tarjeta);
                label.setText(error.getMessage()); label.setVisibility(View.VISIBLE);
            }
        }));
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void enviarCheckout(String comentario, com.example.proyecto_iotelito.data.TarjetaSimulada tarjeta) {
        findViewById(R.id.btn_confirmar_checkout).setEnabled(false);
        com.example.proyecto_iotelito.data.ClientLocalTasks.run(this, this, () -> {
            reservaRepository.solicitarCheckout(reserva.id, calificacion, comentario, tarjeta);
            return true;
        }, saved -> abrirComprobante(), () -> findViewById(R.id.btn_confirmar_checkout).setEnabled(true));
    }

    private void abrirSolicitudODetalle() { abrirComprobante(); }

    private void abrirComprobante() {
        startActivity(new Intent(this, CobroConfirmadoActivity.class)
                .putExtra(CobroConfirmadoActivity.EXTRA_RESERVA_ID, reserva.id));
        finish();
    }

    private String formatMoney(double amount) { return "S/ " + String.format(Locale.US, "%,.2f", amount); }
}
