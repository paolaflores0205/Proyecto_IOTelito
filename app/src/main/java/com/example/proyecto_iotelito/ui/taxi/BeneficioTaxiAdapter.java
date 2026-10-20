package com.example.proyecto_iotelito.ui.taxi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.taxi.BeneficioTaxi;

import java.util.List;

public class BeneficioTaxiAdapter extends RecyclerView.Adapter<BeneficioTaxiAdapter.BeneficioViewHolder> {

    private final List<BeneficioTaxi> beneficios;

    public BeneficioTaxiAdapter(List<BeneficioTaxi> beneficios) {
        this.beneficios = beneficios;
    }

    @NonNull
    @Override
    public BeneficioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_beneficio_taxi, parent, false);
        return new BeneficioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BeneficioViewHolder holder, int position) {
        holder.bind(beneficios.get(position));
    }

    @Override
    public int getItemCount() {
        return beneficios.size();
    }

    static class BeneficioViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcono;
        private final TextView tvTitulo;

        BeneficioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.iv_icono_beneficio);
            tvTitulo = itemView.findViewById(R.id.tv_titulo_beneficio);
        }

        void bind(BeneficioTaxi beneficio) {
            ivIcono.setImageResource(beneficio.getIconResId());
            tvTitulo.setText(beneficio.getTitulo());
        }
    }
}
