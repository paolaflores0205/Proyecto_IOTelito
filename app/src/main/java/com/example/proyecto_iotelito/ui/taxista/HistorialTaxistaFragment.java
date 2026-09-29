package com.example.proyecto_iotelito.ui.taxista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxistaSampleData;
import com.example.proyecto_iotelito.databinding.FragmentHistorialTaxistaBinding;
import com.example.proyecto_iotelito.model.taxista.ServicioHistorial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña "Historial": traslados pasados del conductor en un RecyclerView,
 * filtrados por pestañas (Todos / Completados / Cancelados).
 */
public class HistorialTaxistaFragment extends Fragment {

    private FragmentHistorialTaxistaBinding binding;
    private HistorialAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentHistorialTaxistaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new HistorialAdapter(servicio -> ResumenServicioSheet.newInstance(servicio.id)
                .show(getChildFragmentManager(), ResumenServicioSheet.TAG));
        binding.rvHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistorial.setAdapter(adapter);

        actualizarResumen();
        binding.tabLayoutHistorial.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filtrarPorPestana(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        filtrarPorPestana(binding.tabLayoutHistorial.getSelectedTabPosition());
    }

    /** Contadores de las pestañas y resumen de servicios/cuota, calculados desde la data. */
    private void actualizarResumen() {
        int completados = 0;
        int cancelados = 0;
        double cuota = 0;
        for (ServicioHistorial servicio : TaxistaSampleData.historial()) {
            if (servicio.estado == EstadoServicio.FINALIZADO) {
                completados++;
                cuota += servicio.monto;
            } else if (servicio.estado == EstadoServicio.CANCELADO) {
                cancelados++;
            }
        }
        binding.tabLayoutHistorial.getTabAt(0).setText(
                getString(R.string.taxi_tab_todos, TaxistaSampleData.historial().size()));
        binding.tabLayoutHistorial.getTabAt(1).setText(getString(R.string.taxi_tab_completados, completados));
        binding.tabLayoutHistorial.getTabAt(2).setText(getString(R.string.taxi_tab_cancelados, cancelados));
        binding.tvStatCompletados.setText(getString(R.string.taxi_stat_completados, completados));
        binding.tvStatCuota.setText(getString(R.string.taxi_stat_cuota, cuota));
    }

    private void filtrarPorPestana(int posicion) {
        List<ServicioHistorial> filtrados = new ArrayList<>();
        for (ServicioHistorial servicio : TaxistaSampleData.historial()) {
            if (posicion == 0
                    || (posicion == 1 && servicio.estado == EstadoServicio.FINALIZADO)
                    || (posicion == 2 && servicio.estado == EstadoServicio.CANCELADO)) {
                filtrados.add(servicio);
            }
        }
        adapter.setServicios(filtrados);
        binding.tvHistorialVacio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvHistorial.setVisibility(filtrados.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
