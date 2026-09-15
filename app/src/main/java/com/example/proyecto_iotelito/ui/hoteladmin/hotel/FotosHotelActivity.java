package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFotosHotelBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminFotoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Gestión local de fotografías del hotel (adm-fotos-hotel). */
public class FotosHotelActivity extends AppCompatActivity {

    private ActivityHoteladminFotosHotelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminFotosHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_fotos_hotel);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.boxAgregarFoto.setOnClickListener(v -> agregarFoto());
        binding.btnGuardar.setOnClickListener(v ->
                Toast.makeText(this, R.string.hoteladmin_fotografias_guardadas, Toast.LENGTH_SHORT).show());
        renderFotos();
    }

    private void renderFotos() {
        binding.fotosContainer.removeAllViews();
        List<Integer> fotos = HotelAdminSampleData.FOTOS;
        binding.tvContador.setText(getString(R.string.hoteladmin_fotos_total, fotos.size()));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < fotos.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(crearFoto(inflater, i), celdaParams(false));
            if (i + 1 < fotos.size()) {
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
        item.ivFoto.setImageResource(HotelAdminSampleData.FOTOS.get(position));
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
        Toast.makeText(this, R.string.hoteladmin_foto_agregada, Toast.LENGTH_SHORT).show();
    }

    private void reemplazarFoto(int position) {
        int actual = HotelAdminSampleData.FOTOS.get(position);
        int reemplazo = actual == R.drawable.superadmin_hotel_sol
                ? R.drawable.superadmin_hotel_barranco : R.drawable.superadmin_hotel_sol;
        HotelAdminSampleData.FOTOS.set(position, reemplazo);
        renderFotos();
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
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
