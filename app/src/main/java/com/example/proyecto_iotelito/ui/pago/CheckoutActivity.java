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
import com.example.proyecto_iotelito.model.Reserva;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private Reserva reserva;
    private ReservaRepository reservaRepository;
    private boolean elegibleTaxi;
    private boolean checkoutRemoto;
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
        checkoutRemoto = FirebaseAuth.getInstance().getCurrentUser() != null;
        if (!reserva.puedeSolicitarCheckout()) {
            abrirSolicitudODetalle();
            return;
        }

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_checkout);

        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        ((TextView) findViewById(R.id.tv_habitacion)).setText(
                getString(R.string.habitacion_con_numero, reserva.roomName, reserva.roomNumber));

        TextView tarjetaGuardada=findViewById(R.id.tv_tarjeta_checkout);
        tarjetaGuardada.setText(reserva.tarjeta==null?getString(R.string.checkout_tarjeta_no_guardada)
                :getString(R.string.checkout_tarjeta_reserva,reserva.tarjeta.etiqueta()));
        findViewById(R.id.btn_confirmar_checkout).setEnabled(reserva.tarjeta!=null);
        configurarEstrellas();
        if (checkoutRemoto) {
            ((TextView) findViewById(R.id.tv_comentarios_checkout)).setText(R.string.comentario_checkout_requerido);
            ((MaterialButton) findViewById(R.id.btn_confirmar_checkout)).setText(R.string.solicitar_checkout);
        }

        bindRow(R.id.row_monto_preautorizado, R.string.label_monto_preautorizado, formatMoney(reserva.precioTotal));
        bindRow(R.id.row_consumos_adicionales, R.string.label_consumos_adicionales, formatMoney(0));
        ((TextView) findViewById(R.id.tv_total_liquidar)).setText(formatMoney(reserva.precioTotal));

        elegibleTaxi = com.example.proyecto_iotelito.data.TaxiBenefitPolicy.cumpleMonto(reserva, hotel);
        ((TextView) findViewById(R.id.tv_desc_taxi_cortesia)).setText(
                getString(R.string.desc_taxi_cortesia, com.example.proyecto_iotelito.data.TaxiBenefitPolicy.texto(hotel)));
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
        if (reserva.tarjeta == null) {
            Toast.makeText(this, R.string.checkout_tarjeta_no_guardada, Toast.LENGTH_LONG).show(); return;
        }
        enviarCheckout(comentario);
    }

    private void enviarCheckout(String comentario) {
        findViewById(R.id.btn_confirmar_checkout).setEnabled(false);
        com.example.proyecto_iotelito.data.ClientLocalTasks.run(this, this, () -> {
            reservaRepository.solicitarCheckout(reserva.id, calificacion, comentario);
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
