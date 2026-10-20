package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Genera combinaciones mínimas de habitaciones disponibles para el grupo. */
public final class GeneradorOpcionesHabitaciones {

    private static final int MAX_OPCIONES = 8;

    private GeneradorOpcionesHabitaciones() { }

    public static List<OpcionHabitaciones> generar(int hotelId, int adultos, int ninos) {
        List<Habitacion> disponibles = new ArrayList<>();
        for (Habitacion habitacion : CatalogoHabitaciones.porHotel(hotelId)) {
            if (habitacion.disponible) disponibles.add(habitacion);
        }

        List<OpcionHabitaciones> opciones = new ArrayList<>();
        combinar(disponibles, 0, adultos, ninos, new ArrayList<>(), 0, 0, opciones);
        opciones.removeIf(opcion -> !esCombinacionMinima(opcion, adultos, ninos));
        opciones.sort(Comparator
                .comparingDouble(OpcionHabitaciones::precioPorNoche)
                .thenComparingInt(OpcionHabitaciones::cantidadHabitaciones));
        if (opciones.size() > MAX_OPCIONES) {
            return new ArrayList<>(opciones.subList(0, MAX_OPCIONES));
        }
        return opciones;
    }

    /** Evita ofrecer una habitación extra que pueda retirarse sin perder capacidad. */
    private static boolean esCombinacionMinima(OpcionHabitaciones opcion,
                                                int adultosNecesarios, int ninosNecesarios) {
        for (Habitacion habitacion : opcion.habitaciones()) {
            boolean adultosCubiertos = opcion.capacidadAdultos() - habitacion.adultos >= adultosNecesarios;
            boolean ninosCubiertos = opcion.capacidadNinos() - habitacion.ninos >= ninosNecesarios;
            if (adultosCubiertos && ninosCubiertos) return false;
        }
        return true;
    }

    private static void combinar(List<Habitacion> disponibles, int indice,
                                  int adultosNecesarios, int ninosNecesarios,
                                  List<Habitacion> seleccionadas,
                                  int capacidadAdultos, int capacidadNinos,
                                  List<OpcionHabitaciones> resultado) {
        if (capacidadAdultos >= adultosNecesarios && capacidadNinos >= ninosNecesarios) {
            resultado.add(new OpcionHabitaciones(seleccionadas));
            return;
        }
        if (indice >= disponibles.size()) return;

        Habitacion actual = disponibles.get(indice);
        seleccionadas.add(actual);
        combinar(disponibles, indice + 1, adultosNecesarios, ninosNecesarios,
                seleccionadas,
                capacidadAdultos + actual.adultos,
                capacidadNinos + actual.ninos,
                resultado);
        seleccionadas.remove(seleccionadas.size() - 1);

        combinar(disponibles, indice + 1, adultosNecesarios, ninosNecesarios,
                seleccionadas, capacidadAdultos, capacidadNinos, resultado);
    }
}
