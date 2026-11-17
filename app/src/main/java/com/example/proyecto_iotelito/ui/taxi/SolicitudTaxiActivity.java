package com.example.proyecto_iotelito.ui.taxi;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxiBenefitPolicy;
import com.example.proyecto_iotelito.data.TaxiRepository;
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
    private String aeropuertoCodigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_taxi);
        TaxiUi.status(this, null);
        if (savedInstanceState != null) {
            aeropuertoCodigo = savedInstanceState.getString("airport_code");
            pasajeros = savedInstanceState.getInt("passengers", pasajeros);
            maletas = savedInstanceState.getInt("bags", maletas);
            horaRecojo = savedInstanceState.getInt("pickup_hour", horaRecojo);
            minutoRecojo = savedInstanceState.getInt("pickup_minute", minutoRecojo);
        }

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(
                this,
                reservaId,
                (value, loadedHotel) -> {
                    reserva = value;
                    hotel = loadedHotel;
                    if (!TaxiBenefitPolicy.puedeSolicitar(reserva)) {
                        Toast.makeText(this, R.string.taxi_requisito_estadia, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    TaxiRepository.ejecutar(
                            this,
                            this,
                            () -> TaxiRepository.porReserva(this, reserva.id),
                            existente -> {
                                if (existente >= 0) abrirSolicitud(existente);
                            });
                    maxPasajeros = Math.max(1, obtenerCantidadHuespedes(reserva.huespedes));
                    maxMaletas = maxPasajeros * 2;
                    pasajeros = Math.min(pasajeros, maxPasajeros);
                    maletas = Math.min(maletas, maxMaletas);

                    setupHeader();
                    setupReservaHotel();
                    setupHoraRecojo();
                    setupSteppers();
                    setupConfirmButton();
                    cargarAeropuertos();
                });
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
        containerHoraRecojo.setOnClickListener(
                v -> {
                    TimePickerDialog dialog =
                            new TimePickerDialog(
                                    this,
                                    (view, hourOfDay, minute) -> {
                                        horaRecojo = hourOfDay;
                                        minutoRecojo = minute;
                                        tvHoraRecojo.setText(formatearHoraRecojo());
                                    },
                                    horaRecojo,
                                    minutoRecojo,
                                    true);
                    dialog.show();
                });
    }

    private String formatearHoraRecojo() {
        return String.format(java.util.Locale.ROOT, "%02d:%02d", horaRecojo, minutoRecojo);
    }

    private void setupSteppers() {
        TextView tvPasajeros = findViewById(R.id.tv_pasajeros);
        TextView tvMaletas = findViewById(R.id.tv_maletas);
        tvPasajeros.setText(String.valueOf(pasajeros));
        tvMaletas.setText(String.valueOf(maletas));

        findViewById(R.id.btn_menos_pasajeros)
                .setOnClickListener(
                        v -> {
                            if (pasajeros > 1) {
                                pasajeros--;
                                tvPasajeros.setText(String.valueOf(pasajeros));
                            }
                        });
        findViewById(R.id.btn_mas_pasajeros)
                .setOnClickListener(
                        v -> {
                            if (pasajeros < maxPasajeros) {
                                pasajeros++;
                                tvPasajeros.setText(String.valueOf(pasajeros));
                            }
                        });
        findViewById(R.id.btn_menos_maletas)
                .setOnClickListener(
                        v -> {
                            if (maletas > 0) {
                                maletas--;
                                tvMaletas.setText(String.valueOf(maletas));
                            }
                        });
        findViewById(R.id.btn_mas_maletas)
                .setOnClickListener(
                        v -> {
                            if (maletas < maxMaletas) {
                                maletas++;
                                tvMaletas.setText(String.valueOf(maletas));
                            }
                        });
    }

    private void setupConfirmButton() {
        MaterialButton btnConfirmar = findViewById(R.id.btn_confirmar_solicitud);
        btnConfirmar.setOnClickListener(
                v -> {
                    if (aeropuertoCodigo == null) return;
                    btnConfirmar.setEnabled(false);
                    String hora = formatearHoraRecojo();
                    int cantidadPasajeros = pasajeros;
                    int cantidadMaletas = maletas;
                    com.example.proyecto_iotelito.data.taxiapi.TaxiTasks.run(
                            this,
                            () ->
                                    TaxiRepository.crear(
                                            this,
                                            reserva,
                                            aeropuertoCodigo,
                                            hora,
                                            cantidadPasajeros,
                                            cantidadMaletas),
                            solicitudId -> {
                                btnConfirmar.setEnabled(true);
                                if (solicitudId < 0) {
                                    android.widget.Toast.makeText(
                                                    this,
                                                    R.string.taxi_guardar_error,
                                                    android.widget.Toast.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                                abrirSolicitud(solicitudId);
                            },
                            error -> {
                                btnConfirmar.setEnabled(true);
                                TaxiUi.status(this, error);
                            });
                });
    }

    private void cargarAeropuertos() {
        MaterialButton button = findViewById(R.id.btn_confirmar_solicitud);
        button.setEnabled(false);
        TextView select = findViewById(R.id.tv_aeropuerto_destino);
        TextView help = findViewById(R.id.tv_ayuda_aeropuerto);
        View selector = findViewById(R.id.container_aeropuerto);
        View chevron = findViewById(R.id.iv_elegir_aeropuerto);
        selector.setOnClickListener(null);
        chevron.setVisibility(View.GONE);
        com.example.proyecto_iotelito.data.taxiapi.TaxiTasks.run(
                this,
                () ->
                        com.example.proyecto_iotelito.data.taxiapi.TaxiApis.get(this)
                                .listarAeropuertos(),
                response -> {
                    org.json.JSONArray list = response.optJSONArray("aeropuertos");
                    java.util.List<String> codes = new java.util.ArrayList<>(),
                            names = new java.util.ArrayList<>();
                    String city = hotel.city.trim().toLowerCase(java.util.Locale.ROOT);
                    for (int i = 0; i < list.length(); i++) {
                        org.json.JSONObject a = list.optJSONObject(i);
                        org.json.JSONArray cities = a.optJSONArray("ciudadesHotel");
                        for (int n = 0; n < cities.length(); n++)
                            if (city.equals(cities.optString(n))) {
                                codes.add(a.optString("codigo"));
                                names.add(a.optString("nombre"));
                                break;
                            }
                    }
                    if (codes.isEmpty()) {
                        select.setText("Sin aeropuerto para esta ciudad");
                        TaxiUi.status(
                                this, "No hay aeropuerto disponible para la ciudad del hotel.");
                        return;
                    }
                    int current = Math.max(0, codes.indexOf(aeropuertoCodigo));
                    aeropuertoCodigo = codes.get(current);
                    select.setText(names.get(current));
                    help.setText("Destino según la ciudad del hotel: " + hotel.city);
                    button.setEnabled(true);
                    selector.setClickable(codes.size() > 1);
                    selector.setFocusable(codes.size() > 1);
                    chevron.setVisibility(codes.size() > 1 ? View.VISIBLE : View.GONE);
                    if (codes.size() > 1) selector.setOnClickListener(
                            v ->
                                    new com.google.android.material.dialog
                                                    .MaterialAlertDialogBuilder(this)
                                            .setTitle(R.string.label_aeropuerto_destino)
                                            .setItems(
                                                    names.toArray(new String[0]),
                                                    (d, index) -> {
                                                        aeropuertoCodigo = codes.get(index);
                                                        select.setText(names.get(index));
                                                    })
                                            .show());
                    TaxiUi.status(this, null);
                },
                error -> {
                    TaxiUi.status(this, error);
                    select.setText("Reintentar cargar aeropuertos");
                    selector.setOnClickListener(v -> cargarAeropuertos());
                });
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("airport_code", aeropuertoCodigo);
        state.putInt("passengers", pasajeros);
        state.putInt("bags", maletas);
        state.putInt("pickup_hour", horaRecojo);
        state.putInt("pickup_minute", minutoRecojo);
    }

    private void abrirSolicitud(int id) {
        if (isFinishing()) return;
        startActivity(
                new Intent(this, TaxistaAsignadoActivity.class)
                        .putExtra(TaxiRepository.EXTRA_ID, id));
        finish();
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
