package com.example.proyecto_iotelito.data.taxista;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaxiRemoteStateTest {
    @Test public void viajeCompletoAvanzaSinSaltos() {
        String[] estados={"SOLICITADO","ASIGNADO","EN_CAMINO","EN_TRASLADO","FINALIZADO"};
        for(int i=0;i<estados.length-1;i++)assertTrue(TaxiRemoteState.permite(estados[i],estados[i+1]));
    }
    @Test public void noPermiteRetrocederReasignarNiSaltarPasos() {
        assertFalse(TaxiRemoteState.permite("SOLICITADO","FINALIZADO"));
        assertFalse(TaxiRemoteState.permite("EN_CAMINO","FINALIZADO"));
        assertFalse(TaxiRemoteState.permite("ASIGNADO","SOLICITADO"));
        assertFalse(TaxiRemoteState.permite("FINALIZADO","ASIGNADO"));
        assertFalse(TaxiRemoteState.permite("ASIGNADO","ASIGNADO"));
    }
    @Test public void conservaLosCuatroEstadosQueEntiendeElCliente() {
        assertEquals("EN_CAMINO",TaxiRemoteState.paraCliente("EN_TRASLADO"));
        for(String estado:new String[]{"SOLICITADO","ASIGNADO","EN_CAMINO","FINALIZADO"})
            assertEquals(estado,TaxiRemoteState.paraCliente(estado));
    }
}
