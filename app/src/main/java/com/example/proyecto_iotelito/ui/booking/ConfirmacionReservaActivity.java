package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;

import java.util.Locale;
import java.util.Random;

/**
 * Resumen de la reserva + formulario de pago (simulado). Valida que
 * los campos no estén vacíos usando {@link EditText#setError}
 * (Clase 3.2 - Elementos de UI) antes de confirmar.
 */
public class ConfirmacionReservaActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    public static final String EXTRA_FECHAS = "extra_fechas";
    public static final String EXTRA_TOTAL = "extra_total";

    private Hotel hotel;
    private String fechas;
    private double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmacion_reserva);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        hotel = SampleData.findById(hotelId);
        fechas = getIntent().getStringExtra(EXTRA_FECHAS);
        total = getIntent().getDoubleExtra(EXTRA_TOTAL, hotel.pricePerNight * 3);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_confirmacion);

        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);

        bindRow(R.id.row_habitacion, R.string.label_habitacion, hotel.roomName, false);
        bindRow(R.id.row_fechas, R.string.label_fechas, fechas, false);
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp, hotel.roomCapacity, false);
        bindRow(R.id.row_precio_noche, R.string.label_precio_noche,
                formatMoney(hotel.pricePerNight), true);

        ((TextView) findViewById(R.id.tv_monto_total)).setText(formatMoney(total));

        configurarFormatoTarjeta(findViewById(R.id.et_numero_tarjeta));
        configurarFormatoVencimiento(findViewById(R.id.et_vencimiento));

        findViewById(R.id.btn_confirmar_reserva).setOnClickListener(v -> onConfirmar());
    }

    /** Agrupa el número de tarjeta de 4 en 4 dígitos mientras el usuario escribe (ej. 1234 5678 9012 3456). */
    private void configurarFormatoTarjeta(EditText et) {
        // inputType="number" adjunta un DigitsKeyListener que solo deja pasar dígitos;
        // hay que ampliarlo para que también acepte el espacio que insertamos nosotros.
        et.setKeyListener(DigitsKeyListener.getInstance("0123456789 "));
        et.addTextChangedListener(new TextWatcher() {
            private boolean actualizando = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (actualizando) {
                    return;
                }
                actualizando = true;

                String soloDigitos = editable.toString().replaceAll("[^0-9]", "");
                if (soloDigitos.length() > 16) {
                    soloDigitos = soloDigitos.substring(0, 16);
                }

                StringBuilder formateado = new StringBuilder();
                for (int i = 0; i < soloDigitos.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formateado.append(' ');
                    }
                    formateado.append(soloDigitos.charAt(i));
                }

                editable.replace(0, editable.length(), formateado.toString());
                actualizando = false;
            }
        });
    }

    /** Inserta el "/" apenas se completan los 2 dígitos del mes (ej. 12/25). */
    private void configurarFormatoVencimiento(EditText et) {
        // mismo motivo que en configurarFormatoTarjeta: hay que permitir el "/" en el filtro.
        et.setKeyListener(DigitsKeyListener.getInstance("0123456789/"));
        et.addTextChangedListener(new TextWatcher() {
            private boolean actualizando = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (actualizando) {
                    return;
                }
                actualizando = true;

                String soloDigitos = editable.toString().replaceAll("[^0-9]", "");
                if (soloDigitos.length() > 4) {
                    soloDigitos = soloDigitos.substring(0, 4);
                }

                String formateado = soloDigitos.length() >= 2
                        ? soloDigitos.substring(0, 2) + "/" + soloDigitos.substring(2)
                        : soloDigitos;

                editable.replace(0, editable.length(), formateado);
                actualizando = false;
            }
        });
    }

    private void bindRow(int rowId, int labelRes, String value, boolean tealValue) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        TextView tvValue = row.findViewById(R.id.tv_value);
        tvValue.setText(value);
        if (tealValue) {
            tvValue.setTextColor(ContextCompat.getColor(this, R.color.io_teal));
        }
    }

    private void onConfirmar() {
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

        String codigo = String.format(Locale.getDefault(), "IOT-%04d-%04d",
                hotel.id, new Random().nextInt(9000) + 1000);

        Intent intent = new Intent(this, ReservaConfirmadaActivity.class);
        intent.putExtra(ReservaConfirmadaActivity.EXTRA_HOTEL_ID, hotel.id);
        intent.putExtra(ReservaConfirmadaActivity.EXTRA_FECHAS, fechas);
        intent.putExtra(ReservaConfirmadaActivity.EXTRA_TOTAL, total);
        intent.putExtra(ReservaConfirmadaActivity.EXTRA_CODIGO, codigo);
        startActivity(intent);
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
