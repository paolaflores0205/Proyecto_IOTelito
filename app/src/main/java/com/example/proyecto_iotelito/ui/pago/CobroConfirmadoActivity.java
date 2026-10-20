package com.example.proyecto_iotelito.ui.pago;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.notification.ClienteNotificationManager;

import java.util.Locale;

public class CobroConfirmadoActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private Reserva reserva;
    private final ActivityResultLauncher<String> permisoNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (concedido && reserva != null) {
                    ClienteNotificationManager.mostrarPagoRealizado(this, reserva);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro_confirmado);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(this, reservaId, (value, hotel) -> {
        reserva = value;

        bindRow(R.id.row_hotel, R.string.label_hotel, hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion,
                getString(R.string.habitacion_con_numero, reserva.roomName, reserva.roomNumber));
        bindRow(R.id.row_fechas, R.string.label_fechas, reserva.rangoFechasTexto());
        bindRow(R.id.row_tarjeta, R.string.label_tarjeta_cargo, getString(R.string.tarjeta_simulada_valor));

        ((TextView) findViewById(R.id.tv_monto_total)).setText(formatMoney(reserva.precioTotal));
        ((TextView) findViewById(R.id.tv_cargos_adicionales)).setText(formatMoney(0));

        findViewById(R.id.btn_continuar).setOnClickListener(v -> irAlShell());
        findViewById(R.id.btn_ver_historial).setOnClickListener(v -> irAlShell());

        if (savedInstanceState == null) {
            mostrarNotificacionPago();
        }
        });
    }

    private void mostrarNotificacionPago() {
        boolean teniaPermiso = ClienteNotificationManager.tienePermiso(this);
        // La bandeja interna conserva el evento aunque Android no permita mostrar el aviso.
        ClienteNotificationManager.mostrarPagoRealizado(this, reserva);
        if (!teniaPermiso) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private void irAlShell() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, R.id.nav_reservas);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
