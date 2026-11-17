package com.example.proyecto_iotelito.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class AeropuertosClienteTest {
    @Test public void destinosCorrespondenALasCiudadesDelCatalogo(){
        String[][] cases={{"Lima","LIM"},{" Callao ","LIM"},{"Lima Metropolitana","LIM"},
                {"Cusco","CUZ"},{"CUZCO","CUZ"},{"Arequipa","AQP"},{"Trujillo","TRU"},
                {"Paracas","PIO"},{"Pisco","PIO"}};
        for(String[] pair:cases){
            assertEquals(1,AeropuertosCliente.paraCiudad(pair[0]).size());
            assertEquals(pair[1],AeropuertosCliente.paraCiudad(pair[0]).get(0).codigo);
        }
    }
    @Test public void noAsignaLimaACiudadesDesconocidasNiAlNombreDelHotel(){
        for(String city:new String[]{null,"","Ciudad nueva","Hotel Lima en Cusco","Lima - Cusco"})
            assertTrue(AeropuertosCliente.paraCiudad(city).isEmpty());
    }
    @Test public void noAceptaDestinoDeOtraCiudad(){
        assertThrows(IllegalArgumentException.class,()->AeropuertosCliente.elegir("Cusco","LIM"));
        assertThrows(IllegalArgumentException.class,()->AeropuertosCliente.elegir("Desconocida","LIM"));
        assertEquals("Aeropuerto Internacional Alejandro Velasco Astete (CUZ)",AeropuertosCliente.elegir("Cusco","CUZ").etiqueta());
    }
}
