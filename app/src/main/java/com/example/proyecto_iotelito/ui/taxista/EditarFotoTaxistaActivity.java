package com.example.proyecto_iotelito.ui.taxista;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

/**
 * Edición de la foto de perfil del conductor. Sin selector real de
 * cámara/galería todavía: los botones simulan el resultado con un aviso.
 */
public class EditarFotoTaxistaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_foto_taxista);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.taxi_menu_editar_foto);

        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText("MA");

        findViewById(R.id.btn_cambiar_foto).setOnClickListener(v -> {
            Toast.makeText(this, R.string.taxi_toast_foto_actualizada, Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btn_quitar_foto).setOnClickListener(v -> {
            Toast.makeText(this, R.string.taxi_toast_foto_eliminada, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
