package com.example.proyecto_iotelito.ui.reservas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pestaña "Reservas": segmented control Activas/Finalizadas (Clase 2.3 -
 * Menús / navegación por pestañas). Dentro de "Activas" se agrupa por
 * "Próximas estadías" y "En curso" según la fecha; no existe un estado de
 * "pago pendiente" visible para el cliente, toda reserva activa ya está
 * confirmada.
 */
public class ReservasFragment extends Fragment {

    private TabLayout tabLayout;
    private LinearLayout listContainer;
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
        listContainer = view.findViewById(R.id.reservas_list_container);
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
        listContainer.removeAllViews();

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
                agregarEncabezadoSeccion(R.string.seccion_proximas_estadias);
                agregarTarjetasReserva(proximas);
            }
            if (!enCurso.isEmpty()) {
                agregarEncabezadoSeccion(R.string.seccion_en_curso);
                agregarTarjetasReserva(enCurso);
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
                agregarEncabezadoSeccion(R.string.seccion_pasadas);
                agregarTarjetasReserva(pasadas);
            }
        }
    }

    private void agregarEncabezadoSeccion(int textoRes) {
        TextView header = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = listContainer.getChildCount() > 0 ? dp(20) : 0;
        params.bottomMargin = dp(8);
        header.setLayoutParams(params);
        header.setText(textoRes);
        header.setAllCaps(true);
        header.setTextSize(11);
        header.setLetterSpacing(0.05f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.io_text_muted));
        listContainer.addView(header);
    }

    private void agregarTarjetasReserva(List<Reserva> reservas) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < reservas.size(); i++) {
            View card = inflater.inflate(R.layout.item_reserva_card, listContainer, false);
            bindReservaCard(card, reservas.get(i));
            if (i > 0) {
                ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = dp(12);
            }
            listContainer.addView(card);
        }
    }

    private void bindReservaCard(View card, Reserva reserva) {
        Hotel hotel = SampleData.findById(reserva.hotelId);

        ((TextView) card.findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        ((TextView) card.findViewById(R.id.tv_habitacion)).setText(reserva.roomName);
        ((TextView) card.findViewById(R.id.tv_fechas_huespedes)).setText(
                reserva.rangoFechasTexto() + " · " + reserva.huespedes);
        ((TextView) card.findViewById(R.id.tv_precio_total)).setText(
                "S/ " + String.format(Locale.US, "%,.0f", reserva.precioTotal));

        boolean enCurso = reserva.esActiva() && reserva.estaEnCurso();
        TextView tvEstado = card.findViewById(R.id.tv_estado);
        int colorFondo = enCurso ? R.color.io_teal : EstadoUi.colorFondo(reserva.estado);
        int colorTexto = enCurso ? R.color.white : EstadoUi.colorTexto(reserva.estado);
        tvEstado.setText(enCurso ? getString(R.string.estado_en_curso) : EstadoUi.texto(requireContext(), reserva.estado));
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorFondo)));
        tvEstado.setTextColor(ContextCompat.getColor(requireContext(), colorTexto));

        MaterialButton btnVerDetalle = card.findViewById(R.id.btn_ver_detalle_reserva);
        btnVerDetalle.setOnClickListener(v -> abrirDetalle(reserva.id));
        card.setOnClickListener(v -> abrirDetalle(reserva.id));
    }

    private void abrirDetalle(int reservaId) {
        Intent intent = new Intent(getActivity(), DetalleReservaActivity.class);
        intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reservaId);
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
