package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentHoteladminHotelBinding;

import java.util.Locale;

/**
 * Pestaña "Hotel" del panel de Administrador de hotel: dashboard con el resumen
 * del día, las alertas pendientes y los accesos rápidos a la gestión del hotel
 * (habitaciones, servicios y datos). Réplica de la pantalla adm-dashboard.
 */
public class AdminHotelFragment extends Fragment {

    private FragmentHoteladminHotelBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminHotelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HotelAdminSampleData.ResumenHoy resumen = HotelAdminSampleData.resumenHoy();

        binding.tvSaludo.setText(getString(R.string.hoteladmin_saludo, HotelAdminSampleData.adminNombre));
        binding.tvSubtitulo.setText(getString(R.string.hoteladmin_panel_control, HotelAdminSampleData.hotelNombre));

        binding.tvOcupadas.setText(String.valueOf(resumen.ocupadas));
        int totalHab = resumen.ocupadas + resumen.disponibles;
        int ocupacionPct = totalHab > 0 ? Math.round(resumen.ocupadas * 100f / totalHab) : 0;
        binding.tvOcupacion.setText(getString(R.string.hoteladmin_stat_ocupacion, ocupacionPct));
        binding.tvDisponibles.setText(String.valueOf(resumen.disponibles));
        binding.tvSalidas.setText(String.valueOf(resumen.salidasHoy));
        binding.tvIngresos.setText("S/ " + String.format(Locale.US, "%,.0f", resumen.ingresosHoy));

        binding.cardHabitaciones.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HabitacionesActivity.class)));

        // TODO Hito 2: enrutar a ServiciosActivity / DatosHotelActivity cuando existan.
        View.OnClickListener proximamente = v ->
                Toast.makeText(requireContext(), R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show();
        binding.cardServicios.setOnClickListener(proximamente);
        binding.cardDatos.setOnClickListener(proximamente);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
