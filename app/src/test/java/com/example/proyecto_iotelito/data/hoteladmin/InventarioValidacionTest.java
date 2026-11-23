package com.example.proyecto_iotelito.data.hoteladmin;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

public class InventarioValidacionTest {
    @Test public void habitacionCorrectaNoTieneErrores() {
        assertTrue(InventarioValidacion.habitacion("Hab. 101", "25", "2", "1", "180.50", "Vista al mar").isEmpty());
        assertTrue(InventarioValidacion.habitacion("Suite", "1000", "10", "", "100000", "").isEmpty());
    }

    @Test public void habitacionRechazaValoresImposibles() {
        Map<String, String> e = InventarioValidacion.habitacion("", "4", "0", "11", "0", "");
        assertTrue(e.keySet().containsAll(java.util.Arrays.asList("codigo", "area", "adultos", "ninos", "precio")));
        assertTrue(InventarioValidacion.habitacion("A", "1001", "11", "0", "100000.01", "").keySet()
                .containsAll(java.util.Arrays.asList("area", "adultos", "precio")));
        assertTrue(InventarioValidacion.habitacion("A", "20", "2", "0", "12.345", "").containsKey("precio"));
        assertTrue(InventarioValidacion.habitacion("A", "veinte", "dos", "0", "abc", "").keySet()
                .containsAll(java.util.Arrays.asList("area", "adultos", "precio")));
    }

    @Test public void servicioAdmiteGratisPeroNoNegativoNiExcesivo() {
        assertTrue(InventarioValidacion.servicio("Wifi", "0", "").isEmpty());
        assertTrue(InventarioValidacion.servicio("Spa", "90,50", "").isEmpty());
        assertTrue(InventarioValidacion.servicio("Spa", "-1", "").containsKey("precio"));
        assertTrue(InventarioValidacion.servicio("Spa", "10000.01", "").containsKey("precio"));
        assertTrue(InventarioValidacion.servicio("S", "10", "").containsKey("nombre"));
    }

    @Test public void telefonoDelHotel() {
        assertNull(InventarioValidacion.telefono("(01) 610-4000"));
        assertNull(InventarioValidacion.telefono(""));
        assertNull(InventarioValidacion.telefono("+51 987 654 321"));
        assertNotNull(InventarioValidacion.telefono("123456"));
        assertNotNull(InventarioValidacion.telefono("987abc654"));
        assertNotNull(InventarioValidacion.telefono("1234567890123456"));
    }
}
