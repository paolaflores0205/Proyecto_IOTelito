package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
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
    private boolean remoteMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminHotelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        remoteMode = FirebaseHotelAdminRepository.remote(requireContext());
        // Android puede restaurar el fragment antes de que el shell redirija al login.
        if (remoteMode && SessionRepository.obtener(requireContext()) == null) {
            binding.tvSubtitulo.setText(R.string.hoteladmin_cargando_remoto);
            binding.cardDatos.setEnabled(false); binding.cardHabitaciones.setEnabled(false); binding.cardServicios.setEnabled(false);
            return;
        }
        if (remoteMode) configureRemote();
        else SuperadminRepository.getInstance(requireContext()).hotel(HotelAdminSampleData.HOTEL_ID)
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
        if (remoteMode) return;
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

    private void configureRemote() {
        FirebaseHotelAdminRepository repository = new FirebaseHotelAdminRepository(requireContext());
        binding.tvSaludo.setText(getString(R.string.hoteladmin_saludo, SessionRepository.obtener(requireContext()).getDisplayName()));
        binding.cardAlertaCheckout.setVisibility(View.GONE);
        binding.cardAlertaTaxi.setVisibility(View.GONE);
        binding.tvAlertas.setVisibility(View.GONE);
        binding.tvOcupadas.setText("—"); binding.tvOcupacion.setText("—");
        binding.tvSalidas.setText("—"); binding.tvIngresos.setText("—");
        binding.tvComparacionIngresos.setVisibility(View.GONE);
        binding.tvDisponibles.setText("—");
        binding.btnCerrarSesion.setVisibility(View.VISIBLE);
        binding.btnCerrarSesion.setOnClickListener(v -> {
            SessionRepository.cerrar(requireContext());
            startActivity(new Intent(requireContext(), com.example.proyecto_iotelito.ui.auth.LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
        repository.observeHotel().observe(getViewLifecycleOwner(), state -> {
            binding.cardDatos.setEnabled(state.data != null);
            binding.cardHabitaciones.setEnabled(state.data != null);
            binding.cardServicios.setEnabled(state.data != null);
            binding.tvSubtitulo.setText(state.data != null ? getString(R.string.hoteladmin_panel_control, state.data.name)
                    : state.error == null ? getString(R.string.hoteladmin_cargando_remoto) : state.error);
        });
        repository.observe("rooms", com.example.proyecto_iotelito.model.hoteladmin.Habitacion.class).observe(getViewLifecycleOwner(), state -> {
            if (state.loading || state.error != null) binding.tvDisponibles.setText("—");
            else {
                int available = 0;
                for (com.example.proyecto_iotelito.model.hoteladmin.Habitacion room : state.data) if (room.disponible) available++;
                binding.tvDisponibles.setText(String.valueOf(available));
            }
        });
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
