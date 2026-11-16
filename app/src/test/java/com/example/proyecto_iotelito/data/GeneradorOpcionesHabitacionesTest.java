package com.example.proyecto_iotelito.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import org.junit.Test;

import java.util.ArrayList;
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

    @Test
    public void generaAlternativasTrasExcluirHabitacionOcupadaAntesDelLimite() {
        List<Habitacion> inventario = new ArrayList<>();
        inventario.add(habitacion(1, 1, 1, 1));
        inventario.add(habitacion(2, 1, 1, 100));
        for (int id = 3; id <= 10; id++) inventario.add(habitacion(id, 1, 0, id + 7));

        List<OpcionHabitaciones> primeras = GeneradorOpcionesHabitaciones.generar(inventario, 2, 1);
        assertTrue(primeras.stream().allMatch(opcion -> opcion.habitaciones().stream()
                .anyMatch(habitacion -> habitacion.id == 1)));

        inventario.removeIf(habitacion -> habitacion.id == 1);
        List<OpcionHabitaciones> libres = GeneradorOpcionesHabitaciones.generar(inventario, 2, 1);
        assertFalse(libres.isEmpty());
        assertTrue(libres.stream().allMatch(opcion -> opcion.habitaciones().stream()
                .noneMatch(habitacion -> habitacion.id == 1)));
    }

    private Habitacion habitacion(int id, int adultos, int ninos, double precio) {
        return new Habitacion(id, 1, "Habitación", "Hab. " + id, 20,
                adultos, ninos, precio, "Prueba", 0, true);
    }
}
