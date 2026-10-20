package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Reserva;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReservasPrueba {
    public static final String PREFIJO_CODIGO = "IOT-EJEMPLO-V2-";
    private ReservasPrueba() { }

    public static List<Reserva> crear(LocalDate hoy, int primerId) {
        List<Reserva> reservas = new ArrayList<>();
        for (int indice = 0; indice < 3; indice++) {
            reservas.add(new Reserva(primerId + indice, indice + 4,
                    indice == 1 ? "Suite Ejecutiva" : "Habitación Superior", "" + (310 + indice),
                    hoy.minusDays(3), hoy, indice == 2 ? "2 adultos, 1 niño" : "2 adultos",
                    1140 + indice * 210, Reserva.Estado.CONFIRMADA, PREFIJO_CODIGO + indice));
        }
        reservas.add(new Reserva(primerId + 3, 7, "Suite Deluxe", "405",
                hoy.minusDays(1), hoy.plusDays(2), "2 adultos", 1350,
                Reserva.Estado.CONFIRMADA, PREFIJO_CODIGO + 3));
        reservas.add(new Reserva(primerId + 4, 1, "Suite Ejecutiva", "806",
                hoy.plusDays(4), hoy.plusDays(6), "2 adultos, 1 niño", 1040,
                Reserva.Estado.CONFIRMADA, PREFIJO_CODIGO + 4));
        reservas.add(new Reserva(primerId + 5, 2, "Habitación Superior", "212",
                hoy.minusDays(8), hoy.minusDays(6), "1 adulto", 760,
                Reserva.Estado.COMPLETADA, PREFIJO_CODIGO + 5));
        reservas.add(new Reserva(primerId + 6, 3, "Suite Deluxe", "310",
                hoy.minusDays(12), hoy.minusDays(10), "2 adultos", 900,
                Reserva.Estado.CANCELADA, PREFIJO_CODIGO + 6));
        return reservas;
    }
}
