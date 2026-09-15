package com.example.proyecto_iotelito.ui.taxista;

/**
 * Estados del ciclo de vida de un servicio de taxi, del lado del conductor.
 */
public enum EstadoServicio {
    SOLICITADO,
    ASIGNADO,
    EN_CAMINO,
    EN_TRASLADO,
    FINALIZADO,
    CANCELADO
}
