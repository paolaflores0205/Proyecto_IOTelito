package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Validación de la reserva para cerrar el servicio: por QR (simulado) o
 * escribiendo el código manualmente. Sin lectura real de cámara ni
 * verificación contra una reserva real todavía: el botón simula el resultado
 * en ambos casos.
 */
public class FinalizarServicioActivity extends AppCompatActivity {

    private EditText etCodigoManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalizar_servicio);

        etCodigoManual = findViewById(R.id.et_codigo_manual);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_validar_codigo).setOnClickListener(v -> validar());
    }

    private void validar() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etCodigoManual.getWindowToken(), 0);
        }
        startActivity(new Intent(this, ServicioFinalizadoActivity.class));
    }
}
