package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxistaSampleData;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

/**
 * Pantalla final del flujo de servicio: resumen del traslado recién
 * completado. Desde aquí se puede volver al shell principal en la
 * pestaña Inicio o en Historial, limpiando todo el stack del flujo.
 */
public class ServicioFinalizadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicio_finalizado);

        SolicitudServicio servicio = TaxistaSampleData.servicioActivo();

        View rowPasajero = findViewById(R.id.row_pasajero);
        ((TextView) rowPasajero.findViewById(R.id.tv_avatar_inicial)).setText(servicio.pasajeroIniciales);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_nombre)).setText(servicio.pasajeroNombre);
        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(R.string.taxi_pasajero_huesped);
        rowPasajero.findViewById(R.id.iv_chat).setVisibility(View.GONE);

        bindRow(R.id.row_origen, R.string.taxi_label_origen, servicio.hotel);
        bindRow(R.id.row_destino, R.string.taxi_label_destino, servicio.destino);
        bindRow(R.id.row_distancia, R.string.label_distancia, servicio.viajeDistancia);
        bindRow(R.id.row_tiempo, R.string.label_tiempo, servicio.viajeDuracion);
        bindRow(R.id.row_tarifa, R.string.taxi_label_tarifa, getString(R.string.taxi_tarifa_monto, servicio.tarifa));
        bindRow(R.id.row_pagado_por, R.string.taxi_label_pagado_por, getString(R.string.taxi_valor_hotel));

        findViewById(R.id.btn_volver_inicio).setOnClickListener(v -> irAlShell(R.id.nav_taxi_inicio));
        findViewById(R.id.btn_ver_historial).setOnClickListener(v -> irAlShell(R.id.nav_taxi_historial));
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    /** Limpia todo el stack del flujo de servicio y vuelve al shell en la pestaña indicada. */
    private void irAlShell(int tabMenuItemId) {
        TaxistaSampleData.finalizarServicioActivo();
        Intent intent = new Intent(this, TaxistaMainActivity.class);
        intent.putExtra(TaxistaMainActivity.EXTRA_OPEN_TAB, tabMenuItemId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
