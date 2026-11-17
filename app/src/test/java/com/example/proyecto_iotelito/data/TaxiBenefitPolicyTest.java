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
    @Test public void solicitaConMontoSuficienteYCheckoutPendienteOFinalizado(){
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,1200,Reserva.Estado.CONFIRMADA)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,1200,Reserva.Estado.CANCELADA)));
        assertFalse(TaxiBenefitPolicy.puedeSolicitar(booking(4,999.99,Reserva.Estado.FINALIZADA)));
        assertTrue(TaxiBenefitPolicy.puedeSolicitar(booking(4,1000,Reserva.Estado.COMPLETADA)));
    }
}
