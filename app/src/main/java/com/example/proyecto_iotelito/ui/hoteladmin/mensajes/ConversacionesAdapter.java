package com.example.proyecto_iotelito.ui.hoteladmin.mensajes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.databinding.ItemHoteladminConversacionBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Conversacion;

import java.util.ArrayList;
import java.util.List;

/** Presenta las conversaciones usando la tarjeta ya definida para el administrador. */
public class ConversacionesAdapter extends RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder> {

    public interface OnConversacionClickListener {
        void onConversacionClick(Conversacion conversacion);
    }

    private final List<Conversacion> conversaciones = new ArrayList<>();
    private final OnConversacionClickListener listener;

    public ConversacionesAdapter(OnConversacionClickListener listener) {
        this.listener = listener;
    }

    public void setConversaciones(List<Conversacion> nuevas) {
        conversaciones.clear();
        conversaciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHoteladminConversacionBinding binding = ItemHoteladminConversacionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversacionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversacionViewHolder holder, int position) {
        holder.bind(conversaciones.get(position));
    }

    @Override
    public int getItemCount() {
        return conversaciones.size();
    }

    class ConversacionViewHolder extends RecyclerView.ViewHolder {
        private final ItemHoteladminConversacionBinding binding;

        ConversacionViewHolder(ItemHoteladminConversacionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversacion conversacion) {
            binding.tvInicial.setText(conversacion.inicial);
            binding.tvNombre.setText(conversacion.clienteNombre);
            binding.tvHabitacion.setText(conversacion.habitacion);
            binding.tvMensaje.setText(conversacion.ultimoMensaje);
            binding.tvHora.setText(conversacion.hora);
            binding.tvNoLeidos.setText(String.valueOf(conversacion.noLeidos));
            binding.tvNoLeidos.setVisibility(conversacion.noLeidos > 0 ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> listener.onConversacionClick(conversacion));
        }
    }
}
