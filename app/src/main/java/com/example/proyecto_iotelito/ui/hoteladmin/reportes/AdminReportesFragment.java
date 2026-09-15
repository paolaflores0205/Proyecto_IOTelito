package com.example.proyecto_iotelito.ui.hoteladmin.reportes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentHoteladminReportesBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminReporteServicioBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ItemConsumo;

import java.util.List;
import java.util.Locale;

/**
 * Pestaña "Reportes" del panel de Administrador de hotel (adm-reportes): KPIs del
 * mes, gráfico de barras con los ingresos semanales e ingresos por servicio
 * adicional ordenados de menor a mayor. Datos estáticos de {@link HotelAdminSampleData}.
 */
public class AdminReportesFragment extends Fragment {

    private FragmentHoteladminReportesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminReportesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HotelAdminSampleData.Reporte r = HotelAdminSampleData.reporte();

        binding.tvIngresos.setText(getString(R.string.hoteladmin_moneda,
                String.format(Locale.US, "%,.0f", r.ingresos)));
        binding.tvReservas.setText(String.valueOf(r.reservas));
        binding.tvOcupacion.setText(r.ocupacionPct + "%");
        binding.tvServicios.setText(String.valueOf(r.serviciosVendidos));

        pintarBarras(r.ingresosSemanales);
        pintarServicios(r.ingresosPorServicio);

        // El periodo y el mes son ilustrativos: esta demo solo tiene datos mensuales.
        binding.chipGroupPeriodo.setOnCheckedStateChangeListener((group, ids) -> avisoDemo());
        binding.ivMesPrev.setOnClickListener(v -> avisoDemo());
        binding.ivMesNext.setOnClickListener(v -> avisoDemo());
        binding.tvVerDetalle.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show());
    }

    /** Ajusta la altura de las barras proporcionalmente al mayor ingreso semanal. */
    private void pintarBarras(int[] semanas) {
        View[] barras = {binding.barSem1, binding.barSem2, binding.barSem3, binding.barSem4};
        int max = 1;
        for (int valor : semanas) max = Math.max(max, valor);
        for (int i = 0; i < barras.length && i < semanas.length; i++) {
            int alturaDp = 24 + Math.round(72f * semanas[i] / max); // entre 24dp y 96dp
            ViewGroup.LayoutParams params = barras[i].getLayoutParams();
            params.height = dp(alturaDp);
            barras[i].setLayoutParams(params);
        }
    }

    private void pintarServicios(List<ItemConsumo> servicios) {
        binding.llServicios.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < servicios.size(); i++) {
            ItemConsumo servicio = servicios.get(i);
            ItemHoteladminReporteServicioBinding item = ItemHoteladminReporteServicioBinding.inflate(
                    inflater, binding.llServicios, false);
            item.tvNombre.setText(getString(R.string.hoteladmin_reportes_servicio_rank,
                    i + 1, servicio.concepto));
            item.tvMonto.setText(getString(R.string.hoteladmin_moneda,
                    String.format(Locale.US, "%,.0f", servicio.monto)));
            // El servicio con mayor ingreso (el último, por el orden ascendente) se resalta.
            if (i == servicios.size() - 1) {
                item.tvMonto.setTextColor(ContextCompat.getColor(requireContext(), R.color.io_teal));
            }
            binding.llServicios.addView(item.getRoot());
        }
    }

    private void avisoDemo() {
        Toast.makeText(requireContext(), R.string.hoteladmin_reportes_demo, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
