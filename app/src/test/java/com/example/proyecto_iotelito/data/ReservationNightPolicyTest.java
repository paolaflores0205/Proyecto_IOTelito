package com.example.proyecto_iotelito.data;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReservationNightPolicyTest {
    private final LocalDate entrada=LocalDate.of(2026,12,16),salida=LocalDate.of(2026,12,18);
    @Test public void salidaNoOcupaNocheYPermiteContinuarEnOtroHotel(){
        assertEquals(Arrays.asList(entrada,entrada.plusDays(1)),ReservationNightPolicy.noches(entrada,salida));
        for(String state:new String[]{"CONFIRMADA","ACTIVA","EN_CHECKOUT"}){
            assertTrue(ReservationNightPolicy.bloquea(state,entrada,salida,entrada));
            assertTrue(ReservationNightPolicy.bloquea(state,entrada,salida,salida.minusDays(1)));
            assertFalse(ReservationNightPolicy.bloquea(state,entrada,salida,salida));
        }
    }
    @Test public void bloqueosFinalizadosCanceladosOExterioresNoImpidenOtraEstadia(){
        for(String state:new String[]{"FINALIZADA","COMPLETADA","CANCELADA"})
            assertFalse(ReservationNightPolicy.bloquea(state,entrada,salida,entrada));
        assertFalse(ReservationNightPolicy.bloquea("ACTIVA",entrada,salida,entrada.minusDays(1)));
        assertFalse(ReservationNightPolicy.bloquea("EN_CHECKOUT",entrada,salida,salida.plusDays(1)));
    }
    @Test public void datosInconsistentesNoLiberanUnaNocheSinValidacion(){
        assertThrows(IllegalArgumentException.class,()->ReservationNightPolicy.noches(entrada,entrada));
        assertThrows(IllegalArgumentException.class,()->ReservationNightPolicy.bloquea("DESCONOCIDA",entrada,salida,entrada));
        assertThrows(IllegalArgumentException.class,()->ReservationNightPolicy.bloquea("CONFIRMADA",salida,entrada,entrada));
    }
}
