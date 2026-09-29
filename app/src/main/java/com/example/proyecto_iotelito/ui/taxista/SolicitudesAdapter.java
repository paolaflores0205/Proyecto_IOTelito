package com.example.proyecto_iotelito.ui.taxista;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemSolicitudCardBinding;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

import java.util.ArrayList;
import java.util.List;

/** Adapter de la lista de solicitudes disponibles (pestaña Solicitudes del taxista). */
public class SolicitudesAdapter extends RecyclerView.Adapter<SolicitudesAdapter.SolicitudViewHolder> {

    public interface OnSolicitudClickListener {
        void onSolicitudClick(SolicitudServicio solicitud);
    }

    private final List<SolicitudServicio> solicitudes = new ArrayList<>();
    private final OnSolicitudClickListener listener;

    public SolicitudesAdapter(OnSolicitudClickListener listener) {
        this.listener = listener;
    }

    public void setSolicitudes(List<SolicitudServicio> nuevas) {
        solicitudes.clear();
        solicitudes.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSolicitudCardBinding binding = ItemSolicitudCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SolicitudViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        holder.bind(solicitudes.get(position));
    }

    @Override
    public int getItemCount() {
        return solicitudes.size();
    }

    class SolicitudViewHolder extends RecyclerView.ViewHolder {
        private final ItemSolicitudCardBinding binding;

        SolicitudViewHolder(ItemSolicitudCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SolicitudServicio solicitud) {
            binding.tvTiempoPill.setText(solicitud.minutosEspera < 5
                    ? itemView.getContext().getString(R.string.taxi_tag_menos_5_min)
                    : itemView.getContext().getString(R.string.taxi_pill_minutos, solicitud.minutosEspera));
            binding.tvTarifa.setText(itemView.getContext().getString(R.string.taxi_tarifa_monto, solicitud.tarifa));
            binding.tvHotelNombre.setText(solicitud.hotel);
            binding.tvDestino.setText(solicitud.destino);
            binding.tvFechaHora.setText(solicitud.fechaHora);
            binding.btnVerSolicitud.setOnClickListener(v -> listener.onSolicitudClick(solicitud));
        }
    }
}
