package com.example.proyecto_iotelito.ui.perfil;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Pantalla de Notificaciones recibidas por el cliente (Mockup de Figma).
 * Muestra el listado de tarjetas de notificaciones enviadas por la app / hoteles.
 */
public class NotificacionesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
    }
}
