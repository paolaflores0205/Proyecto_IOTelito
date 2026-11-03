package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
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
    private FirebaseHotelAdminRepository remote;
    private boolean remoteReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
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
        if (FirebaseHotelAdminRepository.remote(this)) {
            remote = new FirebaseHotelAdminRepository(this);
            binding.fabAdd.setEnabled(false);
            remote.observe("services", Servicio.class).observe(this, state -> {
                remoteReady = !state.loading && state.error == null;
                binding.fabAdd.setEnabled(remoteReady);
                renderList(state.data);
                if (!remoteReady) {
                    binding.tvSinServicios.setVisibility(View.VISIBLE);
                    binding.tvSinServicios.setText(state.error == null ? getString(R.string.hoteladmin_cargando_remoto) : state.error);
                } else binding.tvSinServicios.setText(R.string.hoteladmin_sin_servicios);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && remote == null) renderList();
    }

    private void renderList() {
        renderList(HotelAdminSampleData.servicios());
    }

    private void renderList(List<Servicio> servicios) {
        int activeCount = 0;
        for (Servicio service : servicios) if (service.activo) activeCount++;
        binding.tvActivos.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_servicios_activos, activeCount, activeCount));

        adapter.setServicios(servicios);
        binding.tvSinServicios.setVisibility(servicios.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirFormulario(int servicioId) {
        if (remote != null && !remoteReady) return;
        Intent intent = new Intent(this, FormularioServicioActivity.class);
        if (servicioId != -1) {
            intent.putExtra(FormularioServicioActivity.EXTRA_SERVICIO_ID, servicioId);
        }
        startActivity(intent);
    }

    private void confirmarEliminar(Servicio servicio) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hoteladmin_eliminar)
                .setMessage(remote != null ? R.string.hoteladmin_archivar_servicio : R.string.hoteladmin_confirmar_eliminar_serv_msg)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_eliminar, (dialog, which) -> {
                    if (remote != null) {
                        if (!remoteReady) return;
                        remote.archiveService(servicio.id, error -> {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(this, error == null ? getString(R.string.hoteladmin_servicio_eliminado) : error, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    HotelAdminLocalStore.get(this).deleteServicio(servicio.id, error -> {
                        if (error != null) {
                            Toast.makeText(this, R.string.hoteladmin_error_guardado_local,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        List<Servicio> servicios = HotelAdminSampleData.servicios();
                        servicios.removeIf(item -> item.id == servicio.id);
                        renderList();
                        Toast.makeText(this, R.string.hoteladmin_servicio_eliminado,
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .show();
    }
}
