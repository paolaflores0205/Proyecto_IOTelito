package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import java.time.LocalDate;
import java.util.*;

/** Historial simulado reproducible. No representa reservas de clientes reales. */
public final class DemoBookingHistory {
    private DemoBookingHistory() {}
    public static List<Reserva> generate(List<ManagedHotel> hotels, LocalDate anchor) {
        List<Reserva> result = new ArrayList<>();
        LocalDate start = anchor.withDayOfYear(1).minusYears(1);
        LocalDate end = anchor.withDayOfYear(1).plusYears(1);
        int nextId = -1;
        for (ManagedHotel hotel : hotels) {
            if (hotel.rooms <= 0) continue;
            LocalDate[] freeFrom = new LocalDate[hotel.rooms];
            Arrays.fill(freeFrom, start);
            Random random = new Random(hotel.id * 100003L + anchor.getYear());
            for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
                int seasonal = (day.getMonthValue() + hotel.id) % 4;
                int arrivals = 2 + random.nextInt(3) + (seasonal == 0 ? 1 : 0);
                for (int slot = 0; slot < arrivals; slot++) {
                    boolean cancelled = random.nextInt(11) == 0;
                    int requestedRooms = random.nextInt(5) == 0 ? 2 : 1;
                    int nights = 1 + random.nextInt(5);
                    List<Integer> rooms = new ArrayList<>();
                    int offset = random.nextInt(hotel.rooms);
                    for (int i = 0; i < hotel.rooms && rooms.size() < requestedRooms; i++) {
                        int room = (offset + i) % hotel.rooms;
                        if (!freeFrom[room].isAfter(day)) rooms.add(room);
                    }
                    if (rooms.size() < requestedRooms) continue;
                    LocalDate departure = day.plusDays(nights);
                    StringBuilder codes = new StringBuilder();
                    for (int room : rooms) {
                        if (codes.length() > 0) codes.append(", ");
                        codes.append(101 + room);
                        if (!cancelled) freeFrom[room] = departure;
                    }
                    Reserva.Estado state = cancelled ? Reserva.Estado.CANCELADA
                            : !departure.isAfter(anchor) ? Reserva.Estado.COMPLETADA : Reserva.Estado.CONFIRMADA;
                    double nightly = 100 + hotel.id * 24 + seasonal * 18 + random.nextInt(40);
                    result.add(new Reserva(nextId--, hotel.id, "Habitación de demostración", codes.toString(),
                            day, departure, "Datos simulados", nightly * nights * requestedRooms, state,
                            "DEMO-RPT-" + hotel.id + "-" + day + "-" + slot));
                }
            }
        }
        return result;
    }
}
