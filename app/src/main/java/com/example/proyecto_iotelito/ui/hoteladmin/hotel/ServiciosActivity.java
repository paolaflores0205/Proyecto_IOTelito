package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminServiciosBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Lista de servicios adicionales del hotel (adm-servicios). Permite editar,
 * eliminar y agregar servicios. Se refresca en onResume.
 */
public class ServiciosActivity extends AppCompatActivity {

    private ActivityHoteladminServiciosBinding binding;
    private ServiciosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminServiciosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_servicios);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.fabAdd.setOnClickListener(v -> abrirFormulario(-1));
        adapter = new ServiciosAdapter(new ServiciosAdapter.OnServicioActionListener() {
            @Override public void onEditar(Servicio servicio) { abrirFormulario(servicio.id); }
            @Override public void onEliminar(Servicio servicio) { confirmarEliminar(servicio); }
        });
        binding.rvServicios.setLayoutManager(new LinearLayoutManager(this));
        binding.rvServicios.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void renderList() {
        List<Servicio> servicios = HotelAdminSampleData.servicios();
        binding.tvActivos.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_servicios_activos, servicios.size(), servicios.size()));

        adapter.setServicios(servicios);
        binding.tvSinServicios.setVisibility(servicios.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirFormulario(int servicioId) {
        Intent intent = new Intent(this, FormularioServicioActivity.class);
        if (servicioId != -1) {
            intent.putExtra(FormularioServicioActivity.EXTRA_SERVICIO_ID, servicioId);
        }
        startActivity(intent);
    }

    private void confirmarEliminar(Servicio servicio) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hoteladmin_eliminar)
                .setMessage(R.string.hoteladmin_confirmar_eliminar_serv_msg)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_eliminar, (dialog, which) -> {
                    List<Servicio> servicios = HotelAdminSampleData.servicios();
                    for (int i = 0; i < servicios.size(); i++) {
                        if (servicios.get(i).id == servicio.id) {
                            servicios.remove(i);
                            break;
                        }
                    }
                    renderList();
                    Toast.makeText(this, R.string.hoteladmin_servicio_eliminado, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
