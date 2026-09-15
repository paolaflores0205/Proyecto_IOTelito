package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class TaxistaAsignadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxista_asignado);

        MaterialButton btnVerUbicacion = findViewById(R.id.btn_ver_ubicacion);
        TextView tvCancelar = findViewById(R.id.tv_cancelar_solicitud);

        btnVerUbicacion.setOnClickListener(v ->
                startActivity(new Intent(this, SeguimientoTaxiActivity.class)));

        tvCancelar.setOnClickListener(v -> finish());
    }
}
