package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.proyecto_iotelito.model.Reserva;
import java.time.LocalDate;

/** Tabla exclusiva de reportes: nunca se muestra en Mis reservas del cliente. */
@Entity(tableName = "report_demo_bookings")
public class ReportDemoBookingEntity {
    @PrimaryKey public int id;
    public int hotelId;
    @NonNull public String arrival = "", departure = "", roomCodes = "", state = "", code = "";
    public double total;
    public static ReportDemoBookingEntity from(Reserva booking) {
        ReportDemoBookingEntity row = new ReportDemoBookingEntity();
        row.id = booking.id; row.hotelId = booking.hotelId;
        row.arrival = booking.fechaEntrada.toString(); row.departure = booking.fechaSalida.toString();
        row.roomCodes = booking.roomNumber; row.state = booking.estado.name();
        row.code = booking.codigo; row.total = booking.precioTotal;
        return row;
    }
    public Reserva toModel() {
        return new Reserva(id, hotelId, "Habitación de demostración", roomCodes,
                LocalDate.parse(arrival), LocalDate.parse(departure), "Datos simulados",
                total, Reserva.Estado.valueOf(state), code);
    }
}
