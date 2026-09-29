package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemHoteladminReservaBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;

import java.util.ArrayList;
import java.util.List;

/** Presenta las reservas filtradas sin recrear todas las tarjetas visibles. */
public class ReservasAdapter extends RecyclerView.Adapter<ReservasAdapter.ReservaViewHolder> {

    public interface OnReservaActionListener {
        void onDetalle(ReservaAdmin reserva);
        void onTaxi(ReservaAdmin reserva);
    }

    private final List<ReservaAdmin> reservas = new ArrayList<>();
    private final OnReservaActionListener listener;

    public ReservasAdapter(OnReservaActionListener listener) {
        this.listener = listener;
    }

    public void setReservas(List<ReservaAdmin> nuevas) {
        reservas.clear();
        reservas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHoteladminReservaBinding binding = ItemHoteladminReservaBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReservaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        holder.bind(reservas.get(position));
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    class ReservaViewHolder extends RecyclerView.ViewHolder {
        private final ItemHoteladminReservaBinding binding;

        ReservaViewHolder(ItemHoteladminReservaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReservaAdmin reserva) {
            binding.tvHuesped.setText(reserva.huespedNombre);
            binding.tvHabitacion.setText(reserva.habitacion);
            binding.tvFechas.setText(itemView.getContext().getString(
                    R.string.hoteladmin_reserva_fechas, reserva.rangoFechas, reserva.noches));
            binding.tvEstado.setText(EstadoCheckoutUi.texto(itemView.getContext(), reserva.estado));
            binding.tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    EstadoCheckoutUi.colorTexto(reserva.estado)));
            binding.tvEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    itemView.getContext(), EstadoCheckoutUi.colorFondo(reserva.estado))));
            boolean pendiente = reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
            binding.tvAlerta.setVisibility(pendiente ? View.VISIBLE : View.GONE);
            binding.btnCheckout.setVisibility(pendiente ? View.VISIBLE : View.GONE);
            binding.btnTaxi.setVisibility(reserva.tieneTaxi ? View.VISIBLE : View.GONE);
            binding.btnDetalle.setOnClickListener(v -> listener.onDetalle(reserva));
            binding.btnCheckout.setOnClickListener(v -> listener.onDetalle(reserva));
            binding.btnTaxi.setOnClickListener(v -> listener.onTaxi(reserva));
        }
    }
}
