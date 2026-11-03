package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFotosHotelBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminFotoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Gestión local de fotografías del hotel (adm-fotos-hotel). */
public class FotosHotelActivity extends AppCompatActivity {

    private ActivityHoteladminFotosHotelBinding binding;
    private java.util.List<String> remotePhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
        binding = ActivityHoteladminFotosHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_fotos_hotel);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.boxAgregarFoto.setOnClickListener(v -> agregarFoto());
        binding.btnGuardar.setOnClickListener(v -> guardarFotos());
        if (FirebaseHotelAdminRepository.remote(this)) {
            remotePhotos = new java.util.ArrayList<>();
            binding.boxAgregarFoto.setVisibility(View.GONE); binding.btnGuardar.setVisibility(View.GONE);
            new FirebaseHotelAdminRepository(this).observeHotel().observe(this, state -> {
                remotePhotos.clear();
                if (state.data != null) remotePhotos.addAll(state.data.photos);
                if (state.error != null) Toast.makeText(this, state.error, Toast.LENGTH_LONG).show();
                renderFotos();
            });
        } else renderFotos();
    }

    private void renderFotos() {
        binding.fotosContainer.removeAllViews();
        List<Integer> fotos = HotelAdminSampleData.FOTOS;
        int count = remotePhotos == null ? fotos.size() : remotePhotos.size();
        binding.tvContador.setText(getString(R.string.hoteladmin_fotos_total, count));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < count; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(crearFoto(inflater, i), celdaParams(false));
            if (i + 1 < count) {
                row.addView(crearFoto(inflater, i + 1), celdaParams(true));
            } else {
                row.addView(new View(this), celdaParams(true));
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (binding.fotosContainer.getChildCount() > 0) rowParams.topMargin = dp(12);
            binding.fotosContainer.addView(row, rowParams);
        }
    }

    private View crearFoto(LayoutInflater inflater, int position) {
        ItemHoteladminFotoBinding item = ItemHoteladminFotoBinding.inflate(inflater, binding.fotosContainer, false);
        if (remotePhotos == null) item.ivFoto.setImageResource(HotelAdminSampleData.FOTOS.get(position));
        else {
            item.ivFoto.setImageResource(R.drawable.superadmin_hotel_hero);
            com.example.proyecto_iotelito.data.HotelImageLoader.show(item.ivFoto, remotePhotos.get(position));
            item.tvReemplazar.setVisibility(View.GONE); item.tvEliminar.setVisibility(View.GONE);
        }
        item.tvPrincipal.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        item.tvReemplazar.setOnClickListener(v -> reemplazarFoto(position));
        item.tvEliminar.setOnClickListener(v -> confirmarEliminar(position));
        return item.getRoot();
    }

    private LinearLayout.LayoutParams celdaParams(boolean marginStart) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (marginStart) params.leftMargin = dp(12);
        return params;
    }

    private void agregarFoto() {
        int[] recursos = {R.drawable.superadmin_hotel_hero, R.drawable.superadmin_hotel_sol,
                R.drawable.superadmin_hotel_barranco};
        HotelAdminSampleData.FOTOS.add(recursos[HotelAdminSampleData.FOTOS.size() % recursos.length]);
        renderFotos();
        guardarFotosSilencioso();
        Toast.makeText(this, R.string.hoteladmin_foto_agregada, Toast.LENGTH_SHORT).show();
    }

    private void reemplazarFoto(int position) {
        int actual = HotelAdminSampleData.FOTOS.get(position);
        int reemplazo = actual == R.drawable.superadmin_hotel_sol
                ? R.drawable.superadmin_hotel_barranco : R.drawable.superadmin_hotel_sol;
        HotelAdminSampleData.FOTOS.set(position, reemplazo);
        renderFotos();
        guardarFotosSilencioso();
        Toast.makeText(this, R.string.hoteladmin_foto_reemplazada, Toast.LENGTH_SHORT).show();
    }

    private void confirmarEliminar(int position) {
        if (HotelAdminSampleData.FOTOS.size() <= 4) {
            Toast.makeText(this, R.string.hoteladmin_minimo_fotos, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.hoteladmin_confirmar_eliminar_foto)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_eliminar, (dialog, which) -> {
                    HotelAdminSampleData.FOTOS.remove(position);
                    renderFotos();
                    guardarFotosSilencioso();
                })
                .show();
    }

    private void guardarFotos() {
        HotelAdminLocalStore.get(this).saveFotos(error -> Toast.makeText(this,
                error == null ? R.string.hoteladmin_fotografias_guardadas
                        : R.string.hoteladmin_error_guardado_local, Toast.LENGTH_SHORT).show());
    }

    private void guardarFotosSilencioso() {
        HotelAdminLocalStore.get(this).saveFotos(error -> {
            if (error != null) Toast.makeText(this, R.string.hoteladmin_error_guardado_local,
                    Toast.LENGTH_LONG).show();
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
