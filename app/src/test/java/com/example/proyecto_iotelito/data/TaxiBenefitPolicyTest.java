package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Reserva;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaxiBenefitPolicyTest {
    private Reserva booking(int noches,double importe,Reserva.Estado estado){
        LocalDate entrada=LocalDate.of(2026,11,10);
        return new Reserva(1,1,"Simple","101",entrada,entrada.plusDays(noches),"1 adulto",importe,estado,"QA");
    }
    @Test public void minimoIncluyeMilExactosYRechazaMontosInvalidos(){
        assertTrue(TaxiBenefitPolicy.cumpleMonto(1000));
        assertTrue(TaxiBenefitPolicy.cumpleMonto(1250.75));
        for(double amount:new double[]{999.99,0,-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertFalse(TaxiBenefitPolicy.cumpleMonto(amount));
    }
    @Test public void nochesNoReemplazanElMontoMinimo(){
        assertFalse(TaxiBenefitPolicy.cumpleMonto(booking(4,200,Reserva.Estado.EN_CHECKOUT)));
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(1,1000,Reserva.Estado.EN_CHECKOUT)));
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(4,1000,Reserva.Estado.FINALIZADA)));
    }
    private com.example.proyecto_iotelito.model.Hotel hotel(Double minimo){
        com.example.proyecto_iotelito.model.Hotel hotel=new com.example.proyecto_iotelito.model.Hotel(1,"Lima","Hotel","Av. 1",0,0,"",100,
                new String[0],new com.example.proyecto_iotelito.model.Attraction[0],"","","",new String[0]);
        hotel.taxiMinimo=minimo;
        return hotel;
    }
    @Test public void cadaHotelDefineSuMontoMinimo(){
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(2,600,Reserva.Estado.EN_CHECKOUT),hotel(500.0)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(2,600,Reserva.Estado.EN_CHECKOUT),hotel(1500.0)));
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(1,50,Reserva.Estado.FINALIZADA),hotel(0.0)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(2,600,Reserva.Estado.CONFIRMADA),hotel(500.0)));
    }
    @Test public void sinMontoConfiguradoOInvalidoUsaMilSoles(){
        assertEquals(1000.0,TaxiBenefitPolicy.minimo(hotel(null)),0);
        assertEquals(1000.0,TaxiBenefitPolicy.minimo(hotel(-1.0)),0);
        assertEquals(1000.0,TaxiBenefitPolicy.minimo((com.example.proyecto_iotelito.model.Hotel)null),0);
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(2,999,Reserva.Estado.EN_CHECKOUT),hotel(null)));
        assertEquals("S/ 750",TaxiBenefitPolicy.texto(hotel(750.0)));
        assertEquals("S/ 1000",TaxiBenefitPolicy.texto(null));
    }
    @Test public void solicitaConMontoSuficienteYCheckoutPendienteOFinalizado(){
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,1200,Reserva.Estado.CONFIRMADA)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,1200,Reserva.Estado.CANCELADA)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,999.99,Reserva.Estado.FINALIZADA)));
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(4,1000,Reserva.Estado.COMPLETADA)));
    }
}
