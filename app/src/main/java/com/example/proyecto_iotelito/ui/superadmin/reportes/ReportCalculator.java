package com.example.proyecto_iotelito.ui.superadmin.reportes;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Estadísticas locales, sin usar los contadores estáticos del catálogo. */
public final class ReportCalculator {
    public enum Period { DAILY, MONTHLY, YEARLY }
    public static final class Ranking {
        public final ManagedHotel hotel;
        public final double occupancy;
        public Ranking(ManagedHotel hotel, double occupancy) { this.hotel = hotel; this.occupancy = occupancy; }
    }
    public static final class Result {
        public LocalDate start, end;
        public int bookings, cancellations;
        public double amount, occupancy;
        public String title;
        public String[] labels;
        public int[] values;
        public final List<Ranking> ranking = new ArrayList<>();
    }
    public static Result calculate(List<ManagedHotel> hotels, List<Reserva> bookings,
                                   int hotelId, Period period, LocalDate date) {
        Result out = new Result();
        out.start = period == Period.DAILY ? date
                : period == Period.MONTHLY ? date.withDayOfMonth(1) : date.withDayOfYear(1);
        out.end = period == Period.DAILY ? out.start.plusDays(1)
                : period == Period.MONTHLY ? out.start.plusMonths(1) : out.start.plusYears(1);
        if (period == Period.DAILY) {
            out.title = "Reservas del día";
            out.labels = new String[]{"Llegadas", "Salidas", "Estadías"};
        } else if (period == Period.MONTHLY) {
            out.title = "Reservas del mes";
            out.labels = new String[]{"1–7", "8–14", "15–21", "22–28", "29–31"};
        } else {
            out.title = "Reservas del año";
            out.labels = new String[]{"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Set","Oct","Nov","Dic"};
        }
        out.values = new int[out.labels.length];
        Set<Integer> included = new HashSet<>();
        Map<Integer, Long> occupied = new HashMap<>();
        long capacity = 0;
        long days = ChronoUnit.DAYS.between(out.start, out.end);
        for (ManagedHotel hotel : hotels) if (hotelId == 0 || hotel.id == hotelId) {
            included.add(hotel.id);
            capacity += Math.max(0, hotel.rooms) * days;
        }
        long totalRoomNights = 0;
        for (Reserva booking : bookings) {
            if (!included.contains(booking.hotelId)) continue;
            boolean arrival = !booking.fechaEntrada.isBefore(out.start) && booking.fechaEntrada.isBefore(out.end);
            if (arrival) {
                out.bookings++;
                if (booking.estado == Reserva.Estado.CANCELADA) out.cancellations++;
            }
            if (booking.estado == Reserva.Estado.CANCELADA) continue;
            LocalDate from = booking.fechaEntrada.isAfter(out.start) ? booking.fechaEntrada : out.start;
            LocalDate to = booking.fechaSalida.isBefore(out.end) ? booking.fechaSalida : out.end;
            long nights = Math.max(0, ChronoUnit.DAYS.between(from, to));
            // Una reserva puede incluir varios códigos de habitación separados por comas.
            int rooms = roomCount(booking.roomNumber);
            long roomNights = nights * rooms;
            totalRoomNights += roomNights;
            occupied.merge(booking.hotelId, roomNights, Long::sum);
            if (booking.noches > 0) out.amount += booking.precioTotal * nights / booking.noches;
            if (period == Period.DAILY) {
                if (arrival) out.values[0]++;
                if (booking.fechaSalida.equals(date)) out.values[1]++;
                if (nights > 0) out.values[2]++;
            } else if (arrival) {
                int bucket = period == Period.MONTHLY ? (booking.fechaEntrada.getDayOfMonth() - 1) / 7
                        : booking.fechaEntrada.getMonthValue() - 1;
                out.values[bucket]++;
            }
        }
        out.occupancy = capacity > 0 ? Math.min(100, totalRoomNights * 100.0 / capacity) : 0;
        for (ManagedHotel hotel : hotels) if (included.contains(hotel.id)) {
            double rate = hotel.rooms > 0 ? Math.min(100, occupied.getOrDefault(hotel.id, 0L) * 100.0 / (hotel.rooms * days)) : 0;
            out.ranking.add(new Ranking(hotel, rate));
        }
        out.ranking.sort(Comparator.comparingDouble((Ranking r) -> r.occupancy).reversed()
                .thenComparing(r -> r.hotel.name));
        return out;
    }
    private static int roomCount(String codes) {
        if (codes == null || codes.trim().isEmpty()) return 1;
        return Math.max(1, codes.split(",").length);
    }
}
