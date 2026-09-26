package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.taxi.BeneficioTaxi;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

public class BeneficioTaxiActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    private Reserva reserva;
    private Hotel hotel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beneficio_taxi);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, SampleData.RESERVAS.get(0).id);
        reserva = SampleData.findReservaById(reservaId);
        hotel = SampleData.findById(reserva.hotelId);

        TextView tvDescripcion = findViewById(R.id.tv_desc_taxi_gratuito);
        tvDescripcion.setText(getString(R.string.desc_taxi_gratuito_dinamico, hotel.name));

        setupBeneficiosList();
        MaterialButton btnSolicitar = findViewById(R.id.btn_solicitar_taxi);
        MaterialButton btnNoGracias = findViewById(R.id.btn_no_gracias);

        btnSolicitar.setOnClickListener(v -> {
            Intent intent = new Intent(this, SolicitudTaxiActivity.class);
            intent.putExtra(SolicitudTaxiActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
        });

        btnNoGracias.setOnClickListener(v -> finish());
    }

    private void setupBeneficiosList() {
        RecyclerView rvBeneficios = findViewById(R.id.rv_beneficios_taxi);
        rvBeneficios.setLayoutManager(new LinearLayoutManager(this));
        rvBeneficios.setNestedScrollingEnabled(false);
        rvBeneficios.setAdapter(new BeneficioTaxiAdapter(createStaticBenefits()));
    }

    private List<BeneficioTaxi> createStaticBenefits() {
        return Arrays.asList(
                new BeneficioTaxi(
                        getString(R.string.item_valido_aeropuerto),
                        R.drawable.ic_check
                ),
                new BeneficioTaxi(
                        getString(R.string.item_monitoreo_satelital),
                        R.drawable.ic_check
                )
        );
    }
}
