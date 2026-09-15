package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Validación del código QR de la reserva para cerrar el servicio.
 * Sin lectura real de cámara todavía: el botón simula la validación.
 */
public class FinalizarServicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalizar_servicio);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_validar_codigo).setOnClickListener(v ->
                startActivity(new Intent(this, ServicioFinalizadoActivity.class)));
    }
}
