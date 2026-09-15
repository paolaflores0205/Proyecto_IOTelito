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
import com.example.proyecto_iotelito.databinding.ActivityHoteladminServiciosBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminServicioBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

/**
 * Lista de servicios adicionales del hotel (adm-servicios). Permite editar,
 * eliminar y agregar servicios. Se refresca en onResume.
 */
public class ServiciosActivity extends AppCompatActivity {

    private ActivityHoteladminServiciosBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminServiciosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_servicios);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.fabAdd.setOnClickListener(v -> abrirFormulario(-1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void renderList() {
        binding.llServicios.removeAllViews();
        List<Servicio> servicios = HotelAdminSampleData.servicios();
        binding.tvActivos.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_servicios_activos, servicios.size(), servicios.size()));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Servicio s : servicios) {
            ItemHoteladminServicioBinding item =
                    ItemHoteladminServicioBinding.inflate(inflater, binding.llServicios, false);
            item.ivFoto.setImageResource(s.fotoRes);
            item.tvNombre.setText(s.nombre);
            item.tvDescripcion.setText(s.descripcion);
            item.tvPrecio.setText("S/ " + String.format(Locale.US, "%,.0f", s.precio));
            item.tvPrecioSuffix.setText(getString(R.string.hoteladmin_precio_unidad, s.unidad));

            item.tvBadge.setText(s.activo
                    ? R.string.hoteladmin_badge_activo : R.string.hoteladmin_badge_inactivo);
            item.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    s.activo ? R.color.io_success_bg : R.color.io_tag_bg)));
            item.tvBadge.setTextColor(ContextCompat.getColor(this,
                    s.activo ? R.color.io_success_text : R.color.io_text_secondary));

            item.tvEditar.setOnClickListener(v -> abrirFormulario(s.id));
            item.tvEliminar.setOnClickListener(v -> confirmarEliminar(s));
            binding.llServicios.addView(item.getRoot());
        }
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
