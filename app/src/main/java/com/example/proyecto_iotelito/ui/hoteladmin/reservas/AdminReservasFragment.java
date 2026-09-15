package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentHoteladminReservasBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminReservaBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;

import java.util.Locale;

/** Pantalla adm-reservas-checkout: búsqueda, filtros y lista inflada manualmente. */
public class AdminReservasFragment extends Fragment {
    private FragmentHoteladminReservasBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminReservasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.etBuscar.addTextChangedListener(new SimpleTextWatcher(this::renderReservas));
        binding.chipGroupEstado.setOnCheckedStateChangeListener((group, ids) -> renderReservas());
        binding.chipGroupFecha.setOnCheckedStateChangeListener((group, ids) -> renderReservas());
        binding.chipProximas.setChecked(true);
        binding.chipTodasFechas.setChecked(true);
        renderReservas();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) renderReservas();
    }

    private void renderReservas() {
        String query = String.valueOf(binding.etBuscar.getText()).trim().toLowerCase(Locale.ROOT);
        ReservaAdmin.EstadoCheckout filtro = estadoSeleccionado();
        binding.llReservas.removeAllViews();
        int visibles = 0;
        for (ReservaAdmin reserva : HotelAdminSampleData.reservas()) {
            String searchable = (reserva.huespedNombre + " " + reserva.habitacion + " "
                    + reserva.huespedDoc).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (filtro != null && reserva.estado != filtro) continue;
            if (!coincideFecha(reserva)) continue;

            ItemHoteladminReservaBinding item = ItemHoteladminReservaBinding.inflate(
                    getLayoutInflater(), binding.llReservas, false);
            item.tvHuesped.setText(reserva.huespedNombre);
            item.tvHabitacion.setText(reserva.habitacion);
            item.tvFechas.setText(getString(R.string.hoteladmin_reserva_fechas,
                    reserva.rangoFechas, reserva.noches));
            item.tvEstado.setText(EstadoCheckoutUi.texto(requireContext(), reserva.estado));
            item.tvEstado.setTextColor(ContextCompat.getColor(requireContext(),
                    EstadoCheckoutUi.colorTexto(reserva.estado)));
            item.tvEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    requireContext(), EstadoCheckoutUi.colorFondo(reserva.estado))));
            boolean pendiente = reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
            item.tvAlerta.setVisibility(pendiente ? View.VISIBLE : View.GONE);
            item.btnCheckout.setVisibility(pendiente ? View.VISIBLE : View.GONE);
            item.btnTaxi.setVisibility(reserva.tieneTaxi ? View.VISIBLE : View.GONE);
            item.btnDetalle.setOnClickListener(v -> abrirCheckout(reserva.id));
            item.btnCheckout.setOnClickListener(v -> abrirCheckout(reserva.id));
            item.btnTaxi.setOnClickListener(v -> startActivity(
                    new Intent(requireContext(), EstadoTaxiActivity.class)));
            binding.llReservas.addView(item.getRoot());
            visibles++;
        }
        binding.tvCantidad.setText(getResources().getQuantityString(
                R.plurals.hoteladmin_reservas_encontradas, visibles, visibles));
        binding.tvSinResultados.setVisibility(visibles == 0 ? View.VISIBLE : View.GONE);
    }

    private ReservaAdmin.EstadoCheckout estadoSeleccionado() {
        int id = binding.chipGroupEstado.getCheckedChipId();
        if (id == R.id.chip_proximas) return ReservaAdmin.EstadoCheckout.PROXIMA;
        if (id == R.id.chip_hospedados) return ReservaAdmin.EstadoCheckout.HOSPEDADO;
        if (id == R.id.chip_checkout) return ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
        return null;
    }

    private boolean coincideFecha(ReservaAdmin reserva) {
        int id = binding.chipGroupFecha.getCheckedChipId();
        if (id == R.id.chip_hoy) {
            return reserva.estado == ReservaAdmin.EstadoCheckout.HOSPEDADO
                    || reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
        }
        if (id == R.id.chip_semana) {
            return reserva.estado != ReservaAdmin.EstadoCheckout.FINALIZADA;
        }
        return true;
    }

    private void abrirCheckout(int reservaId) {
        Intent intent = new Intent(requireContext(), CobroCheckoutActivity.class);
        intent.putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID, reservaId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable after;
        SimpleTextWatcher(Runnable after) { this.after = after; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { after.run(); }
    }
}
