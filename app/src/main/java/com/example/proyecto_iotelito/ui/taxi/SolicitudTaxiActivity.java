package com.example.proyecto_iotelito.ui.taxi;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.SampleData;
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
    private com.example.proyecto_iotelito.data.AeropuertosCliente.Aeropuerto aeropuerto;
    private String codigoRestaurado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_taxi);
        if(savedInstanceState!=null){
            codigoRestaurado=savedInstanceState.getString("airport_code");
            pasajeros=savedInstanceState.getInt("passengers",pasajeros);maletas=savedInstanceState.getInt("bags",maletas);
            horaRecojo=savedInstanceState.getInt("pickup_hour",horaRecojo);minutoRecojo=savedInstanceState.getInt("pickup_minute",minutoRecojo);
        }

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(this, reservaId, (value, loadedHotel) -> {
        reserva = value;
        hotel = loadedHotel;
        if(!com.example.proyecto_iotelito.data.TaxiBenefitPolicy.puedeSolicitar(reserva)){
            android.widget.Toast.makeText(this,R.string.taxi_requisito_estadia,android.widget.Toast.LENGTH_LONG).show();finish();return;
        }
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.porReserva(this, reserva.id), existente -> {
            if (existente >= 0) abrirSolicitud(existente);
        });
        maxPasajeros = Math.max(1, obtenerCantidadHuespedes(reserva.huespedes));
        maxMaletas = maxPasajeros * 2;
        pasajeros = Math.min(pasajeros, maxPasajeros);
        maletas = Math.min(maletas, maxMaletas);

        setupHeader();
        setupReservaHotel();
        setupAeropuerto();
        setupHoraRecojo();
        setupSteppers();
        setupConfirmButton();
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

    private void setupAeropuerto(){
        java.util.List<com.example.proyecto_iotelito.data.AeropuertosCliente.Aeropuerto> options=
                com.example.proyecto_iotelito.data.AeropuertosCliente.paraCiudad(hotel.city);
        TextView label=findViewById(R.id.tv_aeropuerto_destino),help=findViewById(R.id.tv_ayuda_aeropuerto);
        View selector=findViewById(R.id.container_aeropuerto);
        if(options.isEmpty()){
            label.setText("Sin aeropuerto configurado");
            help.setText("Todavía no hay un aeropuerto configurado para "+hotel.city+". Consulta con el hotel antes de solicitar el traslado.");
            findViewById(R.id.iv_elegir_aeropuerto).setVisibility(View.GONE);return;
        }
        aeropuerto=options.stream().filter(a->a.codigo.equals(codigoRestaurado)).findFirst().orElse(options.get(0));
        label.setText(aeropuerto.etiqueta());
        help.setText("Destino según la ubicación del hotel: "+hotel.city+". Verifica que coincida con tu vuelo.");
        findViewById(R.id.iv_elegir_aeropuerto).setVisibility(options.size()>1?View.VISIBLE:View.GONE);
        selector.setClickable(options.size()>1);selector.setFocusable(options.size()>1);
        if(options.size()>1)selector.setOnClickListener(v->{
            String[] labels=options.stream().map(a->a.etiqueta()).toArray(String[]::new);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Aeropuerto de destino")
                .setItems(labels,(dialog,which)->{aeropuerto=options.get(which);label.setText(aeropuerto.etiqueta());}).show();
        });
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
        return String.format(java.util.Locale.ROOT, "%02d:%02d", horaRecojo, minutoRecojo);
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
        btnConfirmar.setEnabled(aeropuerto!=null);
        btnConfirmar.setOnClickListener(v -> {
            String hora = formatearHoraRecojo();
            int cantidadPasajeros = pasajeros;
            int cantidadMaletas = maletas;
            TaxiRepository.ejecutar(this, this, () -> TaxiRepository.crear(
                    this, reserva, hotel, aeropuerto, hora, cantidadPasajeros, cantidadMaletas), solicitudId -> {
                if (solicitudId < 0) {
                    android.widget.Toast.makeText(this, R.string.taxi_guardar_error, android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                abrirSolicitud(solicitudId);
            });
        });
    }

    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putString("airport_code",aeropuerto==null?codigoRestaurado:aeropuerto.codigo);
        state.putInt("passengers",pasajeros);state.putInt("bags",maletas);
        state.putInt("pickup_hour",horaRecojo);state.putInt("pickup_minute",minutoRecojo);
    }

    private void abrirSolicitud(int id) {
        if (isFinishing()) return;
        startActivity(new Intent(this, TaxistaAsignadoActivity.class).putExtra(TaxiRepository.EXTRA_ID, id));
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
