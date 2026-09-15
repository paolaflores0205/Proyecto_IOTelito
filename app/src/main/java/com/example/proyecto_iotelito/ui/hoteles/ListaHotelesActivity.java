package com.example.proyecto_iotelito.ui.hoteles;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.util.ServicioIconos;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Listado completo de hoteles con buscador y un panel de filtros interactivo
 * (bottom sheet de Material Design), accesible desde "Ver todos" en Explorar.
 * Ciudad/servicios se eligen con Chips, la calificación con estrellas
 * tocables y el precio con un RangeSlider (Clase 3.2 - Elementos de UI).
 */
public class ListaHotelesActivity extends AppCompatActivity {

    private static final int SIN_ORDEN = 0;
    private static final int ORDEN_MENOR_A_MAYOR = 1;
    private static final int ORDEN_MAYOR_A_MENOR = 2;

    private final List<Hotel> todosLosHoteles = new ArrayList<>(SampleData.HOTELS);

    private EditText etBuscar;
    private MaterialButton btnFiltros;
    private TextView tvLimpiarFiltros;
    private TextView tvConteo;
    private LinearLayout listContainer;
    private View vacioView;

    private double dominioPrecioMin;
    private double dominioPrecioMax;

    private String consulta = "";
    private String ciudadSeleccionada = null;
    private int calificacionMinima = 0;
    private double precioMinimo;
    private double precioMaximo;
    private final Set<String> serviciosSeleccionados = new LinkedHashSet<>();
    private int modoOrden = SIN_ORDEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_hoteles);

        double[] dominio = calcularDominioPrecio();
        dominioPrecioMin = dominio[0];
        dominioPrecioMax = dominio[1];
        precioMinimo = dominioPrecioMin;
        precioMaximo = dominioPrecioMax;

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        tvConteo = findViewById(R.id.tv_conteo);
        listContainer = findViewById(R.id.hotel_list_container);
        vacioView = findViewById(R.id.tv_vacio);

        tvLimpiarFiltros = findViewById(R.id.tv_limpiar_filtros);
        tvLimpiarFiltros.setOnClickListener(v -> limpiarFiltros());

        btnFiltros = findViewById(R.id.btn_filtros);
        btnFiltros.setOnClickListener(v -> mostrarFiltros());

        etBuscar = findViewById(R.id.et_buscar);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                consulta = s.toString().trim().toLowerCase(Locale.getDefault());
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        aplicarFiltros();
    }

    private double[] calcularDominioPrecio() {
        double min = Double.MAX_VALUE;
        double max = 0;
        for (Hotel hotel : todosLosHoteles) {
            min = Math.min(min, hotel.pricePerNight);
            max = Math.max(max, hotel.pricePerNight);
        }
        double desde = Math.max(0, Math.floor(min / 50.0) * 50 - 50);
        double hasta = Math.ceil(max / 50.0) * 50 + 50;
        if (hasta - desde < 50) {
            hasta = desde + 50;
        }
        return new double[]{desde, hasta};
    }

    /**
     * Panel de filtros como bottom sheet (Material Design). Los cambios se
     * aplican sobre copias locales y solo se confirman al pulsar
     * "Aplicar filtros", igual que el selector de huéspedes.
     */
    private void mostrarFiltros() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.sheet_filtros_hoteles, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.setOnShowListener(dialogInterface -> {
            View panel = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (panel != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(panel);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        sheetView.findViewById(R.id.iv_cerrar_filtros).setOnClickListener(v -> dialog.dismiss());

        String[] ciudadTemp = {ciudadSeleccionada};
        ChipGroup chipGroupCiudad = sheetView.findViewById(R.id.chipgroup_ciudad);
        Map<Integer, String> idACiudad = new HashMap<>();

        Chip chipTodasCiudades = crearChipFiltro(getString(R.string.opcion_todas_ciudades));
        chipTodasCiudades.setChecked(ciudadSeleccionada == null);
        chipGroupCiudad.addView(chipTodasCiudades);

        Set<String> ciudades = new TreeSet<>();
        for (Hotel hotel : todosLosHoteles) {
            ciudades.add(hotel.city);
        }
        for (String ciudad : ciudades) {
            Chip chip = crearChipFiltro(ciudad);
            chip.setChecked(ciudad.equals(ciudadSeleccionada));
            idACiudad.put(chip.getId(), ciudad);
            chipGroupCiudad.addView(chip);
        }
        chipGroupCiudad.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                ciudadTemp[0] = idACiudad.get(checkedIds.get(0));
            }
        });

        ImageView[] estrellas = {
                sheetView.findViewById(R.id.iv_star_1),
                sheetView.findViewById(R.id.iv_star_2),
                sheetView.findViewById(R.id.iv_star_3),
                sheetView.findViewById(R.id.iv_star_4),
                sheetView.findViewById(R.id.iv_star_5),
        };
        TextView tvCalificacionValor = sheetView.findViewById(R.id.tv_calificacion_valor);
        int[] calificacionTemp = {calificacionMinima};
        Runnable actualizarEstrellas = () -> {
            for (int i = 0; i < estrellas.length; i++) {
                estrellas[i].setImageResource(i < calificacionTemp[0] ? R.drawable.ic_star : R.drawable.ic_star_border);
            }
            tvCalificacionValor.setText(calificacionTemp[0] == 0
                    ? getString(R.string.texto_calificacion_cualquiera)
                    : getString(R.string.texto_calificacion_minima, calificacionTemp[0]));
        };
        actualizarEstrellas.run();
        for (int i = 0; i < estrellas.length; i++) {
            int nuevaSeleccion = i + 1;
            estrellas[i].setOnClickListener(v -> {
                calificacionTemp[0] = calificacionTemp[0] == nuevaSeleccion ? 0 : nuevaSeleccion;
                actualizarEstrellas.run();
            });
        }

        RangeSlider slider = sheetView.findViewById(R.id.slider_precio);
        TextView tvPrecioValor = sheetView.findViewById(R.id.tv_precio_valor);
        slider.setValueFrom((float) dominioPrecioMin);
        slider.setValueTo((float) dominioPrecioMax);
        double[] precioTemp = {precioMinimo, precioMaximo};
        slider.setValues((float) precioTemp[0], (float) precioTemp[1]);
        tvPrecioValor.setText(getString(R.string.texto_rango_precio, precioTemp[0], precioTemp[1]));
        slider.addOnChangeListener((s, value, fromUser) -> {
            List<Float> valores = s.getValues();
            precioTemp[0] = valores.get(0);
            precioTemp[1] = valores.get(1);
            tvPrecioValor.setText(getString(R.string.texto_rango_precio, precioTemp[0], precioTemp[1]));
        });

        ChipGroup chipGroupServicios = sheetView.findViewById(R.id.chipgroup_servicios);
        Set<String> todosLosServicios = new TreeSet<>();
        for (Hotel hotel : todosLosHoteles) {
            todosLosServicios.addAll(Arrays.asList(hotel.services));
        }
        Set<String> serviciosTemp = new LinkedHashSet<>(serviciosSeleccionados);
        for (String servicio : todosLosServicios) {
            Chip chip = crearChipFiltro(servicio);
            chip.setChecked(serviciosSeleccionados.contains(servicio));
            chip.setChipIconResource(ServicioIconos.iconoPara(servicio));
            chip.setChipIconTint(ContextCompat.getColorStateList(this, R.color.io_teal));
            chip.setChipIconVisible(true);
            chip.setOnCheckedChangeListener((boton, marcado) -> {
                if (marcado) {
                    serviciosTemp.add(servicio);
                } else {
                    serviciosTemp.remove(servicio);
                }
            });
            chipGroupServicios.addView(chip);
        }

        ChipGroup chipGroupOrden = sheetView.findViewById(R.id.chipgroup_orden);
        int[] ordenTemp = {modoOrden};
        if (modoOrden == ORDEN_MENOR_A_MAYOR) {
            chipGroupOrden.check(R.id.chip_orden_menor_mayor);
        } else if (modoOrden == ORDEN_MAYOR_A_MENOR) {
            chipGroupOrden.check(R.id.chip_orden_mayor_menor);
        }
        chipGroupOrden.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                ordenTemp[0] = SIN_ORDEN;
                return;
            }
            ordenTemp[0] = checkedIds.get(0) == R.id.chip_orden_menor_mayor ? ORDEN_MENOR_A_MAYOR : ORDEN_MAYOR_A_MENOR;
        });

        sheetView.findViewById(R.id.btn_limpiar_sheet).setOnClickListener(v -> {
            ciudadTemp[0] = null;
            chipGroupCiudad.check(chipTodasCiudades.getId());

            calificacionTemp[0] = 0;
            actualizarEstrellas.run();

            precioTemp[0] = dominioPrecioMin;
            precioTemp[1] = dominioPrecioMax;
            slider.setValues((float) dominioPrecioMin, (float) dominioPrecioMax);
            tvPrecioValor.setText(getString(R.string.texto_rango_precio, dominioPrecioMin, dominioPrecioMax));

            serviciosTemp.clear();
            for (int i = 0; i < chipGroupServicios.getChildCount(); i++) {
                ((Chip) chipGroupServicios.getChildAt(i)).setChecked(false);
            }

            ordenTemp[0] = SIN_ORDEN;
            chipGroupOrden.clearCheck();
        });

        sheetView.findViewById(R.id.btn_aplicar_sheet).setOnClickListener(v -> {
            ciudadSeleccionada = ciudadTemp[0];
            calificacionMinima = calificacionTemp[0];
            precioMinimo = precioTemp[0];
            precioMaximo = precioTemp[1];
            serviciosSeleccionados.clear();
            serviciosSeleccionados.addAll(serviciosTemp);
            modoOrden = ordenTemp[0];
            aplicarFiltros();
            dialog.dismiss();
        });

        dialog.show();
    }

    private Chip crearChipFiltro(String texto) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setTextSize(12);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_bg_color));
        chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.chip_stroke_color));
        chip.setChipStrokeWidth(dpF(1));
        chip.setCheckedIconVisible(false);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_color));
        return chip;
    }

    private void limpiarFiltros() {
        consulta = "";
        etBuscar.setText("");
        ciudadSeleccionada = null;
        calificacionMinima = 0;
        precioMinimo = dominioPrecioMin;
        precioMaximo = dominioPrecioMax;
        serviciosSeleccionados.clear();
        modoOrden = SIN_ORDEN;
        aplicarFiltros();
    }

    private int contarFiltrosActivos() {
        int total = 0;
        if (ciudadSeleccionada != null) total++;
        if (calificacionMinima > 0) total++;
        if (precioMinimo > dominioPrecioMin || precioMaximo < dominioPrecioMax) total++;
        if (!serviciosSeleccionados.isEmpty()) total++;
        if (modoOrden != SIN_ORDEN) total++;
        return total;
    }

    private void aplicarFiltros() {
        List<Hotel> resultado = new ArrayList<>();
        for (Hotel hotel : todosLosHoteles) {
            if (!consulta.isEmpty()) {
                String texto = (hotel.name + " " + hotel.city + " " + hotel.address).toLowerCase(Locale.getDefault());
                if (!texto.contains(consulta)) {
                    continue;
                }
            }
            if (ciudadSeleccionada != null && !ciudadSeleccionada.equals(hotel.city)) {
                continue;
            }
            if (hotel.rating < calificacionMinima) {
                continue;
            }
            if (hotel.pricePerNight < precioMinimo || hotel.pricePerNight > precioMaximo) {
                continue;
            }
            if (!serviciosSeleccionados.isEmpty()
                    && !Arrays.asList(hotel.services).containsAll(serviciosSeleccionados)) {
                continue;
            }
            resultado.add(hotel);
        }

        if (modoOrden == ORDEN_MENOR_A_MAYOR) {
            Collections.sort(resultado, Comparator.comparingDouble(h -> h.pricePerNight));
        } else if (modoOrden == ORDEN_MAYOR_A_MENOR) {
            Collections.sort(resultado, (a, b) -> Double.compare(b.pricePerNight, a.pricePerNight));
        }

        int filtrosActivos = contarFiltrosActivos();
        btnFiltros.setText(filtrosActivos > 0
                ? getString(R.string.btn_filtros_activos, filtrosActivos)
                : getString(R.string.btn_filtros));
        tvLimpiarFiltros.setVisibility(filtrosActivos > 0 ? View.VISIBLE : View.GONE);
        tvConteo.setText(getResources().getQuantityString(
                R.plurals.hoteles_encontrados_plural, resultado.size(), resultado.size()));

        renderHoteles(resultado);
    }

    private void renderHoteles(List<Hotel> hoteles) {
        listContainer.removeAllViews();
        vacioView.setVisibility(hoteles.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Hotel hotel : hoteles) {
            View card = inflater.inflate(R.layout.item_hotel_card_vertical, listContainer, false);
            bindHotelCard(card, hotel);
            if (listContainer.getChildCount() > 0) {
                ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = dp(16);
            }
            listContainer.addView(card);
        }
    }

    private void bindHotelCard(View card, Hotel hotel) {
        ((TextView) card.findViewById(R.id.tv_location)).setText(hotel.address);
        ((TextView) card.findViewById(R.id.tv_rating)).setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
        ((TextView) card.findViewById(R.id.tv_name)).setText(hotel.name);
        ((TextView) card.findViewById(R.id.tv_price)).setText(
                getString(R.string.desde_precio, String.format(Locale.getDefault(), "S/ %.0f", hotel.pricePerNight)));

        MaterialButton btnVerDetalle = card.findViewById(R.id.btn_ver_detalle);
        btnVerDetalle.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.proyecto_iotelito.ui.booking.DetalleHotelActivity.class);
            intent.putExtra(com.example.proyecto_iotelito.ui.booking.DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dpF(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
