package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminHabitacionesBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminHabitacionBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

/**
 * Lista de habitaciones del hotel (adm-habitaciones). Permite editar y eliminar
 * cada una y agregar nuevas mediante el FAB. La lista se refresca en onResume
 * para reflejar los cambios hechos en el formulario.
 */
public class HabitacionesActivity extends AppCompatActivity {

    private ActivityHoteladminHabitacionesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminHabitacionesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_habitaciones);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());

        binding.tvFiltrar.setOnClickListener(v ->
                Toast.makeText(this, R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show());

        binding.fabAdd.setOnClickListener(v -> abrirFormulario(-1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void renderList() {
        binding.llHabitaciones.removeAllViews();
        List<Habitacion> habitaciones = HotelAdminSampleData.habitaciones();
        binding.tvListado.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_listado_habitaciones, habitaciones.size(), habitaciones.size()));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Habitacion h : habitaciones) {
            ItemHoteladminHabitacionBinding item =
                    ItemHoteladminHabitacionBinding.inflate(inflater, binding.llHabitaciones, false);
            item.ivFoto.setImageResource(h.fotoRes);
            item.tvTipo.setText(h.tipo);
            item.tvAforoArea.setText(getString(R.string.hoteladmin_aforo_area, h.aforo(), h.areaM2));
            item.tvPrecio.setText("S/ " + String.format(Locale.US, "%,.0f", h.precioNoche));

            item.tvBadge.setText(h.disponible
                    ? R.string.hoteladmin_badge_disponible : R.string.hoteladmin_badge_ocupada);
            item.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    h.disponible ? R.color.io_success_bg : R.color.io_danger_bg)));
            item.tvBadge.setTextColor(ContextCompat.getColor(this,
                    h.disponible ? R.color.io_success_text : R.color.io_danger_text));

            item.tvEditar.setOnClickListener(v -> abrirFormulario(h.id));
            item.tvEliminar.setOnClickListener(v -> confirmarEliminar(h));
            binding.llHabitaciones.addView(item.getRoot());
        }
    }

    private void abrirFormulario(int habitacionId) {
        Intent intent = new Intent(this, FormularioHabitacionActivity.class);
        if (habitacionId != -1) {
            intent.putExtra(FormularioHabitacionActivity.EXTRA_HABITACION_ID, habitacionId);
        }
        startActivity(intent);
    }

    private void confirmarEliminar(Habitacion habitacion) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hoteladmin_eliminar)
                .setMessage(R.string.hoteladmin_confirmar_eliminar_hab_msg)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_eliminar, (dialog, which) -> {
                    List<Habitacion> habitaciones = HotelAdminSampleData.habitaciones();
                    for (int i = 0; i < habitaciones.size(); i++) {
                        if (habitaciones.get(i).id == habitacion.id) {
                            habitaciones.remove(i);
                            break;
                        }
                    }
                    renderList();
                    Toast.makeText(this, R.string.hoteladmin_habitacion_eliminada, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
