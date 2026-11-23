package com.example.proyecto_iotelito.data;

import java.util.Map;

/** Comprobante de demostración persistido por el administrador, nunca calculado por una pantalla cliente. */
public final class ComprobanteCheckout {
    public final double base, extra, total;
    /** Servicios adicionales consumidos (0 en comprobantes anteriores) y su detalle. */
    public final double servicios;
    public final java.util.List<com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine> lineasServicios;
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
        servicios = data.get("servicesTotal") instanceof Number ? ((Number)data.get("servicesTotal")).doubleValue() : 0;
        lineasServicios = com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine.list(data.get("services"));
        if (!Double.isFinite(base) || !Double.isFinite(extra) || !Double.isFinite(total) || !Double.isFinite(servicios)
                || base < 0 || extra < 0 || servicios < 0
                || Math.abs(base + extra + servicios - total) > 0.001) throw new IllegalArgumentException("Comprobante inválido.");
        motivo = String.valueOf(data.getOrDefault("reason", ""));
        observacion = String.valueOf(data.getOrDefault("observation", ""));
        tarjeta = data.containsKey("cardLast4") ? data.get("cardType") + " terminada en " + data.get("cardLast4") + " · Prueba" : "No informada · Comprobante anterior";
        administrador = String.valueOf(data.getOrDefault("createdByUid", ""));
        this.fecha = fecha;
    }
    /** Todo lo cobrado además del alojamiento: servicios consumidos y cargo por daños. */
    public double adicionales() { return servicios + extra; }
    /** Texto para el cliente con cada servicio y, si hubo, el motivo del cargo adicional. */
    public String detalleAdicionales() {
        StringBuilder texto = new StringBuilder();
        for (com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine linea : lineasServicios)
            texto.append(linea.name).append(" × ").append(linea.quantity)
                    .append(String.format(java.util.Locale.US, ": S/ %,.2f", linea.amount())).append('\n');
        if (extra > 0) texto.append("Cargo adicional — Motivo: ").append(motivo).append('\n').append("Observación: ").append(observacion);
        String resultado = texto.toString().trim();
        return resultado.isEmpty() ? "Sin cargos adicionales." : resultado;
    }
}
