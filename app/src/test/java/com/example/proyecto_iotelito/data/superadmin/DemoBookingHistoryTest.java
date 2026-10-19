package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import org.junit.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class DemoBookingHistoryTest {
    private final LocalDate anchor = LocalDate.of(2026, 10, 19);
    @Test public void cubreDosAniosTodosLosHotelesYEsReproducible() {
        List<ManagedHotel> hotels = com.example.proyecto_iotelito.data.SuperadminSampleData.hotels();
        List<Reserva> first = DemoBookingHistory.generate(hotels, anchor);
        List<Reserva> again = DemoBookingHistory.generate(hotels, anchor);
        assertTrue(first.size() > 20000);
        assertEquals(first.size(), again.size());
        Set<String> codes = new HashSet<>();
        Set<Integer> hotelIds = new HashSet<>();
        Set<Integer> months = new HashSet<>();
        int cancelled = 0, completed = 0, confirmed = 0;
        for (int i = 0; i < first.size(); i++) {
            Reserva booking = first.get(i);
            assertTrue(booking.id < 0);
            assertTrue(codes.add(booking.codigo));
            assertTrue(booking.codigo.startsWith("DEMO-RPT-"));
            assertEquals(booking.codigo, again.get(i).codigo);
            assertEquals(booking.precioTotal, again.get(i).precioTotal, 0);
            assertFalse(booking.fechaEntrada.isBefore(LocalDate.of(2025, 1, 1)));
            assertTrue(booking.fechaEntrada.isBefore(LocalDate.of(2027, 1, 1)));
            assertTrue(booking.precioTotal > 0);
            hotelIds.add(booking.hotelId); months.add(booking.fechaEntrada.getMonthValue());
            if (booking.estado == Reserva.Estado.CANCELADA) cancelled++;
            else if (booking.estado == Reserva.Estado.COMPLETADA) completed++;
            else if (booking.estado == Reserva.Estado.CONFIRMADA) confirmed++;
        }
        assertEquals(12, hotelIds.size()); assertEquals(12, months.size());
        assertTrue(cancelled > 100); assertTrue(completed > 100); assertTrue(confirmed > 100);
        System.out.println("Historial inicial de 12 hoteles: " + first.size() + " reservas simuladas.");
    }
    @Test public void noDuplicaHabitacionEnLaMismaNocheYOmiteHotelesSinInventario() {
        ManagedHotel hotel = new ManagedHotel(1, "Prueba", "Calle", "Distrito", "", 8, 0, 0, true);
        ManagedHotel empty = new ManagedHotel(2, "Nuevo", "Calle", "Distrito", "", 0, 0, 0, true);
        Set<String> occupied = new HashSet<>();
        for (Reserva booking : DemoBookingHistory.generate(Arrays.asList(hotel, empty), anchor)) {
            assertEquals(1, booking.hotelId);
            if (booking.estado == Reserva.Estado.CANCELADA) continue;
            for (LocalDate day = booking.fechaEntrada; day.isBefore(booking.fechaSalida); day = day.plusDays(1)) {
                for (String room : booking.roomNumber.split(",")) assertTrue(occupied.add(day + "-" + room.trim()));
            }
        }
    }
    @Test public void diarioMensualYAnualTienenDatosParaCadaHotelDelCatalogo() {
        List<ManagedHotel> hotels = com.example.proyecto_iotelito.data.SuperadminSampleData.hotels();
        List<Reserva> rows = DemoBookingHistory.generate(hotels, anchor);
        for (ManagedHotel hotel : hotels) {
            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Result day =
                    com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.calculate(
                            hotels, rows, hotel.id,
                            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Period.DAILY, anchor);
            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Result month =
                    com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.calculate(
                            hotels, rows, hotel.id,
                            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Period.MONTHLY, anchor);
            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Result year =
                    com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.calculate(
                            hotels, rows, hotel.id,
                            com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Period.YEARLY, anchor);
            assertTrue(day.bookings > 0);
            assertTrue(month.bookings > day.bookings);
            assertTrue(year.bookings > month.bookings);
            assertTrue(day.amount > 0);
            assertTrue(month.cancellations > 0);
            assertTrue(month.occupancy > 0 && month.occupancy <= 100);
        }
    }
}
