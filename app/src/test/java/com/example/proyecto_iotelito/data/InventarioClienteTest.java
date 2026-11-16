package com.example.proyecto_iotelito.data;

import static org.junit.Assert.*;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import java.util.*;
import org.junit.Test;

public class InventarioClienteTest {
    private Hotel remoto() {
        Hotel hotel = new Hotel(1, "Lima", "Remoto", "Dirección", 0, 0, "", 777,
                new String[0], new Attraction[0], "", "", "", new String[0]);
        hotel.remote = true;
        return hotel;
    }
    private Habitacion room(int id, int adultos, int ninos, double precio) {
        return new Habitacion(id, 1, "Doble", "H" + id, 20, adultos, ninos, precio, "", 0, true);
    }
    @Test public void hotelRemotoConIdDeEjemploNoUsaInventarioLocal() {
        Hotel hotel = remoto();
        assertTrue(InventarioCliente.habitaciones(hotel).isEmpty());
        hotel.inventory.add(room(900, 5, 2, 777));
        assertEquals(900, InventarioCliente.habitaciones(hotel).get(0).id);
        assertFalse(GeneradorOpcionesHabitaciones.generar(InventarioCliente.habitaciones(hotel), 5, 2).isEmpty());
    }
    @Test public void precioRemotoYPrecioPorGrupoNoSeSustituyenPorTarifaLocal() {
        Hotel hotel = remoto();
        assertEquals(777, InventarioCliente.precioDesde(hotel), 0);
        hotel.precioBusqueda = 1234.5;
        assertEquals(1234.5, InventarioCliente.precioDesde(hotel), 0);
    }
    @Test public void ocupacionSeFiltraAntesDeLimitarOchoCombinaciones() {
        List<Habitacion> inventory = new ArrayList<>();
        inventory.add(room(1, 1, 1, 1));
        inventory.add(room(2, 1, 1, 100));
        for (int id = 3; id <= 10; id++) inventory.add(room(id, 1, 0, id + 7));
        assertTrue(GeneradorOpcionesHabitaciones.generar(inventory, 2, 1).stream()
                .allMatch(o -> o.habitaciones().stream().anyMatch(r -> r.id == 1)));
        List<OpcionHabitaciones> libres = InventarioCliente.individuales(inventory);
        libres.removeIf(o -> o.habitaciones().get(0).id == 1);
        List<OpcionHabitaciones> opciones = InventarioCliente.combinarLibres(libres, 2, 1);
        assertEquals(8, opciones.size());
        assertTrue(opciones.stream().allMatch(o -> o.habitaciones().stream().noneMatch(r -> r.id == 1)));
        assertEquals(110, opciones.get(0).precioPorNoche(), 0);
    }
    @Test public void inventarioVacioOcupadoOInsuficienteNoInventaOpciones() {
        assertTrue(InventarioCliente.combinarLibres(Collections.emptyList(), 2, 1).isEmpty());
        Habitacion room = room(1, 2, 0, 100);
        assertTrue(InventarioCliente.combinarLibres(InventarioCliente.individuales(Arrays.asList(room)), 2, 1).isEmpty());
        room.disponible = false;
        assertTrue(InventarioCliente.individuales(Arrays.asList(room)).isEmpty());
    }
    @Test public void conservaHabitacionElegidaAunqueNoSeaLaMasBarataYActualizaTarifa() {
        Hotel hotel = remoto();
        hotel.inventory.add(room(900, 2, 1, 180));
        hotel.inventory.add(room(901, 2, 1, 300));
        OpcionHabitaciones anterior = new OpcionHabitaciones(Arrays.asList(room(901, 2, 1, 250)));
        OpcionHabitaciones actual = InventarioCliente.actualizarOpcion(hotel, anterior, 2, 1);
        assertNotNull(actual);
        assertEquals(901, actual.habitaciones().get(0).id);
        assertEquals(300, actual.precioPorNoche(), 0);
    }
    @Test public void noSustituyeHabitacionEliminadaPorOtraDelHotel() {
        Hotel hotel = remoto();
        hotel.inventory.add(room(900, 2, 1, 180));
        assertNull(InventarioCliente.actualizarOpcion(hotel,
                new OpcionHabitaciones(Arrays.asList(room(901, 2, 1, 250))), 2, 1));
    }
    @Test public void requiereAlternativasSiCambiaCapacidadODisponibilidad() {
        Hotel hotel = remoto();
        Habitacion habitacion = room(900, 2, 0, 180);
        hotel.inventory.add(habitacion);
        OpcionHabitaciones anterior = new OpcionHabitaciones(Arrays.asList(room(900, 2, 1, 180)));
        assertNull(InventarioCliente.actualizarOpcion(hotel, anterior, 2, 1));
        habitacion.disponible = false;
        assertNull(InventarioCliente.actualizarOpcion(hotel, anterior, 2, 0));
    }
}
