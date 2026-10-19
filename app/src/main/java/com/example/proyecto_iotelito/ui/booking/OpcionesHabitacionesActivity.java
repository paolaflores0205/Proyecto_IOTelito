package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.CatalogoHabitaciones;
import com.example.proyecto_iotelito.data.GeneradorOpcionesHabitaciones;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lista las habitaciones o combinaciones que cubren la cantidad de huéspedes. */
public class OpcionesHabitacionesActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    public static final String EXTRA_OPCION = "extra_opcion_habitaciones";
    public static final String EXTRA_MODO_CATALOGO = "extra_modo_catalogo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opciones_habitaciones);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        Hotel hotel = SampleData.findById(hotelId);
        boolean modoReserva = BookingSelection.isPresent(getIntent());
        BookingSelection seleccion = modoReserva ? BookingSelection.from(getIntent()) : null;

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(modoReserva
                ? R.string.titulo_opciones_habitaciones : R.string.titulo_catalogo_habitaciones);
        ((TextView) findViewById(R.id.tv_hotel_opciones)).setText(hotel.name);
        TextView busqueda = findViewById(R.id.tv_busqueda_opciones);
        TextView explicacion = findViewById(R.id.tv_explicacion_opciones);
        if (modoReserva) {
            busqueda.setText(getString(R.string.resumen_busqueda_habitaciones,
                    seleccion.textoHuespedes(this), seleccion.textoFechas()));
            explicacion.setText(R.string.opciones_explicacion);
        } else {
            busqueda.setText(R.string.catalogo_habitaciones_subtitulo);
            explicacion.setText(R.string.catalogo_habitaciones_explicacion);
        }

        List<OpcionHabitaciones> opciones = modoReserva
                ? GeneradorOpcionesHabitaciones.generar(hotelId, seleccion.adultos, seleccion.ninos)
                : opcionesDeCatalogo(hotelId);

        RecyclerView recyclerView = findViewById(R.id.rv_opciones_habitaciones);
        TextView vacio = findViewById(R.id.tv_sin_opciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OpcionHabitacionesAdapter(
                hotelId,
                modoReserva ? seleccion.noches() : 0,
                modoReserva,
                opciones,
                opcion -> abrirDetalle(hotelId, seleccion, opcion, !modoReserva)));

        boolean sinOpciones = opciones.isEmpty();
        vacio.setVisibility(sinOpciones ? View.VISIBLE : View.GONE);
        vacio.setText(modoReserva
                ? R.string.sin_combinaciones_habitaciones : R.string.sin_habitaciones_disponibles);
        recyclerView.setVisibility(sinOpciones ? View.GONE : View.VISIBLE);
    }

    private List<OpcionHabitaciones> opcionesDeCatalogo(int hotelId) {
        List<OpcionHabitaciones> opciones = new ArrayList<>();
        for (Habitacion habitacion : CatalogoHabitaciones.disponiblesPorHotel(hotelId)) {
            opciones.add(new OpcionHabitaciones(Collections.singletonList(habitacion)));
        }
        return opciones;
    }

    private void abrirDetalle(int hotelId, BookingSelection seleccion,
                              OpcionHabitaciones opcion, boolean modoCatalogo) {
        Intent intent = new Intent(this, DetalleHabitacionActivity.class);
        intent.putExtra(DetalleHabitacionActivity.EXTRA_HOTEL_ID, hotelId);
        intent.putExtra(EXTRA_OPCION, opcion);
        intent.putExtra(EXTRA_MODO_CATALOGO, modoCatalogo);
        if (seleccion != null) seleccion.putInto(intent);
        startActivity(intent);
    }
}
