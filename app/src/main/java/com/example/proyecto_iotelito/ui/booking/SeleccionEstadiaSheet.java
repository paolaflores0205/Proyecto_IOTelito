package com.example.proyecto_iotelito.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.example.proyecto_iotelito.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Selección temporal: cancelar nunca modifica el hotel ni el detalle que quedó detrás. */
public class SeleccionEstadiaSheet extends BottomSheetDialogFragment {
    public static final String RESULT = "seleccion_estadia_hotel";
    private long entrada, salida;
    private int adultos = 2, ninos;
    private boolean fechasElegidas;
    private View content;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        long hoy = LocalDate.now(ZoneId.of("America/Lima")).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Bundle source = state != null ? state : getArguments();
        entrada = source == null ? hoy : source.getLong("entrada", hoy);
        salida = source == null ? hoy + 86400000L : source.getLong("salida", hoy + 86400000L);
        adultos = source == null ? 2 : source.getInt("adultos", 2);
        ninos = source == null ? 0 : source.getInt("ninos", 0);
        fechasElegidas = source != null && source.getBoolean("fechas");
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.sheet_estadia_hotel, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        content = view;
        ((TextView)view.findViewById(R.id.tv_hotel_estadia)).setText(requireArguments().getString("hotel"));
        view.findViewById(R.id.btn_fechas_estadia).setOnClickListener(v -> elegirFechas());
        configurarPaso(R.id.btn_menos_adultos, -1, true);
        configurarPaso(R.id.btn_mas_adultos, 1, true);
        configurarPaso(R.id.btn_menos_ninos, -1, false);
        configurarPaso(R.id.btn_mas_ninos, 1, false);
        view.findViewById(R.id.btn_cancelar_estadia).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_aplicar_estadia).setOnClickListener(v -> {
            if (!fechasElegidas || salida <= entrada) return;
            Bundle result = new Bundle();
            result.putLong("entrada", entrada); result.putLong("salida", salida);
            result.putInt("adultos", adultos); result.putInt("ninos", ninos);
            getParentFragmentManager().setFragmentResult(RESULT, result);
            dismiss();
        });
        actualizar();
    }
    private void configurarPaso(int id, int delta, boolean esAdulto) {
        content.findViewById(id).setOnClickListener(v -> {
            if (esAdulto) adultos = Math.max(1, Math.min(10, adultos + delta));
            else ninos = Math.max(0, Math.min(10, ninos + delta));
            actualizar();
        });
    }
    private void actualizar() {
        if (content == null) return;
        ((TextView)content.findViewById(R.id.tv_adultos)).setText(String.valueOf(adultos));
        ((TextView)content.findViewById(R.id.tv_ninos)).setText(String.valueOf(ninos));
        content.findViewById(R.id.btn_menos_adultos).setEnabled(adultos > 1);
        content.findViewById(R.id.btn_mas_adultos).setEnabled(adultos < 10);
        content.findViewById(R.id.btn_menos_ninos).setEnabled(ninos > 0);
        content.findViewById(R.id.btn_mas_ninos).setEnabled(ninos < 10);
        if (fechasElegidas) ((TextView)content.findViewById(R.id.btn_fechas_estadia)).setText(
                new BookingSelection(entrada, salida, adultos, ninos).textoFechas());
        else ((TextView)content.findViewById(R.id.btn_fechas_estadia)).setText(R.string.elegir_fechas_reservar);
        content.findViewById(R.id.btn_aplicar_estadia).setEnabled(fechasElegidas && salida > entrada);
    }
    private void elegirFechas() {
        if (getChildFragmentManager().findFragmentByTag("rango_estadia") != null) return;
        long hoy = LocalDate.now(ZoneId.of("America/Lima")).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.fechas_estadia_hotel)
                .setSelection(new Pair<>(entrada, salida))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(hoy)).build()).build();
        escuchar(picker);
        picker.show(getChildFragmentManager(), "rango_estadia");
    }
    private void escuchar(MaterialDatePicker<Pair<Long, Long>> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(range -> {
            if (range.first == null || range.second == null || range.second <= range.first) {
                fechasElegidas = false;
                actualizar();
                if (content != null) {
                    TextView error = content.findViewById(R.id.tv_error_estadia);
                    error.setText(R.string.estadia_minima_noche); error.setVisibility(View.VISIBLE);
                }
                return;
            }
            entrada = range.first; salida = range.second; fechasElegidas = true;
            if (content != null) content.findViewById(R.id.tv_error_estadia).setVisibility(View.GONE);
            actualizar();
        });
    }
    @Override public void onStart() {
        super.onStart();
        if (getDialog() instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                    ((com.google.android.material.bottomsheet.BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        }
        androidx.fragment.app.Fragment restored = getChildFragmentManager().findFragmentByTag("rango_estadia");
        if (restored instanceof MaterialDatePicker) escuchar((MaterialDatePicker<Pair<Long, Long>>) restored);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("entrada", entrada); state.putLong("salida", salida);
        state.putInt("adultos", adultos); state.putInt("ninos", ninos); state.putBoolean("fechas", fechasElegidas);
    }
    @Override public void onDestroyView() { content = null; super.onDestroyView(); }
}
