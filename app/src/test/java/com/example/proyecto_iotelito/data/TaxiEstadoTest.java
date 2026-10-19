package com.example.proyecto_iotelito.data;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TaxiEstadoTest {
    @Test
    public void soloPermiteElSiguienteEstadoDelRecorrido() {
        TaxiRepository.Estado[] estados = TaxiRepository.Estado.values();
        for (int origen = 0; origen < estados.length; origen++) {
            for (int destino = 0; destino < estados.length; destino++) {
                assertEquals(estados[origen] + " → " + estados[destino],
                        destino == origen + 1, estados[origen].permite(estados[destino]));
            }
        }
    }
}
