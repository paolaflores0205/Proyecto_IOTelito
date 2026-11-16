package com.example.proyecto_iotelito.data;

import java.util.Map;

/** Comprobante de demostración persistido por el administrador, nunca calculado por una pantalla cliente. */
public final class ComprobanteCheckout {
    public final double base, extra, total;
    public final String motivo, observacion, tarjeta, administrador;
    public final long fecha;
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
