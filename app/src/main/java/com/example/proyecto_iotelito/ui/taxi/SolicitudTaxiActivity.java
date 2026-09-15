package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class SolicitudTaxiActivity extends AppCompatActivity {

    private int pasajeros = 2;
    private int maletas = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_taxi);

        setupHeader();
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

    private void setupSteppers() {
        TextView tvPasajeros = findViewById(R.id.tv_pasajeros);
        TextView tvMaletas   = findViewById(R.id.tv_maletas);

        findViewById(R.id.btn_menos_pasajeros).setOnClickListener(v -> {
            if (pasajeros > 1) { pasajeros--; tvPasajeros.setText(String.valueOf(pasajeros)); }
        });
        findViewById(R.id.btn_mas_pasajeros).setOnClickListener(v -> {
            if (pasajeros < 8) { pasajeros++; tvPasajeros.setText(String.valueOf(pasajeros)); }
        });
        findViewById(R.id.btn_menos_maletas).setOnClickListener(v -> {
            if (maletas > 0) { maletas--; tvMaletas.setText(String.valueOf(maletas)); }
        });
        findViewById(R.id.btn_mas_maletas).setOnClickListener(v -> {
            if (maletas < 10) { maletas++; tvMaletas.setText(String.valueOf(maletas)); }
        });
    }

    private void setupConfirmButton() {
        MaterialButton btnConfirmar = findViewById(R.id.btn_confirmar_solicitud);
        btnConfirmar.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaxistaAsignadoActivity.class);
            startActivity(intent);
        });
    }
}
