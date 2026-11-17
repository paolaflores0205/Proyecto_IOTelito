package com.iotelito.taxiweb.model;

/** Estado del pedido. PENDIENTE equivale a SOLICITADO en la app y en la API. */
public enum EstadoSolicitudTaxi {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA,
    FINALIZADA
}
