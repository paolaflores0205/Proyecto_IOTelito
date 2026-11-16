package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.CatalogoHabitaciones;
import com.example.proyecto_iotelito.data.GeneradorOpcionesHabitaciones;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.List;
import java.util.Locale;

/**
 * Detalle de una opción: puede contener una sola habitación o una combinación.
 */
public class DetalleHabitacionActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    private Bundle borradorEstadia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_habitacion);
        if (savedInstanceState != null) borradorEstadia = savedInstanceState.getBundle("borrador_estadia");

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        com.example.proyecto_iotelito.data.ClientDataLoader.hotel(this, hotelId, hotel -> {
        if (!hotel.active || !hotel.hasInventory) { finish(); return; }
        boolean modoCatalogo = getIntent().getBooleanExtra(
                OpcionesHabitacionesActivity.EXTRA_MODO_CATALOGO,
                !BookingSelection.isPresent(getIntent()));
        BookingSelection bookingSelection = modoCatalogo ? null : BookingSelection.from(getIntent());
        OpcionHabitaciones opcion = (OpcionHabitaciones) getIntent().getSerializableExtra(
                OpcionesHabitacionesActivity.EXTRA_OPCION);
        if (opcion == null) {
            if (modoCatalogo) {
                List<Habitacion> habitaciones = hotel.remote ? hotel.inventory : CatalogoHabitaciones.disponiblesPorHotel(hotelId);
                if (habitaciones.isEmpty()) {
                    finish();
                    return;
                }
                opcion = new OpcionHabitaciones(java.util.Collections.singletonList(habitaciones.get(0)));
            } else {
                List<OpcionHabitaciones> opciones = GeneradorOpcionesHabitaciones.generar(
                        hotel.remote ? hotel.inventory : CatalogoHabitaciones.disponiblesPorHotel(hotelId),
                        bookingSelection.adultos, bookingSelection.ninos);
                if (opciones.isEmpty()) {
                    finish();
                    return;
                }
                opcion = opciones.get(0);
            }
            if (opcion == null) {
                finish();
                return;
            }
        }
        final OpcionHabitaciones opcionSeleccionada = opcion;

        Habitacion primeraHabitacion = opcion.habitaciones().get(0);
        ((ImageView) findViewById(R.id.iv_room_photo)).setImageResource(
                primeraHabitacion.fotoRes != 0
                        ? primeraHabitacion.fotoRes : HotelMedia.galleryImage(hotel.id, 1));

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(
                opcion.cantidadHabitaciones() == 1 ? opcion.titulo() : getString(R.string.detalle_combinacion));

        ((TextView) findViewById(R.id.tv_room_name)).setText(opcion.titulo());
        ((TextView) findViewById(R.id.tv_room_price)).setText(
                String.format(Locale.getDefault(), "S/ %.0f", opcion.precioPorNoche()));
        ((TextView) findViewById(R.id.tag_capacidad)).setText(opcion.aforo());
        ((TextView) findViewById(R.id.tag_tamano)).setText(
                getString(R.string.area_total_formato, opcion.areaTotalM2()));
        poblarCaracteristicas(opcion);

        TextView botonReservar = findViewById(R.id.btn_reservar);
        if (modoCatalogo) {
            findViewById(R.id.container_estadia).setVisibility(View.GONE);
            botonReservar.setText(R.string.elegir_fechas_huespedes);
            botonReservar.setOnClickListener(v -> {
                if (getSupportFragmentManager().findFragmentByTag("estadia_hotel") != null) return;
                Bundle args = borradorEstadia == null ? new Bundle() : new Bundle(borradorEstadia);
                args.putString("hotel", hotel.name + " · " + opcionSeleccionada.titulo());
                if (borradorEstadia == null) args.putInt("adultos", Math.min(2, opcionSeleccionada.capacidadAdultos()));
                SeleccionEstadiaSheet sheet = new SeleccionEstadiaSheet();
                sheet.setArguments(args);
                sheet.show(getSupportFragmentManager(), "estadia_hotel");
            });
        } else {
            findViewById(R.id.container_estadia).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_estadia)).setText(
                    getString(R.string.estadia_seleccionada, bookingSelection.textoFechas()));
            double total = opcion.precioPorNoche() * bookingSelection.noches();
            botonReservar.setOnClickListener(v -> {
                Intent intent = new Intent(this, ConfirmacionReservaActivity.class);
                intent.putExtra(ConfirmacionReservaActivity.EXTRA_HOTEL_ID, hotel.id);
                intent.putExtra(ConfirmacionReservaActivity.EXTRA_TOTAL, total);
                intent.putExtra(OpcionesHabitacionesActivity.EXTRA_OPCION, opcionSeleccionada);
                bookingSelection.putInto(intent);
                startActivity(intent);
            });
        }
        getSupportFragmentManager().setFragmentResultListener(SeleccionEstadiaSheet.RESULT, this, (key, result) -> {
            borradorEstadia = new Bundle(result);
            borradorEstadia.putBoolean("fechas", true);
            comprobarEstadia(hotelId, opcionSeleccionada, new BookingSelection(result.getLong("entrada"),
                    result.getLong("salida"), result.getInt("adultos"), result.getInt("ninos")));
        });
        });
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (borradorEstadia != null) state.putBundle("borrador_estadia", borradorEstadia);
    }

    private void comprobarEstadia(int hotelId, OpcionHabitaciones elegida, BookingSelection seleccion) {
        TextView boton = findViewById(R.id.btn_reservar);
        if (!com.example.proyecto_iotelito.data.ConexionCliente.disponible(this)) {
            android.widget.Toast.makeText(this, R.string.cliente_sin_conexion, android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        boton.setEnabled(false);
        boton.setText(R.string.consultando_disponibilidad);
        com.example.proyecto_iotelito.data.ClientLocalTasks.run(this, this, () -> {
            Hotel actual = com.example.proyecto_iotelito.data.HotelCatalogRepository.buscar(this, hotelId);
            if (actual == null || !actual.active) throw new IllegalArgumentException("El hotel ya no está disponible.");
            OpcionHabitaciones opcion = com.example.proyecto_iotelito.data.InventarioCliente.actualizarOpcion(
                    actual, elegida, seleccion.adultos, seleccion.ninos);
            if (opcion == null) return null;
            List<OpcionHabitaciones> libres = com.example.proyecto_iotelito.data.ReservaRepository.getInstance(this)
                    .filtrarDisponibles(hotelId, seleccion.fechaEntrada(), seleccion.fechaSalida(),
                            java.util.Collections.singletonList(opcion));
            return libres.isEmpty() ? null : opcion;
        }, disponible -> {
            boton.setEnabled(true);
            boton.setText(R.string.elegir_fechas_huespedes);
            Intent intent;
            if (disponible != null) {
                intent = new Intent(this, DetalleHabitacionActivity.class);
                intent.putExtra(OpcionesHabitacionesActivity.EXTRA_OPCION, disponible);
                intent.putExtra(OpcionesHabitacionesActivity.EXTRA_MODO_CATALOGO, false);
            } else {
                android.widget.Toast.makeText(this, R.string.habitacion_ver_alternativas, android.widget.Toast.LENGTH_LONG).show();
                intent = new Intent(this, OpcionesHabitacionesActivity.class);
            }
            intent.putExtra(EXTRA_HOTEL_ID, hotelId);
            seleccion.putInto(intent);
            startActivity(intent);
            if (disponible != null) finish();
        }, () -> {
            boton.setEnabled(true);
            boton.setText(R.string.elegir_fechas_huespedes);
        });
    }

    private void poblarCaracteristicas(OpcionHabitaciones opcion) {
        LinearLayout container = findViewById(R.id.features_list);
        container.removeAllViews();

        for (Habitacion habitacion : opcion.habitaciones()) {
            TextView encabezado = new TextView(this);
            encabezado.setText(getString(R.string.titulo_habitacion_codigo,
                    habitacion.tipo, habitacion.nombreCodigo));
            encabezado.setTextSize(14);
            encabezado.setTextColor(ContextCompat.getColor(this, R.color.io_text_primary));
            encabezado.setTypeface(encabezado.getTypeface(), android.graphics.Typeface.BOLD);
            encabezado.setPadding(0, dp(8), 0, dp(2));
            container.addView(encabezado);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            row.setPadding(0, dp(4), 0, dp(4));

            ImageView check = new ImageView(this);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(16), dp(16));
            check.setLayoutParams(checkParams);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.io_teal)));
            row.addView(check);

            TextView label = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.setMarginStart(dp(8));
            label.setLayoutParams(labelParams);
            label.setText(getString(R.string.detalle_habitacion_capacidad,
                    habitacion.aforo(), habitacion.areaM2, habitacion.descripcion));
            label.setTextSize(13);
            label.setTextColor(ContextCompat.getColor(this, R.color.io_text_secondary));
            row.addView(label);

            container.addView(row);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
