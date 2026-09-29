package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemHoteladminServicioBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Une los servicios de ejemplo con las tarjetas existentes. */
public class ServiciosAdapter extends RecyclerView.Adapter<ServiciosAdapter.ServicioViewHolder> {

    public interface OnServicioActionListener {
        void onEditar(Servicio servicio);
        void onEliminar(Servicio servicio);
    }

    private final List<Servicio> servicios = new ArrayList<>();
    private final OnServicioActionListener listener;

    public ServiciosAdapter(OnServicioActionListener listener) {
        this.listener = listener;
    }

    public void setServicios(List<Servicio> nuevos) {
        servicios.clear();
        servicios.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHoteladminServicioBinding binding = ItemHoteladminServicioBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServicioViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServicioViewHolder holder, int position) {
        holder.bind(servicios.get(position));
    }

    @Override
    public int getItemCount() {
        return servicios.size();
    }

    class ServicioViewHolder extends RecyclerView.ViewHolder {
        private final ItemHoteladminServicioBinding binding;

        ServicioViewHolder(ItemHoteladminServicioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Servicio servicio) {
            binding.ivFoto.setImageResource(servicio.fotoRes);
            binding.tvNombre.setText(servicio.nombre);
            binding.tvDescripcion.setText(servicio.descripcion);
            binding.tvPrecio.setText(itemView.getContext().getString(R.string.hoteladmin_moneda,
                    String.format(Locale.US, "%,.0f", servicio.precio)));
            binding.tvPrecioSuffix.setText(itemView.getContext().getString(
                    R.string.hoteladmin_precio_unidad, servicio.unidad));
            binding.tvBadge.setText(servicio.activo
                    ? R.string.hoteladmin_badge_activo : R.string.hoteladmin_badge_inactivo);
            binding.tvBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    itemView.getContext(), servicio.activo ? R.color.io_success_bg : R.color.io_tag_bg)));
            binding.tvBadge.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    servicio.activo ? R.color.io_success_text : R.color.io_text_secondary));
            binding.tvEditar.setOnClickListener(v -> listener.onEditar(servicio));
            binding.tvEliminar.setOnClickListener(v -> listener.onEliminar(servicio));
        }
    }
}
