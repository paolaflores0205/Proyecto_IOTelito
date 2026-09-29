package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemHoteladminHabitacionBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Une las habitaciones de ejemplo con las tarjetas existentes. */
public class HabitacionesAdapter extends RecyclerView.Adapter<HabitacionesAdapter.HabitacionViewHolder> {

    public interface OnHabitacionActionListener {
        void onEditar(Habitacion habitacion);
        void onEliminar(Habitacion habitacion);
    }

    private final List<Habitacion> habitaciones = new ArrayList<>();
    private final OnHabitacionActionListener listener;

    public HabitacionesAdapter(OnHabitacionActionListener listener) {
        this.listener = listener;
    }

    public void setHabitaciones(List<Habitacion> nuevas) {
        habitaciones.clear();
        habitaciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HabitacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHoteladminHabitacionBinding binding = ItemHoteladminHabitacionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HabitacionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitacionViewHolder holder, int position) {
        holder.bind(habitaciones.get(position));
    }

    @Override
    public int getItemCount() {
        return habitaciones.size();
    }

    class HabitacionViewHolder extends RecyclerView.ViewHolder {
        private final ItemHoteladminHabitacionBinding binding;

        HabitacionViewHolder(ItemHoteladminHabitacionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Habitacion habitacion) {
            binding.ivFoto.setImageResource(habitacion.fotoRes);
            binding.tvTipo.setText(habitacion.tipo);
            binding.tvAforoArea.setText(itemView.getContext().getString(
                    R.string.hoteladmin_aforo_area, habitacion.aforo(), habitacion.areaM2));
            binding.tvPrecio.setText(itemView.getContext().getString(R.string.hoteladmin_moneda,
                    String.format(Locale.US, "%,.0f", habitacion.precioNoche)));
            binding.tvBadge.setText(habitacion.disponible
                    ? R.string.hoteladmin_badge_disponible : R.string.hoteladmin_badge_ocupada);
            binding.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    itemView.getContext(), habitacion.disponible ? R.color.io_success_bg : R.color.io_danger_bg)));
            binding.tvBadge.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    habitacion.disponible ? R.color.io_success_text : R.color.io_danger_text));
            binding.tvEditar.setOnClickListener(v -> listener.onEditar(habitacion));
            binding.tvEliminar.setOnClickListener(v -> listener.onEliminar(habitacion));
        }
    }
}
