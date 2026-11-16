package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.HotelCheckout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Un contrato para las reservas históricas y actuales, sin depender del SDK Android. */
public final class AdminReportCodec {
    private AdminReportCodec() { }

    public static Reserva reservation(String documentId, Map<String, Object> data) {
        if (data == null) throw new IllegalArgumentException("Reserva ausente.");
        int id = HotelFirestoreCodec.integer(data.get("id"));
        int hotel = HotelFirestoreCodec.integer(data.get("hotelId"));
        if (id <= 0 || hotel <= 0 || !String.valueOf(id).equals(documentId))
            throw new IllegalArgumentException("Identificador de reserva inconsistente.");
        double amount = number(data.get("precioTotal"));
        LocalDate from = LocalDate.parse((String) data.get("fechaEntrada"));
        LocalDate to = LocalDate.parse((String) data.get("fechaSalida"));
        if (!from.isBefore(to)) throw new IllegalArgumentException("Fechas de reserva inválidas.");
        Reserva value = new Reserva(id, hotel, (String) data.get("roomName"),
                (String) data.get("roomNumber"), from, to, (String) data.get("huespedes"), amount,
                Reserva.Estado.desdeFirestore((String) data.get("estado")), (String) data.get("codigo"));
        // roomIds es la fuente del inventario; roomNumber se conserva para presentación.
        Object roomIds = data.get("roomIds");
        if (roomIds instanceof List) {
            java.util.Set<Integer> unique = new java.util.HashSet<>();
            for (Object room : (List<?>) roomIds) {
                int roomId = HotelFirestoreCodec.integer(room);
                if (roomId <= 0 || !unique.add(roomId)) throw new IllegalArgumentException("Habitaciones inválidas.");
            }
            if (unique.isEmpty()) throw new IllegalArgumentException("Reserva sin habitaciones.");
            value.setCantidadHabitaciones(unique.size());
        }
        return value;
    }

    public static HotelCheckout checkout(String documentId, Map<String, Object> data, Date createdAt) {
        if (data == null || !documentId.equals(data.get("reservationId")) || createdAt == null
                || !"SIMULATED".equals(data.get("paymentMode")))
            throw new IllegalArgumentException("Comprobante incompleto o incompatible.");
        return new HotelCheckout(documentId, HotelFirestoreCodec.integer(data.get("hotelId")),
                createdAt.toInstant().atZone(ZoneId.of("America/Lima")).toLocalDate(),
                number(data.get("base")), number(data.get("extra")), number(data.get("total")));
    }

    private static double number(Object raw) {
        if (!(raw instanceof Number)) throw new IllegalArgumentException("Importe no numérico.");
        double value = ((Number) raw).doubleValue();
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Importe inválido.");
        return value;
    }
}
