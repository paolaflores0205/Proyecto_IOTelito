package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.ui.hoteles.HotelAdapter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.text.Normalizer;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lista de hoteles resultado de una búsqueda (datos estáticos). Los
 * filtros de Precio/Valoración/Servicios abren un panel deslizable de
 * Material Design (bottom sheet), igual que en "Ver todos los hoteles":
 * calificación con estrellas tocables, precio con un RangeSlider y
 * servicios con chips seleccionables.
 */
public class ResultadosActivity extends AppCompatActivity {

    private static final int SIN_ORDEN = 0;
    private static final int ORDEN_MENOR_A_MAYOR = 1;
    private static final int ORDEN_MAYOR_A_MENOR = 2;

    public static final String EXTRA_DESTINO = "extra_destino";
    public static final String EXTRA_RESUMEN = "extra_resumen";

    private final List<Hotel> todosLosHoteles = new ArrayList<>(SampleData.HOTELS);
    private HotelAdapter hotelAdapter;
    private BookingSelection bookingSelection;
    private String destinoBusqueda;

    private MaterialButton btnFiltros;
    private TextView tvLimpiarFiltros;

    private double dominioPrecioMin;
    private double dominioPrecioMax;
    private double precioMinimo;
    private double precioMaximo;
    private int calificacionMinima = 0;
    private final Set<String> serviciosSeleccionados = new LinkedHashSet<>();
    private int modoOrden = SIN_ORDEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);
        bookingSelection = BookingSelection.from(getIntent());

        double[] dominio = calcularDominioPrecio();
        dominioPrecioMin = dominio[0];
        dominioPrecioMax = dominio[1];
        precioMinimo = dominioPrecioMin;
        precioMaximo = dominioPrecioMax;

        destinoBusqueda = getIntent().getStringExtra(EXTRA_DESTINO);
        if (destinoBusqueda == null || destinoBusqueda.trim().isEmpty()) {
            destinoBusqueda = "Lima, Perú";
        }
        ((TextView) findViewById(R.id.tv_destino)).setText(destinoBusqueda);

        String resumen = getIntent().getStringExtra(EXTRA_RESUMEN);
        if (resumen != null) {
            ((TextView) findViewById(R.id.tv_resumen)).setText(resumen);
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView rvHoteles = findViewById(R.id.rv_hoteles_resultados);
        rvHoteles.setLayoutManager(new LinearLayoutManager(this));
        hotelAdapter = new HotelAdapter(HotelAdapter.TipoTarjeta.VERTICAL, hotel -> {
            Intent intent = new Intent(this, DetalleHotelActivity.class);
            intent.putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            bookingSelection.putInto(intent);
            startActivity(intent);
        });
        rvHoteles.setAdapter(hotelAdapter);

        btnFiltros = findViewById(R.id.btn_filtros);
        tvLimpiarFiltros = findViewById(R.id.tv_limpiar_filtros);
        btnFiltros.setOnClickListener(v -> mostrarFiltros());
        tvLimpiarFiltros.setOnClickListener(v -> limpiarFiltros());

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
     * confirman solo al pulsar "Aplicar filtros" (igual que el selector de
     * huéspedes y el de "Ver todos los hoteles").
     */
    private void mostrarFiltros() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.sheet_filtros_resultados, null);
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
        calificacionMinima = 0;
        precioMinimo = dominioPrecioMin;
        precioMaximo = dominioPrecioMax;
        serviciosSeleccionados.clear();
        modoOrden = SIN_ORDEN;
        aplicarFiltros();
    }

    private int contarFiltrosActivos() {
        int total = 0;
        if (calificacionMinima > 0) total++;
        if (precioMinimo > dominioPrecioMin || precioMaximo < dominioPrecioMax) total++;
        if (!serviciosSeleccionados.isEmpty()) total++;
        if (modoOrden != SIN_ORDEN) total++;
        return total;
    }

    private void aplicarFiltros() {
        List<Hotel> resultado = new ArrayList<>();
        for (Hotel hotel : todosLosHoteles) {
            if (!coincideConDestino(hotel, destinoBusqueda)) {
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

        renderHoteles(resultado);
    }

    /**
     * Relaciona distritos de Lima Metropolitana con los hoteles de Lima.
     * Para otras ciudades o textos, compara directamente nombre, ciudad y
     * dirección del hotel. Esto mantiene el ejemplo coherente con la pequeña
     * colección estática disponible en este entregable.
     */
    private boolean coincideConDestino(Hotel hotel, String destino) {
        String consulta = normalizar(destino);
        String[] distritosLima = {
                "lima", "san miguel", "miraflores", "barranco", "san isidro",
                "surco", "santiago de surco", "magdalena", "pueblo libre",
                "jesus maria", "la molina", "san borja", "chorrillos"
        };
        for (String distrito : distritosLima) {
            if (consulta.contains(distrito)) {
                return normalizar(hotel.city).equals("lima");
            }
        }

        String datosHotel = normalizar(hotel.name + " " + hotel.city + " " + hotel.address);
        return datosHotel.contains(consulta);
    }

    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void renderHoteles(List<Hotel> hoteles) {
        hotelAdapter.submitList(hoteles);
    }

    private float dpF(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
