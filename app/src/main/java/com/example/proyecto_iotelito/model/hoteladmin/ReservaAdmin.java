package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva vista desde el lado del administrador del hotel. El {@link EstadoCheckout}
 * determina en qué chip aparece (Próximas / Hospedados / Checkout pendiente) y qué
 * acciones se ofrecen. {@link #estado} no es final: al confirmar el cobro pasa a
 * FINALIZADA en memoria.
 */
public class ReservaAdmin implements Serializable {

    public enum EstadoCheckout {
        PROXIMA, HOSPEDADO, CHECKOUT_PENDIENTE, FINALIZADA
    }

    public final int id;
    public final int hotelId;
    public final String huespedNombre;
    public final String huespedDoc;
    public final String habitacion;
    public final String rangoFechas;
    public final int noches;
    public final double precioAlojamiento;
    public final boolean tieneTaxi;
    public final List<ItemConsumo> consumos;
    public EstadoCheckout estado;

    public ReservaAdmin(int id, int hotelId, String huespedNombre, String huespedDoc,
                        String habitacion, String rangoFechas, int noches,
                        double precioAlojamiento, boolean tieneTaxi,
                        EstadoCheckout estado, List<ItemConsumo> consumos) {
        this.id = id;
        this.hotelId = hotelId;
        this.huespedNombre = huespedNombre;
        this.huespedDoc = huespedDoc;
        this.habitacion = habitacion;
        this.rangoFechas = rangoFechas;
        this.noches = noches;
        this.precioAlojamiento = precioAlojamiento;
        this.tieneTaxi = tieneTaxi;
        this.estado = estado;
        this.consumos = consumos != null ? consumos : new ArrayList<>();
    }

    /** Suma del alojamiento más todos los consumos y cobros registrados. */
    public double total() {
        double total = precioAlojamiento;
        for (ItemConsumo item : consumos) {
            total += item.monto;
        }
        return total;
    }
}
