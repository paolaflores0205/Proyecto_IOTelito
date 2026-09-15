package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class BeneficioTaxiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beneficio_taxi);

        MaterialButton btnSolicitar = findViewById(R.id.btn_solicitar_taxi);
        MaterialButton btnNoGracias = findViewById(R.id.btn_no_gracias);

        btnSolicitar.setOnClickListener(v -> {
            Intent intent = new Intent(this, SolicitudTaxiActivity.class);
            startActivity(intent);
        });

        btnNoGracias.setOnClickListener(v -> finish());
    }
}
