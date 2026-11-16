package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.HotelCheckout;
import org.junit.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class AdminReportCodecTest {
    private Map<String, Object> booking(String state) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 123L); row.put("hotelId", 9L);
        row.put("fechaEntrada", "2026-11-19"); row.put("fechaSalida", "2026-11-20");
        row.put("precioTotal", 260.0); row.put("estado", state);
        row.put("roomName", "Suite"); row.put("roomNumber", "310"); row.put("roomIds", Arrays.asList(1001L, 1002L));
        return row;
    }
    @Test public void aceptaEstadosActualesEHistoricosSinPerderReservas() {
        String[] remote = {"CONFIRMADA","ACTIVA","EN_CHECKOUT","FINALIZADA","COMPLETADA","CANCELADA","PENDIENTE_PAGO"};
        Reserva.Estado[] expected = {Reserva.Estado.CONFIRMADA,Reserva.Estado.CONFIRMADA,
                Reserva.Estado.EN_CHECKOUT,Reserva.Estado.COMPLETADA,Reserva.Estado.COMPLETADA,
                Reserva.Estado.CANCELADA,Reserva.Estado.PENDIENTE_PAGO};
        for (int i = 0; i < remote.length; i++)
            assertEquals(remote[i], expected[i], AdminReportCodec.reservation("123", booking(remote[i])).estado);
    }
    @Test public void cuentaInventarioSinConfundirCodigosDePresentacion() {
        Reserva value = AdminReportCodec.reservation("123", booking("FINALIZADA"));
        assertEquals(2, value.cantidadHabitaciones());
        assertEquals("310", value.roomNumber);
        Map<String, Object> old = booking("COMPLETADA"); old.remove("roomIds"); old.put("roomNumber", "101,102,103");
        assertEquals(3, AdminReportCodec.reservation("123", old).cantidadHabitaciones());
    }
    @Test public void rechazaIdentificadoresEstadosYHabitacionesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.reservation("999", booking("CONFIRMADA")));
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.reservation("123", booking("DESCONOCIDA")));
        Map<String,Object> duplicate = booking("CONFIRMADA"); duplicate.put("roomIds", Arrays.asList(1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.reservation("123", duplicate));
    }
    private Map<String, Object> receipt() {
        Map<String, Object> row = new HashMap<>();
        row.put("reservationId", "123"); row.put("hotelId", 9L); row.put("paymentMode", "SIMULATED");
        row.put("base", 260.0); row.put("extra", 20.0); row.put("total", 280.0);
        return row;
    }
    @Test public void comprobanteUsaElDiaDeLimaAlCambiarDeMesEnUtc() {
        HotelCheckout before = AdminReportCodec.checkout("123", receipt(), Date.from(Instant.parse("2026-12-01T04:59:59Z")));
        HotelCheckout after = AdminReportCodec.checkout("123", receipt(), Date.from(Instant.parse("2026-12-01T05:00:00Z")));
        assertEquals(LocalDate.of(2026, 11, 30), before.date);
        assertEquals(LocalDate.of(2026, 12, 1), after.date);
        assertEquals(280, before.total, 0.001);
    }
    @Test public void comprobanteSinFechaOTotalIncoherenteNoSeCuentaComoCobro() {
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.checkout("123", receipt(), null));
        Map<String,Object> wrong = receipt(); wrong.put("total", 999.0);
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.checkout("123", wrong, new Date()));
        assertThrows(IllegalArgumentException.class, () -> AdminReportCodec.checkout("456", receipt(), new Date()));
    }
}
