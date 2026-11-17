package com.example.proyecto_iotelito.data;
import org.junit.Test;
import static org.junit.Assert.*;
public class TaxiChatPolicyTest {
    @Test public void abreAlAsignarseYConservaLecturaAlFinalizar(){
        assertFalse(TaxiChatPolicy.asignado("","SOLICITADO"));
        assertFalse(TaxiChatPolicy.asignado("driver","SOLICITADO"));
        for(String s:new String[]{"ASIGNADO","EN_CAMINO","EN_TRASLADO"}){
            assertTrue(TaxiChatPolicy.asignado("driver",s));assertTrue(TaxiChatPolicy.permiteEnviar("driver",s));
        }
        assertTrue(TaxiChatPolicy.asignado("driver","FINALIZADO"));
        assertFalse(TaxiChatPolicy.permiteEnviar("driver","FINALIZADO"));
    }
    @Test public void textoAcotadoSinRespuestasInventadas(){
        assertEquals("Estoy en recepción",TaxiChatPolicy.validar(" Estoy en recepción "));
        assertThrows(IllegalArgumentException.class,()->TaxiChatPolicy.validar("   "));
        assertThrows(IllegalArgumentException.class,()->TaxiChatPolicy.validar("x".repeat(4001)));
    }
}
