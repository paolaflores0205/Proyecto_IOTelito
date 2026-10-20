package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Reserva;
import org.junit.Test;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class ReservasPruebaTest {
    @Test
    public void ejemplosIncluyenCheckoutTaxiYDistintosEstados() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = ReservasPrueba.crear(hoy, 200);
        assertEquals(7, reservas.size());
        Set<Integer> hoteles = new HashSet<>();
        assertEquals(3, reservas.stream().filter(reserva -> reserva.esActiva() && reserva.puedeHacerCheckout()).count());
        assertEquals(1, reservas.stream().filter(Reserva::esProxima).count());
        assertEquals(1, reservas.stream().filter(reserva -> reserva.estado == Reserva.Estado.COMPLETADA).count());
        assertEquals(1, reservas.stream().filter(reserva -> reserva.estado == Reserva.Estado.CANCELADA).count());
        for (Reserva reserva : reservas) {
            assertTrue(hoteles.add(reserva.hotelId));
            assertEquals(reserva.hotelId, SampleData.findById(reserva.hotelId).id);
            assertTrue(reserva.noches > 0);
            if (reserva.puedeHacerCheckout()) assertTrue(reserva.precioTotal >= 1000);
        }
    }

    @Test
    public void codigosEstablesPermitenIdentificarLosEjemplosSinDuplicarlos() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> primero = ReservasPrueba.crear(hoy, 200);
        List<Reserva> siguiente = ReservasPrueba.crear(hoy.plusDays(1), 207);
        Set<String> codigos = new HashSet<>();
        for (int indice = 0; indice < primero.size(); indice++) {
            assertTrue(codigos.add(primero.get(indice).codigo));
            assertTrue(primero.get(indice).codigo.startsWith(ReservasPrueba.PREFIJO_CODIGO));
            assertEquals(primero.get(indice).codigo, siguiente.get(indice).codigo);
            assertNotEquals(primero.get(indice).id, siguiente.get(indice).id);
        }
        assertEquals(7, codigos.size());
    }
}
