package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Reserva;
import java.time.LocalDate;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResumenCheckoutClienteTest {
    private Reserva reserva(Reserva.Estado state){
        return new Reserva(42,7,"Suite","101",LocalDate.of(2026,11,10),LocalDate.of(2026,11,13),
                "2 adultos",1200.50,state,"QA42");
    }
    private Map<String,Object> receipt(){
        Map<String,Object> d=new HashMap<>();d.put("reservationId","42");d.put("hotelId",7L);d.put("clientUid","client");
        d.put("base",1200.50);d.put("extra",50.25);d.put("total",1250.75);return d;
    }
    @Test public void todasLasVistasPuedenUsarElMismoTotalSinAlterarAlojamiento(){
        Reserva row=reserva(Reserva.Estado.FINALIZADA);
        row.comprobante=ComprobanteCheckout.deReserva(row,receipt(),123L,"client");
        assertEquals(1250.75,row.importeMostrado(),0);assertEquals(1200.50,row.precioTotal,0);
        assertEquals(50.25,row.comprobante.extra,0);assertFalse(row.esActiva());
    }
    @Test public void conservaFinalizadasNuevasYAntiguasEnHistorial(){
        for(Reserva.Estado state:new Reserva.Estado[]{Reserva.Estado.FINALIZADA,Reserva.Estado.COMPLETADA}){
            Reserva row=reserva(state);assertFalse(row.esActiva());
            row.comprobante=ComprobanteCheckout.deReserva(row,receipt(),123L,"client");
            assertEquals(1250.75,row.importeMostrado(),0);
        }
        assertTrue(reserva(Reserva.Estado.EN_CHECKOUT).esActiva());
    }
    @Test public void noAsociaImportesDeOtraCuentaHotelOReserva(){
        Reserva row=reserva(Reserva.Estado.FINALIZADA);
        for(String field:new String[]{"clientUid","hotelId","reservationId"}){
            Map<String,Object> bad=receipt();bad.put(field,field.equals("hotelId")?8L:"other");
            assertThrows(IllegalArgumentException.class,()->ComprobanteCheckout.deReserva(row,bad,123L,"client"));
        }
        assertThrows(IllegalArgumentException.class,()->ComprobanteCheckout.deReserva(row,receipt(),0,"client"));
    }
    @Test public void noPresentaUnCobroAntesDeFinalizarNiConOtraBase(){
        assertThrows(IllegalArgumentException.class,()->ComprobanteCheckout.deReserva(reserva(Reserva.Estado.EN_CHECKOUT),receipt(),123,"client"));
        Map<String,Object> bad=receipt();bad.put("base",1000);bad.put("total",1050.25);
        assertThrows(IllegalArgumentException.class,()->ComprobanteCheckout.deReserva(reserva(Reserva.Estado.FINALIZADA),bad,123,"client"));
    }
}
