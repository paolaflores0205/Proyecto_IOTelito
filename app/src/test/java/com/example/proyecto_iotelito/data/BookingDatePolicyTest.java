package com.example.proyecto_iotelito.data;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class BookingDatePolicyTest {
    private final LocalDate hoy=LocalDate.of(2026,11,16);
    @Test public void pruebasPermitenIngresoYSalidaAnterioresAHoy(){
        BookingDatePolicy.validar(hoy.minusDays(5),hoy.minusDays(2),hoy,true);
        BookingDatePolicy.validar(hoy.minusDays(1),hoy,hoy,true);
        BookingDatePolicy.validar(hoy,hoy.plusDays(1),hoy,true);
    }
    @Test public void nuncaPermiteCeroNochesNiFechasInvertidas(){
        for(boolean pruebas:new boolean[]{true,false}){
            assertThrows(IllegalArgumentException.class,()->BookingDatePolicy.validar(hoy,hoy,hoy,pruebas));
            assertThrows(IllegalArgumentException.class,()->BookingDatePolicy.validar(hoy,hoy.minusDays(1),hoy,pruebas));
        }
    }
    @Test public void restaurarRestriccionBloqueaIngresoPasado(){
        assertThrows(IllegalArgumentException.class,()->BookingDatePolicy.validar(hoy.minusDays(5),hoy.minusDays(2),hoy,false));
        BookingDatePolicy.validar(hoy,hoy.plusDays(1),hoy,false);
    }
}
