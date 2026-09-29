package com.example.proyecto_iotelito.ui.booking;

import android.content.Context;
import android.content.Intent;

import com.example.proyecto_iotelito.R;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Selección temporal de fechas y huéspedes que acompaña todo el flujo de reserva. */
public final class BookingSelection {

    public static final String EXTRA_CHECK_IN_MILLIS = "extra_check_in_millis";
    public static final String EXTRA_CHECK_OUT_MILLIS = "extra_check_out_millis";
    public static final String EXTRA_ADULTOS = "extra_adultos";
    public static final String EXTRA_NINOS = "extra_ninos";

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;
    private static final Locale LOCALE_PE = new Locale.Builder().setLanguage("es").setRegion("PE").build();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM", LOCALE_PE);

    public final long checkInMillis;
    public final long checkOutMillis;
    public final int adultos;
    public final int ninos;

    public BookingSelection(long checkInMillis, long checkOutMillis, int adultos, int ninos) {
        this.checkInMillis = checkInMillis;
        this.checkOutMillis = Math.max(checkOutMillis, checkInMillis + ONE_DAY_MS);
        this.adultos = Math.max(1, adultos);
        this.ninos = Math.max(0, ninos);
    }

    public static BookingSelection from(Intent intent) {
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        long entradaDefecto = hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long salidaDefecto = hoy.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new BookingSelection(
                intent.getLongExtra(EXTRA_CHECK_IN_MILLIS, entradaDefecto),
                intent.getLongExtra(EXTRA_CHECK_OUT_MILLIS, salidaDefecto),
                intent.getIntExtra(EXTRA_ADULTOS, 2),
                intent.getIntExtra(EXTRA_NINOS, 1));
    }

    public void putInto(Intent intent) {
        intent.putExtra(EXTRA_CHECK_IN_MILLIS, checkInMillis);
        intent.putExtra(EXTRA_CHECK_OUT_MILLIS, checkOutMillis);
        intent.putExtra(EXTRA_ADULTOS, adultos);
        intent.putExtra(EXTRA_NINOS, ninos);
    }

    public LocalDate fechaEntrada() {
        return Instant.ofEpochMilli(checkInMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    public LocalDate fechaSalida() {
        return Instant.ofEpochMilli(checkOutMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    public int noches() {
        return Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(fechaEntrada(), fechaSalida()));
    }

    public String textoFechas() {
        String rango = DATE_FORMATTER.format(fechaEntrada()) + " – " + DATE_FORMATTER.format(fechaSalida());
        return rango + " (" + noches() + (noches() == 1 ? " noche)" : " noches)");
    }

    public String textoHuespedes(Context context) {
        String textoAdultos = context.getResources().getQuantityString(R.plurals.adultos_plural, adultos, adultos);
        if (ninos == 0) {
            return textoAdultos;
        }
        return textoAdultos + ", "
                + context.getResources().getQuantityString(R.plurals.ninos_plural, ninos, ninos);
    }
}
