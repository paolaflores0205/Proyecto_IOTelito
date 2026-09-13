package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;

import java.util.Locale;

/**
 * Pantalla final del flujo Explorar -> Reserva. Desde aquí se puede
 * volver al shell principal en la pestaña Reservas o en Explorar,
 * limpiando todo el stack de actividades del flujo de búsqueda.
 */
public class ReservaConfirmadaActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    public static final String EXTRA_FECHAS = "extra_fechas";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_CODIGO = "extra_codigo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserva_confirmada);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        Hotel hotel = SampleData.findById(hotelId);
        String fechas = getIntent().getStringExtra(EXTRA_FECHAS);
        double total = getIntent().getDoubleExtra(EXTRA_TOTAL, hotel.pricePerNight * 3);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);

        ((TextView) findViewById(R.id.tv_codigo)).setText(getString(R.string.codigo_reserva, codigo));

        bindRow(R.id.row_hotel, R.string.label_hotel, hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion, hotel.roomName);
        bindRow(R.id.row_fechas, R.string.label_fechas, fechas);
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp, hotel.roomCapacity);

        ((TextView) findViewById(R.id.tv_monto_total)).setText("S/ " + String.format(Locale.US, "%,.0f", total));

        findViewById(R.id.btn_ver_reserva).setOnClickListener(v -> irAlShell(R.id.nav_reservas));
        findViewById(R.id.btn_volver_inicio).setOnClickListener(v -> irAlShell(R.id.nav_explorar));
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    /** Limpia todo el stack del flujo de reserva y vuelve al shell en la pestaña indicada. */
    private void irAlShell(int tabMenuItemId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tabMenuItemId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
