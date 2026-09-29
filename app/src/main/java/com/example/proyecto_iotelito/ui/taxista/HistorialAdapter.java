package com.example.proyecto_iotelito.ui.taxista;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemHistorialCardBinding;
import com.example.proyecto_iotelito.model.taxista.ServicioHistorial;

import java.util.ArrayList;
import java.util.List;

/** Adapter de la lista de traslados pasados (pestaña Historial del taxista). */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    public interface OnServicioClickListener {
        void onServicioClick(ServicioHistorial servicio);
    }

    private final List<ServicioHistorial> servicios = new ArrayList<>();
    private final OnServicioClickListener listener;

    public HistorialAdapter(OnServicioClickListener listener) {
        this.listener = listener;
    }

    public void setServicios(List<ServicioHistorial> nuevos) {
        servicios.clear();
        servicios.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistorialCardBinding binding = ItemHistorialCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistorialViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        holder.bind(servicios.get(position));
    }

    @Override
    public int getItemCount() {
        return servicios.size();
    }

    class HistorialViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistorialCardBinding binding;

        HistorialViewHolder(ItemHistorialCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServicioHistorial servicio) {
            binding.tvHora.setText(servicio.hora);
            EstadoServicioUi.pintarPill(binding.tvEstadoPill, servicio.estado);
            binding.tvRuta.setText(servicio.ruta());
            binding.tvPasajero.setText(itemView.getContext().getString(
                    R.string.taxi_pasajero_vehiculo, servicio.pasajero, servicio.vehiculo));
            binding.tvDistanciaTiempo.setText(servicio.detalle());
            binding.tvMonto.setVisibility(servicio.fueCancelado() ? View.GONE : View.VISIBLE);
            binding.tvMonto.setText(itemView.getContext().getString(R.string.taxi_tarifa_monto, servicio.monto));
            binding.getRoot().setOnClickListener(v -> listener.onServicioClick(servicio));
        }
    }
}
