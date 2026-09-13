package com.example.proyecto_iotelito.ui.perfil;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Preferencias de notificaciones (Clase 3.2 - Elementos de UI: Switch).
 * Estado únicamente en memoria, sin persistencia todavía.
 */
public class NotificacionesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_notificaciones);

        bindFila(R.id.row_confirmaciones, R.string.notif_confirmaciones, R.string.notif_confirmaciones_desc, true);
        bindFila(R.id.row_mensajes, R.string.notif_mensajes, R.string.notif_mensajes_desc, true);
        bindFila(R.id.row_promociones, R.string.notif_promociones, R.string.notif_promociones_desc, false);
        bindFila(R.id.row_checkin, R.string.notif_checkin, R.string.notif_checkin_desc, true);
    }

    private void bindFila(int rowId, int tituloRes, int descripcionRes, boolean activoPorDefecto) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_titulo)).setText(tituloRes);
        ((TextView) row.findViewById(R.id.tv_descripcion)).setText(descripcionRes);
        ((MaterialSwitch) row.findViewById(R.id.switch_notificacion)).setChecked(activoPorDefecto);
    }
}
