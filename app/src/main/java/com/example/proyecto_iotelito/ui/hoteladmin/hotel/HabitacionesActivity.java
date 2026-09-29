package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminHabitacionesBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Lista de habitaciones del hotel (adm-habitaciones). Permite editar y eliminar
 * cada una y agregar nuevas mediante el FAB. La lista se refresca en onResume
 * para reflejar los cambios hechos en el formulario.
 */
public class HabitacionesActivity extends AppCompatActivity {

    private ActivityHoteladminHabitacionesBinding binding;
    private HabitacionesAdapter adapter;

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
        adapter = new HabitacionesAdapter(new HabitacionesAdapter.OnHabitacionActionListener() {
            @Override public void onEditar(Habitacion habitacion) { abrirFormulario(habitacion.id); }
            @Override public void onEliminar(Habitacion habitacion) { confirmarEliminar(habitacion); }
        });
        binding.rvHabitaciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHabitaciones.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void renderList() {
        List<Habitacion> habitaciones = HotelAdminSampleData.habitaciones();
        binding.tvListado.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_listado_habitaciones, habitaciones.size(), habitaciones.size()));

        adapter.setHabitaciones(habitaciones);
        binding.tvSinHabitaciones.setVisibility(habitaciones.isEmpty() ? View.VISIBLE : View.GONE);
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
