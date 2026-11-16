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
    private java.time.LocalDate date = java.time.LocalDate.now(com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations.ZONE);
    private List<com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin> bookings = java.util.Collections.emptyList();
    private List<com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.Receipt> receipts = java.util.Collections.emptyList();
    private int rooms;
    private boolean bookingsReady, receiptsReady, roomsReady;
    private String bookingsError, receiptsError, roomsError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminReportesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository.remote(requireContext())) { iniciarRemoto(); return; }
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

    private String period() {
        int id = binding.chipGroupPeriodo.getCheckedChipId();
        return id == R.id.chip_diario ? "DAY" : id == R.id.chip_anual ? "YEAR" : "MONTH";
    }
    private void iniciarRemoto() {
        com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations repository =
                new com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations(requireContext());
        binding.deltaIngresos.setText("Cierres registrados; sin cargo bancario");
        binding.deltaReservas.setText("Llegadas del período");
        binding.deltaOcupacion.setText("Estimada con inventario actual");
        binding.deltaServicios.setText("Sin consumos registrados");
        binding.graphTitle.setText("Importes de cierres por tramo del período");
        binding.tvVerDetalle.setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Datos del reporte")
                .setMessage("Importes: comprobantes de checkout registrados en el período, no pagos bancarios.\nReservas: llegadas del período.\nOcupación estimada: noches reservadas / habitaciones actuales × días.\nLos servicios del catálogo no son ventas: aún no existe un registro de consumos remoto. Horarios de Lima.")
                .setPositiveButton("Entendido", null).show());
        binding.chipGroupPeriodo.setOnCheckedStateChangeListener((group, ids) -> renderRemote());
        binding.ivMesPrev.setOnClickListener(v -> move(-1));
        binding.ivMesNext.setOnClickListener(v -> move(1));
        repository.reservations().observe(getViewLifecycleOwner(), state -> {
            bookings = state.rows; bookingsReady = !state.loading && state.error == null; bookingsError = state.error; renderRemote();
        });
        repository.receipts().observe(getViewLifecycleOwner(), state -> {
            receipts = state.rows; receiptsReady = !state.loading && state.error == null; receiptsError = state.error; renderRemote();
        });
        new com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository(requireContext())
                .observe("rooms", com.example.proyecto_iotelito.model.hoteladmin.Habitacion.class).observe(getViewLifecycleOwner(), state -> {
                    rooms = state.data.size(); roomsReady = !state.loading && state.error == null; roomsError = state.error; renderRemote();
                });
        renderRemote();
    }
    private void move(int delta) {
        date = "DAY".equals(period()) ? date.plusDays(delta) : "YEAR".equals(period()) ? date.plusYears(delta) : date.plusMonths(delta);
        renderRemote();
    }
    private void renderRemote() {
        String kind = period();
        binding.tvMes.setText(date.format(java.time.format.DateTimeFormatter.ofPattern(
                "DAY".equals(kind) ? "dd MMM yyyy" : "YEAR".equals(kind) ? "yyyy" : "MMMM yyyy", new Locale("es", "PE"))));
        android.widget.TextView[] labels = {binding.labelSem1, binding.labelSem2, binding.labelSem3, binding.labelSem4};
        for (int i=0;i<4;i++) labels[i].setText("DAY".equals(kind) ? String.format(Locale.ROOT,"%02d–%02d h",i*6,i*6+6) : "Tramo " + (i+1));
        binding.llServicios.removeAllViews();
        android.widget.TextView info = new android.widget.TextView(requireContext());
        String error = bookingsError != null ? bookingsError : receiptsError != null ? receiptsError : roomsError;
        boolean ready = bookingsReady && receiptsReady && roomsReady;
        info.setText(error != null ? error : !ready ? "Consultando datos del hotel…" : "Sin consumos registrados. Los servicios del catálogo no representan ventas.");
        binding.llServicios.addView(info);
        binding.tvServicios.setText("—");
        if (!ready) {
            binding.tvIngresos.setText("—"); binding.tvReservas.setText("—"); binding.tvOcupacion.setText("—");
            pintarRemoteBarras(new double[4]); return;
        }
        com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.Report r =
                com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.report(bookings,receipts,rooms,date,kind);
        binding.tvIngresos.setText(String.format(Locale.US,"S/ %,.2f",r.income));
        binding.tvReservas.setText(String.valueOf(r.bookings));
        binding.tvOcupacion.setText(r.occupancy == null ? "—" : String.format(Locale.US,"%.1f%%",r.occupancy));
        pintarRemoteBarras(r.buckets);
    }
    private void pintarRemoteBarras(double[] values) {
        View[] bars = {binding.barSem1,binding.barSem2,binding.barSem3,binding.barSem4};
        double max = 1; for (double value : values) max = Math.max(max,value);
        for (int i=0;i<4;i++) {
            ViewGroup.LayoutParams params = bars[i].getLayoutParams(); params.height = dp((int)Math.round(96*values[i]/max));
            bars[i].setLayoutParams(params); bars[i].setContentDescription(String.format(Locale.US,"Tramo %d: S/ %.2f",i+1,values[i]));
        }
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
