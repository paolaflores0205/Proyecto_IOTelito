package com.example.proyecto_iotelito.data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Restricción temporal compartida por los calendarios y el guardado de reservas. */
public final class BookingDatePolicy {
    // Temporal para probar reservas vencidas, checkout y taxi. Restaurar a false después de las pruebas.
    public static final boolean PERMITIR_FECHAS_PASADAS_PARA_PRUEBAS = true;
    private BookingDatePolicy() { }
    public static long inicioCalendarioMillis() {
        LocalDate earliest = PERMITIR_FECHAS_PASADAS_PARA_PRUEBAS
                ? LocalDate.of(1900,1,1) : LocalDate.now(ZoneId.of("America/Lima"));
        return earliest.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
    public static void validar(LocalDate entrada, LocalDate salida) {
        validar(entrada,salida,LocalDate.now(ZoneId.of("America/Lima")),PERMITIR_FECHAS_PASADAS_PARA_PRUEBAS);
    }
    static void validar(LocalDate entrada, LocalDate salida, LocalDate hoy, boolean permitirPasadas) {
        if(entrada==null||salida==null||!salida.isAfter(entrada))
            throw new IllegalArgumentException("La salida debe ser posterior al ingreso, con al menos una noche.");
        if(!permitirPasadas&&entrada.isBefore(hoy))
            throw new IllegalArgumentException("El ingreso no puede ser anterior a hoy.");
    }
}
