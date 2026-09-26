package com.example.proyecto_iotelito.ui.taxi;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.google.android.material.button.MaterialButton;

public class SolicitudTaxiActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private int pasajeros = 2;
    private int maletas = 3;
    private int maxPasajeros = 2;
    private int maxMaletas = 4;
    private int horaRecojo = 15;
    private int minutoRecojo = 30;
    private Reserva reserva;
    private Hotel hotel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_taxi);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, SampleData.RESERVAS.get(0).id);
        reserva = SampleData.findReservaById(reservaId);
        hotel = SampleData.findById(reserva.hotelId);
        maxPasajeros = Math.max(1, obtenerCantidadHuespedes(reserva.huespedes));
        maxMaletas = maxPasajeros * 2;
        pasajeros = Math.min(pasajeros, maxPasajeros);
        maletas = Math.min(maletas, maxMaletas);

        setupHeader();
        setupReservaHotel();
        setupHoraRecojo();
        setupSteppers();
        setupConfirmButton();
    }

    private void setupHeader() {
        View header = findViewById(R.id.sub_header);
        if (header != null) {
            ImageView ivBack = header.findViewById(R.id.iv_back);
            TextView tvTitle = header.findViewById(R.id.tv_title);
            if (tvTitle != null) tvTitle.setText(R.string.titulo_solicitar_taxi);
            if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        }
    }

    private void setupReservaHotel() {
        TextView tvDireccionHotel = findViewById(R.id.tv_direccion_hotel);
        tvDireccionHotel.setText(hotel.address);
    }

    private void setupHoraRecojo() {
        TextView tvHoraRecojo = findViewById(R.id.tv_hora_recojo);
        View containerHoraRecojo = findViewById(R.id.container_hora_recojo);
        tvHoraRecojo.setText(formatearHoraRecojo());
        containerHoraRecojo.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        horaRecojo = hourOfDay;
                        minutoRecojo = minute;
                        tvHoraRecojo.setText(formatearHoraRecojo());
                    },
                    horaRecojo,
                    minutoRecojo,
                    true
            );
            dialog.show();
        });
    }

    private String formatearHoraRecojo() {
        return String.format("%02d:%02d", horaRecojo, minutoRecojo);
    }

    private void setupSteppers() {
        TextView tvPasajeros = findViewById(R.id.tv_pasajeros);
        TextView tvMaletas   = findViewById(R.id.tv_maletas);
        tvPasajeros.setText(String.valueOf(pasajeros));
        tvMaletas.setText(String.valueOf(maletas));

        findViewById(R.id.btn_menos_pasajeros).setOnClickListener(v -> {
            if (pasajeros > 1) { pasajeros--; tvPasajeros.setText(String.valueOf(pasajeros)); }
        });
        findViewById(R.id.btn_mas_pasajeros).setOnClickListener(v -> {
            if (pasajeros < maxPasajeros) { pasajeros++; tvPasajeros.setText(String.valueOf(pasajeros)); }
        });
        findViewById(R.id.btn_menos_maletas).setOnClickListener(v -> {
            if (maletas > 0) { maletas--; tvMaletas.setText(String.valueOf(maletas)); }
        });
        findViewById(R.id.btn_mas_maletas).setOnClickListener(v -> {
            if (maletas < maxMaletas) { maletas++; tvMaletas.setText(String.valueOf(maletas)); }
        });
    }

    private void setupConfirmButton() {
        MaterialButton btnConfirmar = findViewById(R.id.btn_confirmar_solicitud);
        btnConfirmar.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaxistaAsignadoActivity.class);
            startActivity(intent);
        });
    }

    private int obtenerCantidadHuespedes(String textoHuespedes) {
        int total = 0;
        String[] partes = textoHuespedes.split(",");
        for (String parte : partes) {
            String limpia = parte.trim();
            if (limpia.isEmpty()) {
                continue;
            }
            String[] tokens = limpia.split("\\s+");
            try {
                total += Integer.parseInt(tokens[0]);
            } catch (NumberFormatException ignored) {
                // Si el texto de demo cambiara, mantenemos un pasajero como mínimo.
            }
        }
        return total == 0 ? 1 : total;
    }
}
