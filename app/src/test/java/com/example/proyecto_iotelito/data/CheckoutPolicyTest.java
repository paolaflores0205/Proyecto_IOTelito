package com.example.proyecto_iotelito.data;

import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class CheckoutPolicyTest {
    @Test public void tarjetaSoloConservaCamposEnmascarados() {
        TarjetaSimulada card = TarjetaSimulada.validar("4242 4242 4242 4242", "12/39", "123", YearMonth.of(2026,11));
        assertEquals("Visa", card.tipo); assertEquals("4242", card.ultimos4);
        assertEquals(new HashSet<>(Arrays.asList("cardType","cardLast4","cardToken","paymentMode")), card.campos().keySet());
        assertFalse(card.campos().toString().contains("4242424242424242"));
        assertFalse(card.campos().containsValue("123"));
        assertEquals(card.token, TarjetaSimulada.leer(card.campos()).token);
    }
    @Test public void rechazaNumeroVencimientoYCvvInvalidos() {
        YearMonth today = YearMonth.of(2026,11);
        assertThrows(IllegalArgumentException.class, () -> TarjetaSimulada.validar("4242424242424241","12/39","123",today));
        assertThrows(IllegalArgumentException.class, () -> TarjetaSimulada.validar("0000000000000000","12/39","123",today));
        for (String expiry : Arrays.asList("00/39","13/39","12/20","11/2026","11"))
            assertThrows(IllegalArgumentException.class, () -> TarjetaSimulada.validar("4242424242424242",expiry,"123",today));
        assertThrows(IllegalArgumentException.class, () -> TarjetaSimulada.validar("4242424242424242","12/39","12",today));
        assertThrows(IllegalArgumentException.class, () -> TarjetaSimulada.validar("4242424242424242","12/39","1234",today));
        assertNotNull(TarjetaSimulada.validar("4242424242424242","11/26","123",today));
    }
    @Test public void amexExigeCuatroDigitosYUnTokenNuevoPorRegistro() {
        TarjetaSimulada a = TarjetaSimulada.validar("378282246310005","12/39","1234",YearMonth.of(2026,11));
        assertEquals("Amex",a.tipo);
        assertNotEquals(a.token,TarjetaSimulada.validar("378282246310005","12/39","1234",YearMonth.of(2026,11)).token);
    }
    @Test public void opinionObligatoriaYAcotada() {
        CheckoutPolicy.validarOpinion(5,"Buen servicio\nGracias");
        for (int rating : new int[]{-1,0,6}) assertThrows(IllegalArgumentException.class, () -> CheckoutPolicy.validarOpinion(rating,"Bien"));
        for (String text : Arrays.asList("","  ","x".repeat(4001)))
            assertThrows(IllegalArgumentException.class, () -> CheckoutPolicy.validarOpinion(4,text));
    }
    @Test public void solicitarUnaVezDesdeSalidaYConservarLecturaDeEstadoAntiguo() {
        LocalDate departure = LocalDate.of(2026,11,16);
        assertFalse(CheckoutPolicy.puedeSolicitar("CONFIRMADA",departure,departure.minusDays(1)));
        assertTrue(CheckoutPolicy.puedeSolicitar("CONFIRMADA",departure,departure));
        assertTrue(CheckoutPolicy.puedeSolicitar("ACTIVA",departure,departure.plusDays(1)));
        for (String state : Arrays.asList("EN_CHECKOUT","FINALIZADA","COMPLETADA","CANCELADA"))
            assertFalse(CheckoutPolicy.puedeSolicitar(state,departure,departure));
        assertTrue(CheckoutPolicy.finalizada("COMPLETADA")); assertTrue(CheckoutPolicy.finalizada("FINALIZADA"));
        assertFalse(CheckoutPolicy.finalizada("EN_CHECKOUT"));
    }
    @Test public void comprobanteUsaTotalConAdicionalYRechazaDescuadre() {
        Map<String,Object> data = new HashMap<>();
        data.put("base",600.0);data.put("extra",40.0);data.put("total",640.0);
        data.put("reason","Daño");data.put("observation","Detalle verificado");data.put("cardType","Visa");data.put("cardLast4","4242");
        ComprobanteCheckout receipt = new ComprobanteCheckout(data,123);
        assertEquals(640,receipt.total,0);assertEquals("Daño",receipt.motivo);assertTrue(receipt.tarjeta.contains("4242"));
        data.put("total",600.0);assertThrows(IllegalArgumentException.class,()->new ComprobanteCheckout(data,123));
    }
}
