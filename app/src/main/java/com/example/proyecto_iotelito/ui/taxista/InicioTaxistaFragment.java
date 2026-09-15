package com.example.proyecto_iotelito.ui.taxista;

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
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Pestaña "Inicio" del taxista: saludo, disponibilidad, resumen del día
 * y acceso rápido a las solicitudes pendientes. Contenido con datos de
 * ejemplo; el listado real de solicitudes llega en el siguiente incremento.
 */
public class InicioTaxistaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio_taxista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.tv_saludo))
                .setText(getString(R.string.taxi_saludo, "Miguel"));

        ((TextView) view.findViewById(R.id.tv_avatar_inicial)).setText("MA");
        ((TextView) view.findViewById(R.id.tv_nombre_conductor)).setText("Miguel Arispe S.");
        ((TextView) view.findViewById(R.id.tv_rating_vehiculo)).setText("★ 4.8 · Toyota Corolla (ABC-123)");

        ((TextView) view.findViewById(R.id.tv_stat_servicios)).setText("5");
        ((TextView) view.findViewById(R.id.tv_stat_trasladados)).setText("3");
        ((TextView) view.findViewById(R.id.tv_stat_calificacion)).setText("4.8");

        TextView tvEstadoDisponibilidad = view.findViewById(R.id.tv_estado_disponibilidad);
        MaterialSwitch switchDisponibilidad = view.findViewById(R.id.switch_disponibilidad);
        switchDisponibilidad.setOnCheckedChangeListener((buttonView, isChecked) ->
                tvEstadoDisponibilidad.setText(isChecked
                        ? R.string.taxi_disponible_desc
                        : R.string.taxi_no_disponible_desc));

        MaterialButton btnVerSolicitudes = view.findViewById(R.id.btn_ver_solicitudes);
        btnVerSolicitudes.setText(getString(R.string.taxi_btn_ver_solicitudes, 4));
        btnVerSolicitudes.setOnClickListener(v -> irASolicitudes());

        view.findViewById(R.id.banner_nueva_solicitud).setOnClickListener(v -> irASolicitudes());
    }

    private void irASolicitudes() {
        if (getActivity() instanceof TaxistaMainActivity) {
            ((TaxistaMainActivity) getActivity()).selectTab(R.id.nav_taxi_solicitudes);
        }
    }
}
