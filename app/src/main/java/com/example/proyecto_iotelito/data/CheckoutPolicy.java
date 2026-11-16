package com.example.proyecto_iotelito.data;

import java.time.LocalDate;

/** Reglas de negocio compartidas por las pantallas y transacciones de checkout. */
public final class CheckoutPolicy {
    private CheckoutPolicy() { }
    public static void validarOpinion(int estrellas, String observaciones) {
        if (estrellas < 1 || estrellas > 5) throw new IllegalArgumentException("Selecciona de 1 a 5 estrellas.");
        if (observaciones == null || observaciones.trim().isEmpty() || observaciones.trim().length() > 4000)
            throw new IllegalArgumentException("Escribe tus observaciones de la estadía (máximo 4000 caracteres).");
    }
    public static boolean puedeSolicitar(String estado, LocalDate salida, LocalDate hoy) {
        return ("CONFIRMADA".equals(estado) || "ACTIVA".equals(estado)) && !hoy.isBefore(salida);
    }
    public static boolean finalizada(String estado) { return "FINALIZADA".equals(estado) || "COMPLETADA".equals(estado); }
}
