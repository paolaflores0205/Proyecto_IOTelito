package com.example.proyecto_iotelito.model.superadmin;

import java.time.LocalDate;

/** Comprobante de checkout confirmado en servidor. El pago del proyecto es simulado. */
public final class HotelCheckout {
    public final String reservationId;
    public final int hotelId;
    public final LocalDate date;
    public final double base, extra, total;

    public HotelCheckout(String reservationId, int hotelId, LocalDate date,
                         double base, double extra, double total) {
        if (reservationId == null || reservationId.isEmpty() || hotelId <= 0 || date == null
                || !Double.isFinite(base) || !Double.isFinite(extra) || !Double.isFinite(total)
                || base < 0 || extra < 0 || total < 0 || Math.abs(base + extra - total) > 0.005)
            throw new IllegalArgumentException("Comprobante de checkout inválido.");
        this.reservationId = reservationId; this.hotelId = hotelId; this.date = date;
        this.base = base; this.extra = extra; this.total = total;
    }
}
