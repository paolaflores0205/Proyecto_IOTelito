package com.example.proyecto_iotelito.ui.taxista;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.google.android.material.tabs.TabLayout;

/**
 * Pestaña "Historial": traslados pasados del conductor, agrupados por
 * pestañas (Todos / Completados / Cancelados). Tarjetas con contenido de
 * ejemplo; el listado dinámico llega en el siguiente incremento.
 */
public class HistorialTaxistaFragment extends Fragment {

    private final EstadoServicio[] cardEstados = new EstadoServicio[3];
    private View[] cardViews = new View[3];
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_taxista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindHistorial(0, view.findViewById(R.id.card_historial_1), "14:10",
                EstadoServicio.FINALIZADO,
                "Hotel Miraflores Park → Aeropuerto Jorge Chávez",
                "María García · Toyota Corolla", "18.2 km · 25 min");

        bindHistorial(1, view.findViewById(R.id.card_historial_2), "10:45",
                EstadoServicio.FINALIZADO,
                "Dazzler Miraflores → Aeropuerto Callao",
                "Juan Pérez · Toyota Corolla", "15 km · 32 min");

        bindHistorial(2, view.findViewById(R.id.card_historial_3), "ayer, 19:30",
                EstadoServicio.CANCELADO,
                "Casa Andina Premium → Aeropuerto Jorge Chávez",
                "Lucía Fernández · Toyota Corolla", "Cancelado por el pasajero");

        emptyView = view.findViewById(R.id.tv_historial_vacio);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout_historial);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filtrarPorPestaña(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        filtrarPorPestaña(0);
    }

    private void filtrarPorPestaña(int posicion) {
        boolean hayVisibles = false;
        for (int i = 0; i < cardViews.length; i++) {
            boolean visible = posicion == 0
                    || (posicion == 1 && cardEstados[i] == EstadoServicio.FINALIZADO)
                    || (posicion == 2 && cardEstados[i] == EstadoServicio.CANCELADO);
            cardViews[i].setVisibility(visible ? View.VISIBLE : View.GONE);
            hayVisibles |= visible;
        }
        emptyView.setVisibility(hayVisibles ? View.GONE : View.VISIBLE);
    }

    private void bindHistorial(int index, View card, String hora, EstadoServicio estado,
                                String ruta, String pasajero, String distanciaTiempo) {
        ((TextView) card.findViewById(R.id.tv_hora)).setText(hora);

        TextView tvEstado = card.findViewById(R.id.tv_estado_pill);
        tvEstado.setText(EstadoServicioUi.texto(requireContext(), estado));
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), EstadoServicioUi.colorFondo(estado))));
        tvEstado.setTextColor(ContextCompat.getColor(requireContext(), EstadoServicioUi.colorTexto(estado)));

        ((TextView) card.findViewById(R.id.tv_ruta)).setText(ruta);
        ((TextView) card.findViewById(R.id.tv_pasajero)).setText(pasajero);
        ((TextView) card.findViewById(R.id.tv_distancia_tiempo)).setText(distanciaTiempo);

        cardViews[index] = card;
        cardEstados[index] = estado;
    }
}
