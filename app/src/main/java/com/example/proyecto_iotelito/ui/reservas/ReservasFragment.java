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
import java.util.List;
import java.util.Locale;

/**
 * Pestaña "Reservas": tabs Activas/Historial (Clase 2.3 - Menús /
 * navegación por pestañas) sobre la lista estática de {@link Reserva}.
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
        // refresca por si el usuario acaba de pagar una reserva pendiente
        renderReservas(tabLayout.getSelectedTabPosition() == 0);
    }

    private void renderReservas(boolean activas) {
        listContainer.removeAllViews();
        List<Reserva> filtradas = new ArrayList<>();
        for (Reserva reserva : SampleData.RESERVAS) {
            if (reserva.esActiva() == activas) {
                filtradas.add(reserva);
            }
        }

        vacioView.setVisibility(filtradas.isEmpty() ? View.VISIBLE : View.GONE);
        vacioView.setText(activas ? R.string.sin_reservas_activas : R.string.sin_reservas_historial);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Reserva reserva : filtradas) {
            View card = inflater.inflate(R.layout.item_reserva_card, listContainer, false);
            bindReservaCard(card, reserva);
            if (listContainer.getChildCount() > 0) {
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
                reserva.rangoFechas + " · " + reserva.huespedes);
        ((TextView) card.findViewById(R.id.tv_precio_total)).setText(
                "S/ " + String.format(Locale.US, "%,.0f", reserva.precioTotal));

        TextView tvEstado = card.findViewById(R.id.tv_estado);
        tvEstado.setText(EstadoUi.texto(requireContext(), reserva.estado));
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), EstadoUi.colorFondo(reserva.estado))));
        tvEstado.setTextColor(ContextCompat.getColor(requireContext(), EstadoUi.colorTexto(reserva.estado)));

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
