package com.example.proyecto_iotelito.ui.superadmin.reportes;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import org.junit.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class ReportCalculatorTest {
    private final LocalDate date = LocalDate.of(2026, 10, 19);
    private ManagedHotel hotel(int id, int rooms) {
        return new ManagedHotel(id, "Hotel " + id, "Dirección", "Distrito", "Admin", rooms, 999, 0, true);
    }
    private Reserva booking(int id, int hotel, LocalDate from, LocalDate to, double total, Reserva.Estado state) {
        return new Reserva(id, hotel, "Doble", "101,102", from, to, "2 adultos", total, state, "TEST");
    }
    @Test public void hotelPeriodoMetricasYGraficaUsanElMismoFiltro() {
        List<ManagedHotel> hotels = Arrays.asList(hotel(1, 10), hotel(2, 10));
        List<Reserva> rows = Arrays.asList(
            booking(1, 1, date, date.plusDays(2), 200, Reserva.Estado.CONFIRMADA),
            booking(2, 2, date, date.plusDays(1), 500, Reserva.Estado.COMPLETADA),
            booking(3, 1, date.minusMonths(1), date.minusMonths(1).plusDays(1), 100, Reserva.Estado.CONFIRMADA),
            booking(4, 1, date, date.plusDays(1), 900, Reserva.Estado.CANCELADA));
        ReportCalculator.Result day = ReportCalculator.calculate(hotels, rows, 1, ReportCalculator.Period.DAILY, date);
        assertEquals(2, day.bookings); assertEquals(1, day.cancellations);
        assertEquals(100, day.amount, 0.01); assertEquals(20, day.occupancy, 0.01);
        assertArrayEquals(new int[]{1,0,1}, day.values);
        assertEquals(1, day.ranking.size()); assertEquals(1, day.ranking.get(0).hotel.id);
        ReportCalculator.Result month = ReportCalculator.calculate(hotels, rows, 1, ReportCalculator.Period.MONTHLY, date);
        assertEquals(2, month.bookings); assertEquals(200, month.amount, 0.01);
        assertEquals(1, Arrays.stream(month.values).sum());
        assertNotEquals(day.occupancy, month.occupancy, 0.001);
        ReportCalculator.Result year = ReportCalculator.calculate(hotels, rows, 1, ReportCalculator.Period.YEARLY, date);
        assertEquals(3, year.bookings); assertEquals(300, year.amount, 0.01);
        assertEquals(12, year.values.length); assertEquals(2, Arrays.stream(year.values).sum());
        ReportCalculator.Result all = ReportCalculator.calculate(hotels, rows, 0, ReportCalculator.Period.DAILY, date);
        assertEquals(3, all.bookings); assertEquals(600, all.amount, 0.01); assertEquals(2, all.ranking.size());
    }
    @Test public void respetaLimitesCrucesDeMesYSalidaExcluida() {
        Reserva across = booking(1, 1, LocalDate.of(2026,9,30), LocalDate.of(2026,10,2), 400, Reserva.Estado.COMPLETADA);
        ReportCalculator.Result october = ReportCalculator.calculate(Collections.singletonList(hotel(1, 2)),
            Collections.singletonList(across), 0, ReportCalculator.Period.MONTHLY, date);
        assertEquals(0, october.bookings);
        assertEquals(200, october.amount, 0.01);
        assertEquals(100.0 / 31, october.occupancy, 0.01);
        ReportCalculator.Result departure = ReportCalculator.calculate(Collections.singletonList(hotel(1, 2)),
            Collections.singletonList(across), 0, ReportCalculator.Period.DAILY, LocalDate.of(2026,10,2));
        assertArrayEquals(new int[]{0,1,0}, departure.values);
        assertEquals(0, departure.amount, 0.01);
    }
    @Test public void sinDatosNoInventaEstadisticasNiCapacidad() {
        ReportCalculator.Result out = ReportCalculator.calculate(Collections.singletonList(hotel(1, 0)),
            Collections.emptyList(), 1, ReportCalculator.Period.YEARLY, date);
        assertEquals(0, out.bookings); assertEquals(0, out.amount, 0);
        assertEquals(0, out.occupancy, 0); assertEquals(1, out.ranking.size());
        assertEquals(0, Arrays.stream(out.values).sum());
    }
}
