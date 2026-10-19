package com.example.proyecto_iotelito.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import org.junit.Test;

import java.util.List;

public class GeneradorOpcionesHabitacionesTest {

    @Test
    public void grupoPequenoRecibeOpcionesConCapacidadSuficiente() {
        List<OpcionHabitaciones> opciones = GeneradorOpcionesHabitaciones.generar(1, 2, 1);

        assertFalse(opciones.isEmpty());
        for (OpcionHabitaciones opcion : opciones) {
            assertTrue(opcion.capacidadAdultos() >= 2);
            assertTrue(opcion.capacidadNinos() >= 1);
        }
    }

    @Test
    public void seisAdultosRecibenCombinacionesDeVariasHabitaciones() {
        List<OpcionHabitaciones> opciones = GeneradorOpcionesHabitaciones.generar(1, 6, 0);

        assertFalse(opciones.isEmpty());
        for (OpcionHabitaciones opcion : opciones) {
            assertTrue(opcion.capacidadAdultos() >= 6);
            assertTrue(opcion.cantidadHabitaciones() >= 3);
        }
    }

    @Test
    public void nuncaIncluyeHabitacionesNoDisponibles() {
        List<OpcionHabitaciones> opciones = GeneradorOpcionesHabitaciones.generar(1, 4, 2);

        assertFalse(opciones.isEmpty());
        for (OpcionHabitaciones opcion : opciones) {
            for (Habitacion habitacion : opcion.habitaciones()) {
                assertTrue(habitacion.disponible);
                assertTrue(habitacion.id != 2 && habitacion.id != 5);
            }
        }
    }

    @Test
    public void noIncluyeHabitacionesExtraInnecesarias() {
        int adultos = 4;
        int ninos = 2;
        List<OpcionHabitaciones> opciones = GeneradorOpcionesHabitaciones.generar(1, adultos, ninos);

        for (OpcionHabitaciones opcion : opciones) {
            for (Habitacion habitacion : opcion.habitaciones()) {
                boolean sigueCubriendoAdultos =
                        opcion.capacidadAdultos() - habitacion.adultos >= adultos;
                boolean sigueCubriendoNinos =
                        opcion.capacidadNinos() - habitacion.ninos >= ninos;
                assertFalse(sigueCubriendoAdultos && sigueCubriendoNinos);
            }
        }
    }
}
