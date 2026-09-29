package com.example.proyecto_iotelito.model.taxista;

/**
 * Solicitud de traslado hotel → aeropuerto disponible para el taxista
 * (estado SOLICITADO, aún sin taxista asignado).
 */
public class SolicitudServicio {
    public final int id;
    /** Minutos que faltan para el recojo; sirve para el filtro "Menos de 10 min". */
    public final int minutosEspera;
    public final String hotel;
    public final String destino;
    public final String fechaHora;
    public final String pasajeroNombre;
    public final String pasajeroIniciales;
    public final String distanciaTiempo;
    /** Tarifa del servicio, cubierta por la reserva del hotel (no la paga el pasajero). */
    public final double tarifa;
    /** Tramo hotel → aeropuerto (el servicio en sí), a diferencia de distanciaTiempo, que es el tramo hasta el hotel. */
    public final String viajeDistancia;
    public final String viajeDuracion;

    public SolicitudServicio(int id, int minutosEspera, String hotel, String destino,
                             String fechaHora, String pasajeroNombre, String pasajeroIniciales,
                             String distanciaTiempo, double tarifa,
                             String viajeDistancia, String viajeDuracion) {
        this.id = id;
        this.minutosEspera = minutosEspera;
        this.hotel = hotel;
        this.destino = destino;
        this.fechaHora = fechaHora;
        this.pasajeroNombre = pasajeroNombre;
        this.pasajeroIniciales = pasajeroIniciales;
        this.distanciaTiempo = distanciaTiempo;
        this.tarifa = tarifa;
        this.viajeDistancia = viajeDistancia;
        this.viajeDuracion = viajeDuracion;
    }
}
