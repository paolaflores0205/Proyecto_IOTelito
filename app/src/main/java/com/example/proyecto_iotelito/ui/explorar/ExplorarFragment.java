package com.example.proyecto_iotelito.ui.explorar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.ui.booking.DetalleHotelActivity;
import com.example.proyecto_iotelito.ui.booking.ResultadosActivity;
import com.example.proyecto_iotelito.ui.hoteles.ListaHotelesActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pestaña "Explorar": panel de búsqueda + hoteles recomendados.
 * Punto de entrada del flujo Explorar -> Resultados -> Detalle -> Reserva.
 */
public class ExplorarFragment extends Fragment {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Locale LOCALE_PE = new Locale.Builder().setLanguage("es").setRegion("PE").build();
    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    private static final int MIN_ADULTOS = 1;
    private static final int MAX_ADULTOS = 10;
    private static final int MIN_NINOS = 0;
    private static final int MAX_NINOS = 10;

    private EditText etDestino;
    private TextView tvFechaEntrada;
    private TextView tvFechaSalida;
    private TextView tvHuespedes;

    private long fechaEntradaMillis;
    private long fechaSalidaMillis;
    private int adultos = 2;
    private int ninos = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvSaludo = view.findViewById(R.id.tv_saludo);
        tvSaludo.setText(getString(R.string.saludo, SampleData.PERFIL.firstName()));

