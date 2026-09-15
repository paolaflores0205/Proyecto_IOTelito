package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
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
    private boolean elegibleTaxi;
    private int calificacion = 0;

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

        reserva.estado = Reserva.Estado.COMPLETADA;

        if (elegibleTaxi) {
            mostrarOfertaTaxi();
        } else {
            irACobroConfirmado();
        }
    }

    private void mostrarOfertaTaxi() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_oferta_taxi, null);
        sheet.setContentView(sheetView);
        sheet.setCanceledOnTouchOutside(false);

        sheetView.findViewById(R.id.btn_aceptar_taxi).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, BeneficioTaxiActivity.class));
            finish();
        });

        sheetView.findViewById(R.id.btn_rechazar_taxi).setOnClickListener(v -> {
            sheet.dismiss();
            irACobroConfirmado();
        });

        sheet.show();
    }

    private void irACobroConfirmado() {
        Intent intent = new Intent(this, CobroConfirmadoActivity.class);
        intent.putExtra(CobroConfirmadoActivity.EXTRA_RESERVA_ID, reserva.id);
        startActivity(intent);
        finish();
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
