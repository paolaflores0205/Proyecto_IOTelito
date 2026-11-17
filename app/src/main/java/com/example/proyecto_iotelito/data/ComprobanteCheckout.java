package com.example.proyecto_iotelito.data;

import java.util.Map;

/** Comprobante de demostración persistido por el administrador, nunca calculado por una pantalla cliente. */
public final class ComprobanteCheckout {
    public final double base, extra, total;
    public final String motivo, observacion, tarjeta, administrador;
    public final long fecha;
    public static ComprobanteCheckout deReserva(com.example.proyecto_iotelito.model.Reserva reserva,
            Map<String,Object> data, long fecha, String uid) {
        if(!CheckoutPolicy.finalizada(reserva.estado.name()) || !uid.equals(data.get("clientUid"))
                || !String.valueOf(reserva.id).equals(data.get("reservationId"))
                || !(data.get("hotelId") instanceof Number)
                || ((Number)data.get("hotelId")).doubleValue()!=reserva.hotelId || fecha<=0)
            throw new IllegalArgumentException("El comprobante no corresponde a esta reserva finalizada.");
        ComprobanteCheckout receipt=new ComprobanteCheckout(data,fecha);
        if(Math.abs(receipt.base-reserva.precioTotal)>0.001)
            throw new IllegalArgumentException("El alojamiento del comprobante no coincide con la reserva.");
        return receipt;
    }
    public ComprobanteCheckout(Map<String,Object> data, long fecha) {
        base = ((Number)data.get("base")).doubleValue(); extra = ((Number)data.get("extra")).doubleValue();
        total = ((Number)data.get("total")).doubleValue();
        if (!Double.isFinite(base) || !Double.isFinite(extra) || !Double.isFinite(total) || base < 0 || extra < 0
                || Math.abs(base + extra - total) > 0.001) throw new IllegalArgumentException("Comprobante inválido.");
        motivo = String.valueOf(data.getOrDefault("reason", ""));
        observacion = String.valueOf(data.getOrDefault("observation", ""));
        tarjeta = data.containsKey("cardLast4") ? data.get("cardType") + " terminada en " + data.get("cardLast4") + " · Prueba" : "No informada · Comprobante anterior";
        administrador = String.valueOf(data.getOrDefault("createdByUid", ""));
        this.fecha = fecha;
    }
}