        etDestino = view.findViewById(R.id.et_destino);
        etDestino.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                abrirResultados();
                return true;
            }
            return false;
        });

        inicializarFechasPorDefecto();
        tvFechaEntrada = view.findViewById(R.id.tv_fecha_entrada);
        tvFechaSalida = view.findViewById(R.id.tv_fecha_salida);
        actualizarTextosFecha();

        view.findViewById(R.id.row_fecha_entrada).setOnClickListener(v -> mostrarSelectorFecha(true));
        view.findViewById(R.id.row_fecha_salida).setOnClickListener(v -> mostrarSelectorFecha(false));

        tvHuespedes = view.findViewById(R.id.tv_huespedes);
        actualizarTextoHuespedes();
        view.findViewById(R.id.row_huespedes).setOnClickListener(v -> mostrarSelectorHuespedes());

        MaterialButton btnBuscar = view.findViewById(R.id.btn_buscar_hoteles);
        btnBuscar.setOnClickListener(v -> abrirResultados());

        bindHotelCard(view.findViewById(R.id.card_hotel_0), SampleData.HOTELS.get(0));
        bindHotelCard(view.findViewById(R.id.card_hotel_1), SampleData.HOTELS.get(1));

        view.findViewById(R.id.tv_ver_todos).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ListaHotelesActivity.class)));
    }

    private void inicializarFechasPorDefecto() {
        Calendar cal = Calendar.getInstance(UTC);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fechaEntradaMillis = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 3);
        fechaSalidaMillis = cal.getTimeInMillis();
    }

    /**
     * Abre un MaterialDatePicker restringido para que la fecha de entrada nunca
     * pueda quedar después de la de salida (y viceversa), además de bloquear
     * fechas pasadas.
     */
    private void mostrarSelectorFecha(boolean esEntrada) {
        List<CalendarConstraints.DateValidator> validadores = new ArrayList<>();
        validadores.add(DateValidatorPointForward.now());
        if (esEntrada) {
            validadores.add(DateValidatorPointBackward.before(fechaSalidaMillis));
        } else {
            validadores.add(DateValidatorPointForward.from(fechaEntradaMillis));
        }

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(CompositeDateValidator.allOf(validadores))
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(esEntrada ? R.string.titulo_selector_fecha_entrada : R.string.titulo_selector_fecha_salida)
                .setSelection(esEntrada ? fechaEntradaMillis : fechaSalidaMillis)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(seleccionMillis -> {
            if (esEntrada) {
                fechaEntradaMillis = seleccionMillis;
                if (fechaEntradaMillis > fechaSalidaMillis) {
                    fechaSalidaMillis = fechaEntradaMillis + ONE_DAY_MS;
                }
            } else {
                fechaSalidaMillis = seleccionMillis;
            }
            actualizarTextosFecha();
        });

        picker.show(getParentFragmentManager(), "date_picker");
    }

    private void actualizarTextosFecha() {
        SimpleDateFormat formato = new SimpleDateFormat("d MMM", LOCALE_PE);
        formato.setTimeZone(UTC);
        tvFechaEntrada.setText(formato.format(fechaEntradaMillis));
        tvFechaSalida.setText(formato.format(fechaSalidaMillis));
    }

    private String formatearRangoFechas() {
        SimpleDateFormat formato = new SimpleDateFormat("d MMM", LOCALE_PE);
        formato.setTimeZone(UTC);
        return formato.format(fechaEntradaMillis) + " – " + formato.format(fechaSalidaMillis);
    }

    /**
     * Diálogo con steppers de Adultos/Niños (Clase 3.2 - Dialogs). Los cambios
     * se aplican sobre copias locales y solo se confirman al pulsar "Aplicar".
     */
    private void mostrarSelectorHuespedes() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_huespedes, null);

        TextView tvAdultos = dialogView.findViewById(R.id.tv_adultos);
        TextView tvNinos = dialogView.findViewById(R.id.tv_ninos);
        ImageView btnMenosAdultos = dialogView.findViewById(R.id.btn_menos_adultos);
        ImageView btnMasAdultos = dialogView.findViewById(R.id.btn_mas_adultos);
        ImageView btnMenosNinos = dialogView.findViewById(R.id.btn_menos_ninos);
        ImageView btnMasNinos = dialogView.findViewById(R.id.btn_mas_ninos);

        int[] adultosTemp = {adultos};
        int[] ninosTemp = {ninos};

        Runnable actualizarVista = () -> {
            tvAdultos.setText(String.valueOf(adultosTemp[0]));
            tvNinos.setText(String.valueOf(ninosTemp[0]));
            actualizarEstadoStepper(btnMenosAdultos, adultosTemp[0] > MIN_ADULTOS);
            actualizarEstadoStepper(btnMasAdultos, adultosTemp[0] < MAX_ADULTOS);
            actualizarEstadoStepper(btnMenosNinos, ninosTemp[0] > MIN_NINOS);
            actualizarEstadoStepper(btnMasNinos, ninosTemp[0] < MAX_NINOS);
        };
        actualizarVista.run();

        btnMenosAdultos.setOnClickListener(v -> {
            if (adultosTemp[0] > MIN_ADULTOS) {
                adultosTemp[0]--;
                actualizarVista.run();
            }
        });
        btnMasAdultos.setOnClickListener(v -> {
            if (adultosTemp[0] < MAX_ADULTOS) {
                adultosTemp[0]++;
                actualizarVista.run();
            }
        });
        btnMenosNinos.setOnClickListener(v -> {
            if (ninosTemp[0] > MIN_NINOS) {
                ninosTemp[0]--;
                actualizarVista.run();
            }
        });
        btnMasNinos.setOnClickListener(v -> {
            if (ninosTemp[0] < MAX_NINOS) {
                ninosTemp[0]++;
                actualizarVista.run();
            }
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_aplicar, (dialog, which) -> {
                    adultos = adultosTemp[0];
                    ninos = ninosTemp[0];
                    actualizarTextoHuespedes();
                })
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void actualizarEstadoStepper(ImageView boton, boolean habilitado) {
        boton.setEnabled(habilitado);
        boton.setAlpha(habilitado ? 1f : 0.35f);
    }

    private void actualizarTextoHuespedes() {
        tvHuespedes.setText(formatearHuespedes());
    }

    private String formatearHuespedes() {
        String textoAdultos = getResources().getQuantityString(R.plurals.adultos_plural, adultos, adultos);
        if (ninos <= 0) {
            return textoAdultos;
        }
        String textoNinos = getResources().getQuantityString(R.plurals.ninos_plural, ninos, ninos);
        return textoAdultos + ", " + textoNinos;
    }

    private void abrirResultados() {
        String destino = etDestino.getText().toString().trim();
        if (TextUtils.isEmpty(destino)) {
            destino = "Lima, Perú";
        }
        int totalHuespedes = adultos + ninos;
        String resumen = formatearRangoFechas() + " · "
                + getResources().getQuantityString(R.plurals.huespedes_total_plural, totalHuespedes, totalHuespedes);

        Intent intent = new Intent(getActivity(), ResultadosActivity.class);
        intent.putExtra(ResultadosActivity.EXTRA_DESTINO, destino);
        intent.putExtra(ResultadosActivity.EXTRA_RESUMEN, resumen);
        startActivity(intent);
    }

    private void bindHotelCard(View cardRoot, Hotel hotel) {
        TextView tvCity = cardRoot.findViewById(R.id.tv_city);
        TextView tvRating = cardRoot.findViewById(R.id.tv_rating);
        TextView tvName = cardRoot.findViewById(R.id.tv_name);
        TextView tvPrice = cardRoot.findViewById(R.id.tv_price);

        tvCity.setText(hotel.city);
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
        tvName.setText(hotel.name);
        tvPrice.setText(getString(R.string.precio_por_noche,
                String.format(Locale.getDefault(), "S/ %.0f", hotel.pricePerNight)));

        cardRoot.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetalleHotelActivity.class);
            intent.putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }
}
