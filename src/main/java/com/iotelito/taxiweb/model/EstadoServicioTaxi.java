package com.iotelito.taxiweb.model;

/** Estado del servicio desde que un taxista acepta el pedido. */
public enum EstadoServicioTaxi {
    ASIGNADO,
    EN_CAMINO,
    EN_TRASLADO,
    FINALIZADO,
    CANCELADO;

    /** En curso: el taxista no puede aceptar otro pedido. */
    public boolean activo() {
        return this == ASIGNADO || this == EN_CAMINO || this == EN_TRASLADO;
    }
}
