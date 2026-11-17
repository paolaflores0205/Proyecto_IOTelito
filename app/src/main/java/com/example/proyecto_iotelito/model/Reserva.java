package com.example.proyecto_iotelito.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Reserva del cliente (datos estáticos). El {@link Estado} determina si
 * aparece en la pestaña "Activas" o "Finalizadas" de Mis reservas; dentro
 * de "Activas", la fecha determina si se agrupa como "Próxima estadía" o
 * "En curso" (no existe un estado de "pago pendiente" visible para el
 * cliente: toda reserva activa ya está confirmada).
 */
public class Reserva implements Serializable {

    private static final String[] MESES_ABREV = {
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"
    };

    public enum Estado {
        CONFIRMADA, ACTIVA, EN_CHECKOUT, FINALIZADA, PENDIENTE_PAGO, COMPLETADA, CANCELADA;

        public static Estado desdeFirestore(String estado) { return valueOf(estado); }
    }

    public final int id;
    public final int hotelId;
    public final String roomName;
    public final String roomNumber;
    public final LocalDate fechaEntrada;
    public final LocalDate fechaSalida;
    public final String huespedes;
    public final int noches;
    public final double precioTotal;
    public Estado estado;
    public final String codigo;
    private int cantidadHabitaciones;

    /** El inventario remoto prevalece sobre los códigos de presentación. */
    public void setCantidadHabitaciones(int cantidad) {
        if (cantidad < 1) throw new IllegalArgumentException("La reserva requiere habitaciones.");
        cantidadHabitaciones = cantidad;
    }

    public int cantidadHabitaciones() {
        if (cantidadHabitaciones > 0) return cantidadHabitaciones;
        if (roomNumber == null || roomNumber.trim().isEmpty()) return 1;
        return Math.max(1, roomNumber.split(",").length);
    }
    public com.example.proyecto_iotelito.data.TarjetaSimulada tarjeta;
    public Integer valoracion;
    public String observaciones;
    public com.example.proyecto_iotelito.data.ComprobanteCheckout comprobante;

    /** Conserva precioTotal como alojamiento; el total liquidado solo proviene del comprobante. */
    public double importeMostrado() {
        return com.example.proyecto_iotelito.data.CheckoutPolicy.finalizada(estado.name()) && comprobante != null
                ? comprobante.total : precioTotal;
    }

    public Reserva(int id, int hotelId, String roomName, String roomNumber, LocalDate fechaEntrada, LocalDate fechaSalida,
                    String huespedes, double precioTotal, Estado estado, String codigo) {
        this.id = id;
        this.hotelId = hotelId;
        this.roomName = roomName;
        this.roomNumber = roomNumber;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.huespedes = huespedes;
        this.noches = (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
        this.precioTotal = precioTotal;
        this.estado = estado;
        this.codigo = codigo;
    }

    public boolean esActiva() {
        return estado == Estado.CONFIRMADA || estado == Estado.ACTIVA || estado == Estado.EN_CHECKOUT || estado == Estado.PENDIENTE_PAGO;
    }

    /** La estadía ya empezó y todavía no termina. */
    public boolean estaEnCurso() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaEntrada) && !hoy.isAfter(fechaSalida);
    }

    /** La estadía todavía no empieza. */
    public boolean esProxima() {
        return LocalDate.now().isBefore(fechaEntrada);
    }

    /** El cliente puede solicitar el checkout desde el día de salida en Lima. */
    public boolean puedeHacerCheckout() {
        return com.example.proyecto_iotelito.data.CheckoutPolicy.puedeSolicitar(estado.name(), fechaSalida, LocalDate.now(java.time.ZoneId.of("America/Lima")));
    }

    public boolean puedeSolicitarCheckout() { return puedeHacerCheckout(); }

    public String rangoFechasTexto() {
        String mesEntrada = MESES_ABREV[fechaEntrada.getMonthValue() - 1];
        String mesSalida = MESES_ABREV[fechaSalida.getMonthValue() - 1];
        if (fechaEntrada.getMonthValue() == fechaSalida.getMonthValue()) {
            return fechaEntrada.getDayOfMonth() + " – " + fechaSalida.getDayOfMonth() + " " + mesEntrada;
        }
        return fechaEntrada.getDayOfMonth() + " " + mesEntrada + " – " + fechaSalida.getDayOfMonth() + " " + mesSalida;
    }
}
