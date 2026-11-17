package com.example.proyecto_iotelito.ui.superadmin.reportes;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminReportesBinding;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.model.superadmin.HotelCheckout;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportesFragment extends Fragment {
    private FragmentSuperadminReportesBinding binding;
    private List<ManagedHotel> hotels = new ArrayList<>();
    private List<Reserva> reservations = new ArrayList<>();
    private List<HotelCheckout> checkouts = new ArrayList<>();
    private boolean checkoutsReady, hotelsReady;
    private String checkoutStatus = "Consultando comprobantes…";
    private String remoteStatus = "Cargando reservas…";
    private boolean reservationsReady;
    private String catalogStatus = "";
    private int selectedHotel;
    private LocalDate date = LocalDate.now(ZoneId.of("America/Lima"));
    private final Locale locale = Locale.forLanguageTag("es-PE");
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentSuperadminReportesBinding.inflate(inflater, container, false);
        if (state != null) {
            selectedHotel = state.getInt("report_hotel");
            date = LocalDate.parse(state.getString("report_date", LocalDate.now().toString()));
        }
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        binding.actvHotelSelector.setOnItemClickListener((parent, v, position, id) -> {
            selectedHotel = position == 0 ? 0 : hotels.get(position - 1).id;
            render();
        });
        binding.chipGroupPeriod.setOnCheckedStateChangeListener((group, ids) -> render());
        binding.btnReportDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Fecha del reporte")
                    .setSelection(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()).build();
            picker.addOnPositiveButtonClickListener(value -> {
                date = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate();
                render();
            });
            picker.show(getChildFragmentManager(), "report_date_picker");
        });
        SuperadminViewModel model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.hotelsState().observe(getViewLifecycleOwner(), stateRows -> {
            hotels = stateRows.hotels;
            hotelsReady = !stateRows.loading && stateRows.error == null;
            catalogStatus = stateRows.error != null ? " · " + stateRows.error : stateRows.loading ? " · Cargando hoteles…" : "";
            List<String> names = new ArrayList<>();
            names.add(getString(R.string.superadmin_all_hotels));
            String selection = names.get(0);
            boolean found = selectedHotel == 0;
            for (ManagedHotel hotel : hotels) {
                names.add(hotel.name);
                if (hotel.id == selectedHotel) { selection = hotel.name; found = true; }
            }
            if (hotelsReady && !found) selectedHotel = 0;
            binding.actvHotelSelector.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));
            binding.actvHotelSelector.setText(selection, false);
            binding.actvHotelSelector.setEnabled(hotelsReady);
            render();
        });
        model.reservationsState().observe(getViewLifecycleOwner(), stateRows -> {
            reservations = stateRows.rows;
            reservationsReady = !stateRows.loading && stateRows.error == null;
            remoteStatus = stateRows.error != null ? stateRows.error : "Cargando reservas…";
            render();
        });
        model.checkoutsState().observe(getViewLifecycleOwner(), stateRows -> {
            checkouts = stateRows.rows;
            checkoutsReady = !stateRows.loading && stateRows.error == null;
            checkoutStatus = stateRows.error != null ? stateRows.error : stateRows.loading
                    ? "Consultando comprobantes…" : "Comprobantes por fecha de cierre de estadía";
            render();
        });
        render();
    }
    private void render() {
        if (binding == null) return;
        int id = binding.chipGroupPeriod.getCheckedChipId();
        ReportCalculator.Period period = id == R.id.chip_daily ? ReportCalculator.Period.DAILY
                : id == R.id.chip_yearly ? ReportCalculator.Period.YEARLY : ReportCalculator.Period.MONTHLY;
        List<Reserva> source = new ArrayList<>(reservations);
        ReportCalculator.Result data = ReportCalculator.calculate(hotels, source, checkouts, selectedHotel, period, date);
        boolean ready = reservationsReady && hotelsReady;
        binding.tvReportSource.setText((reservationsReady
                ? getResources().getQuantityString(R.plurals.superadmin_report_bookings, data.bookings, data.bookings)
                : remoteStatus) + catalogStatus);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", locale);
        binding.tvSelectedRange.setText(data.start.format(formatter) + " — " + data.end.minusDays(1).format(formatter));
        binding.btnReportDate.setText("Cambiar fecha: " + date.format(formatter));
        binding.tvBookings.setText(ready ? String.valueOf(data.bookings) : "—");
        binding.tvBookingsNote.setText("Incluye canceladas; por fecha de llegada");
        binding.tvCancellations.setText(ready ? String.valueOf(data.cancellations) : "—");
        binding.tvCancellationsNote.setText(ready ? String.format(locale, "%.1f%% de las llegadas", data.bookings == 0 ? 0 : data.cancellations * 100.0 / data.bookings) : "Esperando datos confirmados");
        binding.tvOccupancy.setText(ready ? String.format(locale, "%.1f%%", data.occupancy) : "—");
        binding.tvOccupancyNote.setText("Estimación según habitaciones reservadas");
        binding.tvIncome.setText(ready ? String.format(locale, "S/ %,.2f", data.amount) : "—");
        binding.tvIncomeNote.setText("Valor de las estadías en el período");
        boolean receiptsReady = checkoutsReady && hotelsReady;
        binding.tvCheckoutIncome.setText(receiptsReady ? String.format(locale, "S/ %,.2f", data.checkoutAmount) : "—");
        binding.tvCheckoutExtras.setText(receiptsReady ? String.format(locale, "S/ %,.2f", data.checkoutExtras) : "—");
        binding.tvCheckoutCount.setText(receiptsReady ? "Comprobantes: " + data.checkouts : "Esperando datos confirmados");
        binding.tvCheckoutStatus.setText(checkoutStatus);
        binding.tvChartTitle.setText(data.title);
        binding.weeklyBookingsChart.setData(data.labels, data.values);
        binding.weeklyBookingsChart.setVisibility(ready ? View.VISIBLE : View.INVISIBLE);
        binding.weeklyBookingsChart.setContentDescription(data.title + ": " + Arrays.toString(data.values));
        binding.weeklyBookingsChart.setMinimumWidth((int) (getResources().getDisplayMetrics().density * (period == ReportCalculator.Period.YEARLY ? 650 : 280)));
        binding.tvReportEmpty.setVisibility(ready && data.bookings == 0 && data.amount == 0 ? View.VISIBLE : View.GONE);
        binding.rankingContainer.removeAllViews();
        if (!ready) return;
        for (ReportCalculator.Ranking rank : data.ranking) {
            TextView label = new TextView(requireContext());
            label.setText(rank.hotel.name + " · " + (rank.hotel.rooms == 0 ? "Ocupación no calculable: registra habitaciones"
                    : String.format(locale, "%.1f%% de ocupación en el período", rank.occupancy)));
            label.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.io_navy));
            label.setPadding(0, 16, 0, 6);
            binding.rankingContainer.addView(label);
            LinearProgressIndicator bar = new LinearProgressIndicator(requireContext());
            bar.setMax(1000);
            bar.setProgress((int) Math.round(rank.occupancy * 10));
            bar.setIndicatorColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.io_teal));
            binding.rankingContainer.addView(bar, new LinearLayout.LayoutParams(-1, 12));
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("report_hotel", selectedHotel);
        out.putString("report_date", date.toString());
    }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
