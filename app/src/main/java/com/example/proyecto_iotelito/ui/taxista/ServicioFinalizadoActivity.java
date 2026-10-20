package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

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

        if(!TaxistaFlujo.sesion(this))return;
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        TaxistaFlujo.cargar(this,vm,e->{
        TaxistaFlujo.pasajero(this,e);
        View rowPasajero = findViewById(R.id.row_pasajero);


        ((TextView) rowPasajero.findViewById(R.id.tv_pasajero_detalle)).setText(R.string.taxi_pasajero_huesped);
        rowPasajero.findViewById(R.id.iv_chat).setVisibility(View.GONE);

        bindRow(R.id.row_origen, R.string.taxi_label_origen, e.hotel);
        bindRow(R.id.row_destino, R.string.taxi_label_destino, e.destino);
        bindRow(R.id.row_distancia, R.string.label_distancia, dato(e.distancia));
        bindRow(R.id.row_tiempo, R.string.label_tiempo, dato(e.duracion));
        bindRow(R.id.row_tarifa, R.string.taxi_label_tarifa, e.cliente?getString(R.string.taxista_local_sin_tarifa):getString(R.string.taxi_tarifa_monto,e.tarifa));
        bindRow(R.id.row_pagado_por, R.string.taxi_label_pagado_por, getString(R.string.taxi_valor_hotel));

        });

        findViewById(R.id.btn_volver_inicio).setOnClickListener(v -> irAlShell(R.id.nav_taxi_inicio));
        findViewById(R.id.btn_ver_historial).setOnClickListener(v -> irAlShell(R.id.nav_taxi_historial));
    }

    private String dato(String s){return s==null||s.isEmpty()?getString(R.string.taxista_local_sin_dato):s;}

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    /** Limpia todo el stack del flujo de servicio y vuelve al shell en la pestaña indicada. */
    private void irAlShell(int tabMenuItemId) {
        Intent intent = new Intent(this, TaxistaMainActivity.class);
        intent.putExtra(TaxistaMainActivity.EXTRA_OPEN_TAB, tabMenuItemId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
