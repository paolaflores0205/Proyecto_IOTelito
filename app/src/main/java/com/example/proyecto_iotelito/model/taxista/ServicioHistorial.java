package com.example.proyecto_iotelito.model.taxista;

import com.example.proyecto_iotelito.ui.taxista.EstadoServicio;

/**
 * Traslado pasado del taxista (FINALIZADO o CANCELADO) mostrado en la pestaña Historial.
 */
public class ServicioHistorial {
    public final int id;
    public final String hora;
    public final EstadoServicio estado;
    public final String origen;
    public final String destino;
    public final String pasajero;
    public final String vehiculo;
    /** Solo en servicios finalizados; null si fue cancelado. */
    public final String distancia;
    /** Solo en servicios finalizados; null si fue cancelado. */
    public final String duracion;
    /** Solo en servicios cancelados; null si fue finalizado. */
    public final String motivoCancelacion;
    /** Tarifa del servicio, pagada por el hotel; 0 si fue cancelado. */
    public final double monto;

    public ServicioHistorial(int id, String hora, EstadoServicio estado, String origen, String destino,
                             String pasajero, String vehiculo, String distancia, String duracion,
                             String motivoCancelacion, double monto) {
        this.id = id;
        this.hora = hora;
        this.estado = estado;
        this.origen = origen;
        this.destino = destino;
        this.pasajero = pasajero;
        this.vehiculo = vehiculo;
        this.distancia = distancia;
        this.duracion = duracion;
        this.motivoCancelacion = motivoCancelacion;
        this.monto = monto;
    }

    public boolean fueCancelado() {
        return estado == EstadoServicio.CANCELADO;
    }

    public String ruta() {
        return origen + " → " + destino;
    }

    /** Línea corta para la tarjeta: distancia y tiempo, o el motivo si se canceló. */
    public String detalle() {
        return fueCancelado() ? motivoCancelacion : distancia + " · " + duracion;
    }
}
