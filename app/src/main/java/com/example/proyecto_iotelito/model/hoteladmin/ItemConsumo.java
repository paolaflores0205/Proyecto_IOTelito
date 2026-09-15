package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;

/**
 * Línea del detalle de consumo en el checkout: concepto y monto en soles.
 * Se usa para el alojamiento, los servicios consumidos y los cobros por daños.
 */
public class ItemConsumo implements Serializable {

    public final String concepto;
    public final double monto;
    public final String motivo;
    public final String observacion;

    public ItemConsumo(String concepto, double monto) {
        this(concepto, monto, "", "");
    }

    public ItemConsumo(String concepto, double monto, String motivo, String observacion) {
        this.concepto = concepto;
        this.monto = monto;
        this.motivo = motivo;
        this.observacion = observacion;
    }
}
