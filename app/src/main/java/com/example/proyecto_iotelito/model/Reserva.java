package com.example.proyecto_iotelito.model;

import java.io.Serializable;

/**
 * Reserva del cliente (datos estáticos). El {@link Estado} determina si
 * aparece en la pestaña "Activas" o "Historial" de Mis reservas, y qué
 * acciones se ofrecen en el detalle (pagar, chatear).
 */
public class Reserva implements Serializable {

    public enum Estado {
        CONFIRMADA, PENDIENTE_PAGO, COMPLETADA, CANCELADA
    }

    public final int id;
    public final int hotelId;
    public final String roomName;
    public final String rangoFechas;
    public final String huespedes;
    public final int noches;
    public final double precioTotal;
    /** No es final: al pagar una reserva pendiente, su estado pasa a CONFIRMADA en memoria. */
    public Estado estado;
    public final String codigo;

    public Reserva(int id, int hotelId, String roomName, String rangoFechas, String huespedes,
                    int noches, double precioTotal, Estado estado, String codigo) {
        this.id = id;
        this.hotelId = hotelId;
        this.roomName = roomName;
        this.rangoFechas = rangoFechas;
        this.huespedes = huespedes;
        this.noches = noches;
        this.precioTotal = precioTotal;
        this.estado = estado;
        this.codigo = codigo;
    }

    public boolean esActiva() {
        return estado == Estado.CONFIRMADA || estado == Estado.PENDIENTE_PAGO;
    }
}
