package com.example.proyecto_iotelito.data.taxista;

/** Política de transiciones remotas, independiente de las pantallas del cliente. */
public final class TaxiRemoteState {
    private TaxiRemoteState() { }
    public static boolean permite(String actual, String siguiente) {
        return ("SOLICITADO".equals(actual) && "ASIGNADO".equals(siguiente))
                || ("ASIGNADO".equals(actual) && "EN_CAMINO".equals(siguiente))
                || ("EN_CAMINO".equals(actual) && "EN_TRASLADO".equals(siguiente))
                || ("EN_TRASLADO".equals(actual) && "FINALIZADO".equals(siguiente));
    }
    public static String paraCliente(String estado) {
        return "EN_TRASLADO".equals(estado) ? "EN_CAMINO" : estado;
    }
}
