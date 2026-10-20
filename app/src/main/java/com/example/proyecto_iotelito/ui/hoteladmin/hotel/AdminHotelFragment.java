package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.databinding.FragmentHoteladminHotelBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.CobroCheckoutActivity;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.EstadoTaxiActivity;

import java.util.Locale;

/**
 * Pestaña "Hotel" del panel de Administrador de hotel: dashboard con el resumen
 * del día, las alertas pendientes y los accesos rápidos a la gestión del hotel
 * (habitaciones, servicios y datos). Réplica de la pantalla adm-dashboard.
 */
public class AdminHotelFragment extends Fragment {

    private static final int ALERTA_CHECKOUT_RESERVA_ID = 101;
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
        SuperadminRepository.getInstance(requireContext()).hotel(HotelAdminSampleData.HOTEL_ID)
                .observe(getViewLifecycleOwner(), hotel -> {
                    HotelAdminSampleData.aplicarHotelCompartido(hotel);
                    if (hotel != null && binding != null) {
                        binding.tvSubtitulo.setText(getString(
                                R.string.hoteladmin_panel_control, hotel.name));
                    }
                });
        binding.cardHabitaciones.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HabitacionesActivity.class)));
        binding.cardServicios.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ServiciosActivity.class)));

        binding.cardDatos.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DatosHotelActivity.class)));
        binding.cardAlertaTaxi.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EstadoTaxiActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        HotelAdminSampleData.ResumenHoy resumen = HotelAdminSampleData.resumenHoy();
        binding.tvSaludo.setText(getString(R.string.hoteladmin_saludo, HotelAdminSampleData.adminNombre));
        binding.tvSubtitulo.setText(getString(R.string.hoteladmin_panel_control, HotelAdminSampleData.hotelNombre));
        binding.tvOcupadas.setText(String.valueOf(resumen.ocupadas));
        int totalHab = resumen.ocupadas + resumen.disponibles;
        int ocupacionPct = totalHab > 0 ? Math.round(resumen.ocupadas * 100f / totalHab) : 0;
        binding.tvOcupacion.setText(getString(R.string.hoteladmin_stat_ocupacion, ocupacionPct));
        binding.tvDisponibles.setText(String.valueOf(resumen.disponibles));
        binding.tvSalidas.setText(String.valueOf(resumen.salidasHoy));
        binding.tvIngresos.setText(getString(R.string.hoteladmin_moneda,
                String.format(Locale.US, "%,.0f", resumen.ingresosHoy)));
        actualizarAlertaCheckout();
    }

    private void actualizarAlertaCheckout() {
        ReservaAdmin pendiente = null;
        for (ReservaAdmin reserva : HotelAdminSampleData.reservas()) {
            if (reserva.id == ALERTA_CHECKOUT_RESERVA_ID
                    && reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE) {
                pendiente = reserva;
                break;
            }
        }
        binding.cardAlertaCheckout.setVisibility(pendiente == null ? View.GONE : View.VISIBLE);
        binding.tvAlertas.setText(getString(R.string.hoteladmin_alertas,
                pendiente == null ? 1 : 2));
        if (pendiente != null) {
            int reservaId = pendiente.id;
            binding.cardAlertaCheckout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CobroCheckoutActivity.class);
                intent.putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID, reservaId);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
