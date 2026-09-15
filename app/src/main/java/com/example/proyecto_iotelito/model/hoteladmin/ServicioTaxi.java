package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;

/**
 * Servicio de taxi gratuito al aeropuerto asociado a una reserva. Los estados
 * siguen los definidos en el plan de proyecto. {@link #estado} no es final: el
 * admin sigue su avance en tiempo real.
 */
public class ServicioTaxi implements Serializable {

    public enum Estado {
        SOLICITADO, ASIGNADO, EN_CAMINO, EN_TRASLADO, FINALIZADO
    }

    public final String huespedNombre;
    public final String habitacion;
    public final String destino;
    public final String conductor;
    public final String vehiculo;
    public final String placa;
    /** Teléfono del conductor del taxi. */
    public final String telefono;
    /** Teléfono del huésped que solicitó el traslado. */
    public final String huespedTelefono;
    public final double rating;
    public Estado estado;

    public ServicioTaxi(String huespedNombre, String habitacion, String destino,
                        String conductor, String vehiculo, String placa, String telefono,
                        String huespedTelefono, double rating, Estado estado) {
        this.huespedNombre = huespedNombre;
        this.habitacion = habitacion;
        this.destino = destino;
        this.conductor = conductor;
        this.vehiculo = vehiculo;
        this.placa = placa;
        this.telefono = telefono;
        this.huespedTelefono = huespedTelefono;
        this.rating = rating;
        this.estado = estado;
    }
}
