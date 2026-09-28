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
import com.example.proyecto_iotelito.databinding.FragmentSolicitudesTaxistaBinding;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña "Solicitudes": servicios disponibles cerca del conductor, en un
 * RecyclerView alimentado por {@link TaxistaSampleData}. Los chips filtran u ordenan la lista.
 */
public class SolicitudesTaxistaFragment extends Fragment {

    private FragmentSolicitudesTaxistaBinding binding;
    private SolicitudesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentSolicitudesTaxistaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new SolicitudesAdapter(this::abrirDetalle);
        binding.rvSolicitudes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSolicitudes.setAdapter(adapter);

        binding.chipTodas.setChecked(true);
        binding.chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> renderSolicitudes());
        renderSolicitudes();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver del detalle, la solicitud aceptada ya no debe aparecer.
        if (binding != null) {
            renderSolicitudes();
        }
    }

    private void renderSolicitudes() {
        int chipActivo = binding.chipGroupFiltros.getCheckedChipId();
        List<SolicitudServicio> filtradas = new ArrayList<>();
        for (SolicitudServicio solicitud : TaxistaSampleData.solicitudes()) {
            if (chipActivo == R.id.chip_menos_10_min && solicitud.minutosEspera >= 10) continue;
            filtradas.add(solicitud);
        }
        if (chipActivo == R.id.chip_mayor_tarifa) {
            filtradas.sort((a, b) -> Double.compare(b.tarifa, a.tarifa));
        }
        adapter.setSolicitudes(filtradas);
        binding.tvSolicitudesSubtitulo.setText(getString(R.string.taxi_solicitudes_subtitulo, filtradas.size()));
        binding.tvSolicitudesVacio.setVisibility(filtradas.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvSolicitudes.setVisibility(filtradas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void abrirDetalle(SolicitudServicio solicitud) {
        startActivity(DetalleServicioActivity.createIntent(requireContext(), solicitud.id));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
