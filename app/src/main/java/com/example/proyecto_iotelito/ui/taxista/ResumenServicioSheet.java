package com.example.proyecto_iotelito.ui.taxista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxistaSampleData;
import com.example.proyecto_iotelito.databinding.ItemSummaryRowBinding;
import com.example.proyecto_iotelito.databinding.SheetResumenServicioTaxistaBinding;
import com.example.proyecto_iotelito.model.taxista.ServicioHistorial;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Resumen de un traslado del historial, con el mismo contenido que la pantalla
 * "Servicio finalizado": pasajero, ruta, distancia, tiempo y tarifa. Si el
 * servicio fue cancelado muestra el motivo en lugar de distancia, tiempo y tarifa.
 */
public class ResumenServicioSheet extends BottomSheetDialogFragment {

    public static final String TAG = "resumen_servicio_sheet";
    private static final String ARG_SERVICIO_ID = "arg_servicio_id";

    private SheetResumenServicioTaxistaBinding binding;

    public static ResumenServicioSheet newInstance(int servicioId) {
        ResumenServicioSheet sheet = new ResumenServicioSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_SERVICIO_ID, servicioId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = SheetResumenServicioTaxistaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ServicioHistorial servicio = TaxistaSampleData.historialPorId(
                requireArguments().getInt(ARG_SERVICIO_ID, -1));
        if (servicio == null) {
            dismiss();
            return;
        }

        EstadoServicioUi.pintarPill(binding.tvEstadoPill, servicio.estado);

        binding.rowPasajero.tvAvatarInicial.setText(iniciales(servicio.pasajero));
        binding.rowPasajero.tvPasajeroNombre.setText(servicio.pasajero);
        binding.rowPasajero.tvPasajeroDetalle.setText(R.string.taxi_pasajero_huesped);
        binding.rowPasajero.ivChat.setVisibility(View.GONE);
        binding.rowPasajero.ivLlamar.setVisibility(View.GONE);

        fila(binding.rowHora, R.string.taxi_label_hora, servicio.hora);
        fila(binding.rowOrigen, R.string.taxi_label_origen, servicio.origen);
        fila(binding.rowDestino, R.string.taxi_label_destino, servicio.destino);

        boolean cancelado = servicio.fueCancelado();
        binding.rowDistancia.getRoot().setVisibility(cancelado ? View.GONE : View.VISIBLE);
        binding.rowTiempo.getRoot().setVisibility(cancelado ? View.GONE : View.VISIBLE);
        binding.rowTarifa.getRoot().setVisibility(cancelado ? View.GONE : View.VISIBLE);
        binding.rowPagadoPor.getRoot().setVisibility(cancelado ? View.GONE : View.VISIBLE);
        binding.rowMotivo.getRoot().setVisibility(cancelado ? View.VISIBLE : View.GONE);

        if (cancelado) {
            fila(binding.rowMotivo, R.string.taxi_label_motivo, servicio.motivoCancelacion);
        } else {
            fila(binding.rowDistancia, R.string.label_distancia, servicio.distancia);
            fila(binding.rowTiempo, R.string.label_tiempo, servicio.duracion);
            fila(binding.rowTarifa, R.string.taxi_label_tarifa,
                    getString(R.string.taxi_tarifa_monto, servicio.monto));
            fila(binding.rowPagadoPor, R.string.taxi_label_pagado_por, getString(R.string.taxi_valor_hotel));
        }

        binding.btnCerrar.setOnClickListener(v -> dismiss());
    }

    private void fila(ItemSummaryRowBinding fila, int etiquetaRes, String valor) {
        fila.tvLabel.setText(etiquetaRes);
        fila.tvValue.setText(valor);
    }

    private String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
