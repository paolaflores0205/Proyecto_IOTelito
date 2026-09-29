package com.example.proyecto_iotelito.ui.reservas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Reserva;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pestaña "Reservas": segmented control Activas/Finalizadas (Clase 2.3 -
 * Menús / navegación por pestañas). Dentro de "Activas" se agrupa por
 * "Próximas estadías" y "En curso" según la fecha; no existe un estado de
 * "pago pendiente" visible para el cliente, toda reserva activa ya está
 * confirmada.
 */
public class ReservasFragment extends Fragment {

    private TabLayout tabLayout;
    private ReservaAdapter reservaAdapter;
    private TextView vacioView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout);
        RecyclerView rvReservas = view.findViewById(R.id.rv_reservas);
        rvReservas.setLayoutManager(new LinearLayoutManager(requireContext()));
        reservaAdapter = new ReservaAdapter(reserva -> abrirDetalle(reserva.id));
        rvReservas.setAdapter(reservaAdapter);
        vacioView = view.findViewById(R.id.tv_reservas_vacio);

        configurarTabs();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderReservas(tab.getPosition() == 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        renderReservas(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresca por si cambió algo (ej. el estado de una reserva) mientras el usuario estaba en otra pantalla
        configurarTabs();
        renderReservas(tabLayout.getSelectedTabPosition() == 0);
    }

    private void configurarTabs() {
        int activas = 0;
        int finalizadas = 0;
        for (Reserva reserva : SampleData.RESERVAS) {
            if (reserva.esActiva()) {
                activas++;
            } else {
                finalizadas++;
            }
        }

        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab());
            tabLayout.addTab(tabLayout.newTab());
        }
        tabLayout.getTabAt(0).setText(getString(R.string.tab_activas_conteo, activas));
        tabLayout.getTabAt(1).setText(getString(R.string.tab_finalizadas_conteo, finalizadas));
    }

    private void renderReservas(boolean activas) {
        List<ReservaAdapter.Item> items = new ArrayList<>();

        if (activas) {
            List<Reserva> proximas = new ArrayList<>();
            List<Reserva> enCurso = new ArrayList<>();
            for (Reserva reserva : SampleData.RESERVAS) {
                if (!reserva.esActiva()) {
                    continue;
                }
                if (reserva.estaEnCurso()) {
                    enCurso.add(reserva);
                } else {
                    proximas.add(reserva);
                }
            }
            Comparator<Reserva> porFechaEntrada = Comparator.comparing(r -> r.fechaEntrada);
            Collections.sort(proximas, porFechaEntrada);
            Collections.sort(enCurso, porFechaEntrada);

            vacioView.setVisibility(proximas.isEmpty() && enCurso.isEmpty() ? View.VISIBLE : View.GONE);
            vacioView.setText(R.string.sin_reservas_activas);

            if (!proximas.isEmpty()) {
                items.add(ReservaAdapter.Item.encabezado(R.string.seccion_proximas_estadias));
                agregarReservas(items, proximas);
            }
            if (!enCurso.isEmpty()) {
                items.add(ReservaAdapter.Item.encabezado(R.string.seccion_en_curso));
                agregarReservas(items, enCurso);
            }
        } else {
            List<Reserva> pasadas = new ArrayList<>();
            for (Reserva reserva : SampleData.RESERVAS) {
                if (!reserva.esActiva()) {
                    pasadas.add(reserva);
                }
            }
            Collections.sort(pasadas, (a, b) -> b.fechaEntrada.compareTo(a.fechaEntrada));

            vacioView.setVisibility(pasadas.isEmpty() ? View.VISIBLE : View.GONE);
            vacioView.setText(R.string.sin_reservas_historial);

            if (!pasadas.isEmpty()) {
                items.add(ReservaAdapter.Item.encabezado(R.string.seccion_pasadas));
                agregarReservas(items, pasadas);
            }
        }
        reservaAdapter.submitList(items);
    }

    private void agregarReservas(List<ReservaAdapter.Item> items, List<Reserva> reservas) {
        for (Reserva reserva : reservas) {
            items.add(ReservaAdapter.Item.reserva(reserva));
        }
    }

    private void abrirDetalle(int reservaId) {
        Intent intent = new Intent(getActivity(), DetalleReservaActivity.class);
        intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reservaId);
        startActivity(intent);
    }
}
