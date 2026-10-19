package com.example.proyecto_iotelito.ui.booking;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.notification.ClienteNotificationManager;

import java.util.Locale;

/**
 * Pantalla final del flujo Explorar -> Reserva. Desde aquí se puede
 * volver al shell principal en la pestaña Reservas o en Explorar,
 * limpiando todo el stack de actividades del flujo de búsqueda.
 */
public class ReservaConfirmadaActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_CODIGO = "extra_codigo";
    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private Reserva reserva;
    private final ActivityResultLauncher<String> permisoNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (concedido && reserva != null) {
                    ClienteNotificationManager.mostrarReservaConfirmada(this, reserva);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserva_confirmada);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        Hotel hotel = SampleData.findById(hotelId);
        BookingSelection bookingSelection = BookingSelection.from(getIntent());
        double total = getIntent().getDoubleExtra(
                EXTRA_TOTAL, hotel.pricePerNight * bookingSelection.noches());
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        reserva = ReservaRepository.getInstance(this).buscarPorId(reservaId);

        ((ImageView) findViewById(R.id.iv_resumen_hotel))
                .setImageResource(HotelMedia.galleryImage(hotel.id, 1));

        ((TextView) findViewById(R.id.tv_codigo)).setText(getString(R.string.codigo_reserva, codigo));

        bindRow(R.id.row_hotel, R.string.label_hotel, hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion,
                reserva != null ? reserva.roomName : hotel.roomName);
        bindRow(R.id.row_fechas, R.string.label_fechas, bookingSelection.textoFechas());
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp,
                bookingSelection.textoHuespedes(this));

        ((TextView) findViewById(R.id.tv_monto_total)).setText("S/ " + String.format(Locale.US, "%,.0f", total));

        findViewById(R.id.btn_ver_reserva).setOnClickListener(v -> irAlShell(R.id.nav_reservas));
        findViewById(R.id.btn_volver_inicio).setOnClickListener(v -> irAlShell(R.id.nav_explorar));

        if (savedInstanceState == null && reserva != null) {
            mostrarNotificacionReserva();
        }
    }

    private void mostrarNotificacionReserva() {
        boolean teniaPermiso = ClienteNotificationManager.tienePermiso(this);
        // También registra el aviso en la bandeja local si el permiso del sistema fue denegado.
        ClienteNotificationManager.mostrarReservaConfirmada(this, reserva);
        if (!teniaPermiso) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
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
