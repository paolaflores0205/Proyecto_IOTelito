package com.example.proyecto_iotelito.ui.taxista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Pestaña "Perfil" del taxista: datos personales, vehículo verificado,
 * disponibilidad y accesos de cuenta. Contenido con datos de ejemplo;
 * la edición real llega en el siguiente incremento.
 */
public class PerfilTaxistaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_taxista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindRow(view, R.id.row_dni, R.string.taxi_label_dni, "68492135");
        bindRow(view, R.id.row_telefono, R.string.taxi_label_telefono, "+51 987 654 321");
        bindRow(view, R.id.row_correo, R.string.taxi_label_correo, "miguel.arispe@iotelito.com");

        bindRow(view, R.id.row_modelo, R.string.taxi_label_modelo, "Toyota Corolla (2024)");
        bindRow(view, R.id.row_placa, R.string.taxi_label_placa, "ABC-123");
        bindRow(view, R.id.row_color, R.string.taxi_label_color, "Gris Metálico");

        TextView tvEstadoDisponibilidad = view.findViewById(R.id.tv_estado_disponibilidad_perfil);
        MaterialSwitch switchDisponibilidad = view.findViewById(R.id.switch_disponibilidad_perfil);
        switchDisponibilidad.setOnCheckedChangeListener((buttonView, isChecked) ->
                tvEstadoDisponibilidad.setText(isChecked
                        ? R.string.taxi_disponible_desc
                        : R.string.taxi_no_disponible_desc));

        View rowEditarFoto = view.findViewById(R.id.row_editar_foto);
        ((ImageView) rowEditarFoto.findViewById(R.id.iv_icono)).setImageResource(R.drawable.ic_edit);
        ((TextView) rowEditarFoto.findViewById(R.id.tv_label)).setText(R.string.taxi_menu_editar_foto);
        rowEditarFoto.setOnClickListener(v -> mostrarProximamente());

        View rowSoporte = view.findViewById(R.id.row_soporte);
        ((ImageView) rowSoporte.findViewById(R.id.iv_icono)).setImageResource(R.drawable.ic_mail);
        ((TextView) rowSoporte.findViewById(R.id.tv_label)).setText(R.string.taxi_menu_soporte);
        rowSoporte.setOnClickListener(v -> mostrarProximamente());

        view.findViewById(R.id.tv_cerrar_sesion).setOnClickListener(v ->
                Toast.makeText(getContext(), R.string.taxi_toast_sesion_cerrada, Toast.LENGTH_SHORT).show());
    }

    private void bindRow(View parent, int rowId, int labelRes, String value) {
        View row = parent.findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private void mostrarProximamente() {
        Toast.makeText(getContext(), R.string.taxi_toast_proximamente, Toast.LENGTH_SHORT).show();
    }
}
