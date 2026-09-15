package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class SeguimientoTaxiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_taxi);

        findViewById(R.id.btn_back_map).setOnClickListener(v -> finish());

        MaterialButton btnChat = findViewById(R.id.btn_abrir_chat_conductor);
        MaterialButton btnQr   = findViewById(R.id.btn_ver_qr);

        btnChat.setOnClickListener(v ->
                startActivity(new Intent(this, CodigoQrTaxiActivity.class)));
        btnQr.setOnClickListener(v ->
                startActivity(new Intent(this, CodigoQrTaxiActivity.class)));
    }
}
