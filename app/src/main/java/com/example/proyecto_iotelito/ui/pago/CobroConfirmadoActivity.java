package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;

import java.util.Locale;

/**
 * Confirmación del pago de una reserva pendiente. Desde aquí se vuelve al
 * shell principal (pestaña Reservas o Explorar), limpiando el stack del
 * flujo de pago igual que {@link com.example.proyecto_iotelito.ui.booking.ReservaConfirmadaActivity}.
 */
public class CobroConfirmadoActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro_confirmado);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, SampleData.RESERVAS.get(0).id);
        Reserva reserva = SampleData.findReservaById(reservaId);
        Hotel hotel = SampleData.findById(reserva.hotelId);

        ((TextView) findViewById(R.id.tv_codigo)).setText(getString(R.string.codigo_reserva, reserva.codigo));

        bindRow(R.id.row_hotel, R.string.label_hotel, hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion, reserva.roomName);
        bindRow(R.id.row_fechas, R.string.label_fechas, reserva.rangoFechas);
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp, reserva.huespedes);

        ((TextView) findViewById(R.id.tv_monto_total)).setText(
                "S/ " + String.format(Locale.US, "%,.0f", reserva.precioTotal));

        findViewById(R.id.btn_ver_reservas).setOnClickListener(v -> irAlShell(R.id.nav_reservas));
        findViewById(R.id.btn_volver_inicio).setOnClickListener(v -> irAlShell(R.id.nav_explorar));
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private void irAlShell(int tabMenuItemId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tabMenuItemId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
