package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Catálogo compartido por el buscador del cliente. Para el hotel administrado
 * se reutiliza la misma lista editable de Admin; los demás hoteles mantienen
 * inventarios estáticos hasta que todos migren a Firebase.
 */
public final class CatalogoHabitaciones {

    private static final List<Habitacion> CUSCO = new ArrayList<>(Arrays.asList(
            new Habitacion(201, 2, "Suite Ejecutiva", "Hab. 804", 38, 2, 0, 520.0,
                    "Cama Queen Size, vista a la plaza colonial y calefacción inteligente.",
                    R.drawable.superadmin_hotel_hero, true),
            new Habitacion(202, 2, "Habitación Doble", "Hab. 506", 32, 2, 1, 410.0,
                    "Dos camas, escritorio y detalles de piedra colonial.",
                    R.drawable.superadmin_hotel_sol, true),
            new Habitacion(203, 2, "Suite Deluxe", "Hab. 901", 52, 3, 2, 710.0,
                    "Suite familiar con sala, minibar y vista al centro histórico.",
                    R.drawable.superadmin_hotel_barranco, true),
            new Habitacion(204, 2, "Habitación Simple", "Hab. 305", 23, 1, 0, 260.0,
                    "Cama individual y espacio de trabajo.",
                    R.drawable.superadmin_hotel_sol, true)
    ));

    private static final List<Habitacion> AREQUIPA = new ArrayList<>(Arrays.asList(
            new Habitacion(301, 3, "Habitación Superior", "Hab. 212", 35, 2, 1, 380.0,
                    "Cama King Size, vista al volcán Misti y minibar.",
                    R.drawable.superadmin_hotel_barranco, true),
            new Habitacion(302, 3, "Habitación Doble", "Hab. 214", 30, 2, 0, 320.0,
                    "Dos camas dobles y vista al patio colonial.",
                    R.drawable.superadmin_hotel_sol, true),
            new Habitacion(303, 3, "Suite Deluxe", "Hab. 401", 49, 3, 2, 590.0,
                    "Suite familiar con sala independiente y vista panorámica.",
                    R.drawable.superadmin_hotel_hero, true),
            new Habitacion(304, 3, "Habitación Simple", "Hab. 118", 21, 1, 0, 210.0,
                    "Cama individual, escritorio y climatización inteligente.",
                    R.drawable.superadmin_hotel_sol, true)
    ));

    private CatalogoHabitaciones() { }

    private static final java.util.Map<Integer, List<Habitacion>> ADICIONALES = new java.util.HashMap<>();

    public static List<Habitacion> porHotel(int hotelId) {
        List<Habitacion> origen;
        if (hotelId == HotelAdminSampleData.HOTEL_ID) {
            origen = HotelAdminSampleData.habitaciones();
        } else if (hotelId == 2) {
            origen = CUSCO;
        } else if (hotelId == 3) {
            origen = AREQUIPA;
        } else {
            origen = ADICIONALES.computeIfAbsent(hotelId, id -> {
                com.example.proyecto_iotelito.model.Hotel hotel = SampleData.findById(id);
                if (hotel.id != id) return new ArrayList<>();
                return new ArrayList<>(Arrays.asList(
                        new Habitacion(id * 100 + 1, id, hotel.roomName, "Hab. 310", 38, 2, 1,
                                hotel.pricePerNight, hotel.description, HotelMedia.hotelImage(id), true),
                        new Habitacion(id * 100 + 2, id, "Habitación Doble", "Hab. 405", 32, 2, 0,
                                hotel.pricePerNight - 60, "Dos camas y espacio de trabajo.",
                                HotelMedia.galleryImage(id, 1), true)));
            });
        }

        List<Habitacion> resultado = new ArrayList<>();
        for (Habitacion habitacion : origen) {
            if (habitacion.hotelId == hotelId) {
                resultado.add(habitacion);
            }
        }
        return resultado;
    }

    public static double precioMinimoDisponible(int hotelId, double precioFallback) {
        double minimo = Double.MAX_VALUE;
        for (Habitacion habitacion : porHotel(hotelId)) {
            if (habitacion.disponible) minimo = Math.min(minimo, habitacion.precioNoche);
        }
        return minimo == Double.MAX_VALUE ? precioFallback : minimo;
    }

    public static List<Habitacion> disponiblesPorHotel(int hotelId) {
        List<Habitacion> disponibles = new ArrayList<>();
        for (Habitacion habitacion : porHotel(hotelId)) {
            if (habitacion.disponible) disponibles.add(habitacion);
        }
        return disponibles;
    }
}
