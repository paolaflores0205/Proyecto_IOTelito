package com.example.proyecto_iotelito.model;

/** Aviso del cliente guardado en la bandeja local de la aplicación. */
public class NotificacionCliente {

    public enum Tipo { RESERVA, PAGO, TAXI }

    public final int id;
    public final Tipo tipo;
    public final String titulo;
    public final long creadaEn;
    public final boolean leida;
    public final int reservaId;

    public NotificacionCliente(int id, Tipo tipo, String titulo, long creadaEn,
                               boolean leida, int reservaId) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.creadaEn = creadaEn;
        this.leida = leida;
        this.reservaId = reservaId;
    }
}
