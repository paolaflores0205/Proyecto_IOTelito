package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.proyecto_iotelito.data.ClientLocalTasks;
import com.example.proyecto_iotelito.data.HotelPhotoStore;
import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFotosHotelBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminFotoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Edición de la galería del hotel asignado. */
public class FotosHotelActivity extends AppCompatActivity {

    private ActivityHoteladminFotosHotelBinding binding;
    private FirebaseHotelAdminRepository remote;
    private ArrayList<String> remotePhotos;
    private ArrayList<String> original;
    private int replacement = -1;
    private boolean busy;
    private boolean hotelAvailable;
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::importPhoto);

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
            remote = new FirebaseHotelAdminRepository(this);
            remotePhotos = savedInstanceState == null ? null : savedInstanceState.getStringArrayList("photoDraft");
            original = savedInstanceState == null ? null : savedInstanceState.getStringArrayList("photoOriginal");
            replacement = savedInstanceState == null ? -1 : savedInstanceState.getInt("photoReplacement", -1);
            if (remotePhotos == null) remotePhotos = new ArrayList<>();
            updateControls();
            remote.observeHotel().observe(this, state -> {
                hotelAvailable = state.data != null;
                if (state.data != null && original == null) {
                    original = new ArrayList<>(state.data.photos);
                    remotePhotos = new ArrayList<>(original);
                }
                if (state.error != null) Toast.makeText(this, state.error, Toast.LENGTH_LONG).show();
                renderFotos();
                updateControls();
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
            item.tvReemplazar.setEnabled(hotelAvailable && !busy);
            item.tvEliminar.setEnabled(hotelAvailable && !busy);
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
        if (remote != null) {
            if (!hotelAvailable || busy || remotePhotos.size() >= HotelPhotoStore.MAX_PHOTOS) return;
            replacement = -1;
            picker.launch("image/*");
            return;
        }
        int[] recursos = {R.drawable.superadmin_hotel_hero, R.drawable.superadmin_hotel_sol,
                R.drawable.superadmin_hotel_barranco};
        HotelAdminSampleData.FOTOS.add(recursos[HotelAdminSampleData.FOTOS.size() % recursos.length]);
        renderFotos();
        guardarFotosSilencioso();
        Toast.makeText(this, R.string.hoteladmin_foto_agregada, Toast.LENGTH_SHORT).show();
    }

    private void reemplazarFoto(int position) {
        if (remote != null) {
            if (!hotelAvailable || busy) return;
            replacement = position;
            picker.launch("image/*");
            return;
        }
        int actual = HotelAdminSampleData.FOTOS.get(position);
        int reemplazo = actual == R.drawable.superadmin_hotel_sol
                ? R.drawable.superadmin_hotel_barranco : R.drawable.superadmin_hotel_sol;
        HotelAdminSampleData.FOTOS.set(position, reemplazo);
        renderFotos();
        guardarFotosSilencioso();
        Toast.makeText(this, R.string.hoteladmin_foto_reemplazada, Toast.LENGTH_SHORT).show();
    }

    private void confirmarEliminar(int position) {
        if ((remote == null ? HotelAdminSampleData.FOTOS.size() : remotePhotos.size()) <= 4) {
            Toast.makeText(this, R.string.hoteladmin_minimo_fotos, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.hoteladmin_confirmar_eliminar_foto)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_eliminar, (dialog, which) -> {
                    if (remote == null) {
                        HotelAdminSampleData.FOTOS.remove(position);
                        guardarFotosSilencioso();
                    } else if (hotelAvailable && !busy) {
                        remotePhotos.remove(position);
                        updateControls();
                    }
                    renderFotos();
                })
                .show();
    }

    private void guardarFotos() {
        if (remote != null) {
            if (!hotelAvailable || busy || original == null) return;
            busy = true;
            renderFotos();
            updateControls();
            remote.savePhotos(remotePhotos, original, error -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                if (error == null) {
                    Toast.makeText(this, R.string.hoteladmin_fotografias_guardadas, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    updateControls();
                    renderFotos();
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
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

    private void importPhoto(Uri uri) {
        if (uri == null || remote == null || original == null || busy) return;
        int position = replacement;
        busy = true;
        renderFotos();
        updateControls();
        ClientLocalTasks.run(this, this, () -> HotelPhotoStore.importPhotos(this, Collections.singletonList(uri)).get(0),
                photo -> {
                    if (position < 0) remotePhotos.add(photo);
                    else remotePhotos.set(position, photo);
                    busy = false;
                    renderFotos();
                    updateControls();
                }, () -> {
                    busy = false;
                    renderFotos();
                    updateControls();
                });
    }

    private void updateControls() {
        binding.boxAgregarFoto.setEnabled(hotelAvailable && !busy && remotePhotos.size() < HotelPhotoStore.MAX_PHOTOS);
        binding.btnGuardar.setEnabled(hotelAvailable && !busy && remotePhotos.size() >= 4
                && !remotePhotos.equals(original));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putStringArrayList("photoDraft", remotePhotos);
        state.putStringArrayList("photoOriginal", original);
        state.putInt("photoReplacement", replacement);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
