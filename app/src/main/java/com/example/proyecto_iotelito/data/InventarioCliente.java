package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selecciona la fuente del inventario sin sustituir datos remotos por ejemplos locales. */
public final class InventarioCliente {
    private InventarioCliente() { }

    public static List<Habitacion> habitaciones(Hotel hotel) {
        return hotel.remote ? hotel.inventory : CatalogoHabitaciones.disponiblesPorHotel(hotel.id);
    }

    public static double precioDesde(Hotel hotel) {
        if (hotel.precioBusqueda != null) return hotel.precioBusqueda;
        return hotel.remote ? hotel.pricePerNight
                : CatalogoHabitaciones.precioMinimoDisponible(hotel.id, hotel.pricePerNight);
    }

    public static List<OpcionHabitaciones> individuales(List<Habitacion> inventario) {
        List<OpcionHabitaciones> result = new ArrayList<>();
        for (Habitacion room : inventario) if (room.disponible)
            result.add(new OpcionHabitaciones(Collections.singletonList(room)));
        return result;
    }

    /** Conserva los IDs elegidos, pero obtiene capacidad y tarifas del inventario actual. */
    public static OpcionHabitaciones actualizarOpcion(Hotel hotel, OpcionHabitaciones elegida,
                                                      int adultos, int ninos) {
        if (!hotel.active || !hotel.hasInventory) return null;
        List<Habitacion> actuales = new ArrayList<>();
        for (Habitacion anterior : elegida.habitaciones()) {
            Habitacion actual = null;
            for (Habitacion room : habitaciones(hotel))
                if (room.id == anterior.id && room.disponible) { actual = room; break; }
            if (actual == null) return null;
            actuales.add(actual);
        }
        if (actuales.isEmpty()) return null;
        OpcionHabitaciones opcion = new OpcionHabitaciones(actuales);
        return opcion.capacidadAdultos() >= adultos && opcion.capacidadNinos() >= ninos ? opcion : null;
    }

    /** Recibe habitaciones ya comprobadas por fecha, antes de limitar las combinaciones a ocho. */
    public static List<OpcionHabitaciones> combinarLibres(List<OpcionHabitaciones> individuales,
                                                         int adultos, int ninos) {
        List<Habitacion> libres = new ArrayList<>();
        for (OpcionHabitaciones opcion : individuales) libres.addAll(opcion.habitaciones());
        return GeneradorOpcionesHabitaciones.generar(libres, adultos, ninos);
    }
}
