package com.example.proyecto_iotelito.model;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class ReservaCheckoutTest {
    private Reserva crear(Reserva.Estado estado, LocalDate salida) {
        return new Reserva(1, 1, "Suite", "310", LocalDate.now().minusDays(3), salida,
                "2 adultos", 1140, estado, "IOT-PRUEBA");
    }

    @Test
    public void soloLasReservasActivasConSalidaHoyPermitenCheckout() {
        for (Reserva.Estado estado : Reserva.Estado.values()) {
            Reserva reserva = crear(estado, LocalDate.now());
            assertEquals(estado.name(), reserva.esActiva(), reserva.puedeHacerCheckout());
        }
        assertFalse(crear(Reserva.Estado.CONFIRMADA, LocalDate.now().plusDays(1)).puedeHacerCheckout());
        assertFalse(crear(Reserva.Estado.CONFIRMADA, LocalDate.now().minusDays(1)).puedeHacerCheckout());
    }

    @Test
    public void finalizarLaReservaImpideRepetirCheckoutElMismoDia() {
        Reserva reserva = crear(Reserva.Estado.CONFIRMADA, LocalDate.now());
        assertTrue(reserva.puedeHacerCheckout());
        reserva.estado = Reserva.Estado.COMPLETADA;
        assertFalse(reserva.puedeHacerCheckout());
    }
}
