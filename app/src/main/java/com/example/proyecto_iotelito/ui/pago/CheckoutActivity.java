package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;

import java.util.Locale;

/**
 * Checkout de una reserva ya creada pero con pago pendiente (a diferencia
 * de {@link com.example.proyecto_iotelito.ui.booking.ConfirmacionReservaActivity},
 * que paga una reserva nueva). Mismo patrón de validación con
 * {@link EditText#setError} (Clase 3.2 - Elementos de UI).
 */
public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private Reserva reserva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, SampleData.RESERVAS.get(0).id);
        reserva = SampleData.findReservaById(reservaId);
        Hotel hotel = SampleData.findById(reserva.hotelId);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_checkout);

        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion, reserva.roomName);
        bindRow(R.id.row_fechas, R.string.label_fechas, reserva.rangoFechas);
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp, reserva.huespedes);
        bindRow(R.id.row_codigo, R.string.label_codigo, reserva.codigo);
        ((TextView) findViewById(R.id.tv_monto_total)).setText(formatMoney(reserva.precioTotal));

        findViewById(R.id.btn_confirmar_pago).setOnClickListener(v -> onConfirmarPago());
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private void onConfirmarPago() {
        EditText etNumero = findViewById(R.id.et_numero_tarjeta);
        EditText etVencimiento = findViewById(R.id.et_vencimiento);
        EditText etCvv = findViewById(R.id.et_cvv);

        boolean valido = true;
        if (TextUtils.isEmpty(etNumero.getText())) {
            etNumero.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(etVencimiento.getText())) {
            etVencimiento.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(etCvv.getText())) {
            etCvv.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (!valido) {
            return;
        }

        reserva.estado = Reserva.Estado.CONFIRMADA;

        Intent intent = new Intent(this, CobroConfirmadoActivity.class);
        intent.putExtra(CobroConfirmadoActivity.EXTRA_RESERVA_ID, reserva.id);
        startActivity(intent);
        finish();
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
