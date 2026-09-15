package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

/**
 * Pestaña "Solicitudes": servicios disponibles cerca del conductor.
 * Tarjetas con contenido de ejemplo; el listado dinámico (RecyclerView)
 * llega en el siguiente incremento.
 */
public class SolicitudesTaxistaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_solicitudes_taxista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((com.google.android.material.chip.Chip) view.findViewById(R.id.chip_todas)).setChecked(true);

        bindSolicitud(view.findViewById(R.id.card_solicitud_1),
                "Menos de 5 min", "Hotel Miraflores Park",
                "Aeropuerto Internacional Jorge Chávez", "02 ago 2026, 14:00",
                "Ana García", "AG", "14 min · 6.2 km");

        bindSolicitud(view.findViewById(R.id.card_solicitud_2),
                "20 min", "Palacio del Inka Cusco (Lima Branch)",
                "Aeropuerto Internacional Jorge Chávez", "03 ago 2026, 15:30",
                "Carlos Mendoza", "CM", "22 min · 9.8 km");

        bindSolicitud(view.findViewById(R.id.card_solicitud_3),
                "10 min", "Dazzler Miraflores",
                "Aeropuerto Internacional Jorge Chávez", "04 ago 2026, 09:15",
                "Rodrigo Salas", "RS", "11 min · 4.5 km");

        bindSolicitud(view.findViewById(R.id.card_solicitud_4),
                "35 min", "Barranco Art Boutique",
                "Terminal Terrestre Javier Prado", "05 ago 2026, 11:00",
                "Valentina Ríos", "VR", "38 min · 21 km");
    }

    private void bindSolicitud(View card, String tiempo, String hotel, String destino, String fechaHora,
                                String pasajeroNombre, String iniciales, String distanciaTiempo) {
        ((TextView) card.findViewById(R.id.tv_tiempo_pill)).setText(tiempo);
        ((TextView) card.findViewById(R.id.tv_hotel_nombre)).setText(hotel);
        ((TextView) card.findViewById(R.id.tv_destino)).setText(destino);
        ((TextView) card.findViewById(R.id.tv_fecha_hora)).setText(fechaHora);

        MaterialButton btnVerSolicitud = card.findViewById(R.id.btn_ver_solicitud);
        btnVerSolicitud.setOnClickListener(v -> startActivity(DetalleServicioActivity.createIntent(
                getActivity(), hotel, destino, pasajeroNombre, iniciales, distanciaTiempo)));
    }
}
