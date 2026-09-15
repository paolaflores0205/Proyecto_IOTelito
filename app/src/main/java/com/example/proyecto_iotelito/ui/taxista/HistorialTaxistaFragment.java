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

/**
 * Pestaña "Historial": traslados pasados del conductor, agrupados por
 * pestañas (Todos / Completados / Cancelados). Tarjetas con contenido de
 * ejemplo; el listado dinámico llega en el siguiente incremento.
 */
public class HistorialTaxistaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_taxista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindHistorial(view.findViewById(R.id.card_historial_1), "14:10",
                EstadoServicio.FINALIZADO,
                "Hotel Miraflores Park → Aeropuerto Jorge Chávez",
                "María García · Toyota Corolla", "18.2 km · 25 min");

        bindHistorial(view.findViewById(R.id.card_historial_2), "10:45",
                EstadoServicio.FINALIZADO,
                "Dazzler Miraflores → Aeropuerto Callao",
                "Juan Pérez · Toyota Corolla", "15 km · 32 min");
    }

    private void bindHistorial(View card, String hora, EstadoServicio estado,
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
    }
}
