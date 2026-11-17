package com.example.proyecto_iotelito.data.taxiapi;

/** Errores del contrato; status=0 indica que no hubo respuesta HTTP. */
public final class TaxiApiException extends RuntimeException {
    public enum Codigo {
        VALIDACION,
        AEROPUERTO_NO_CORRESPONDE,
        QR_INVALIDO,
        NO_AUTENTICADO,
        PROHIBIDO,
        TAXISTA_NO_HABILITADO,
        NO_ENCONTRADO,
        TAXISTA_NO_REGISTRADO,
        SERVICIO_NO_DISPONIBLE,
        SERVICIO_ACTIVO,
        TAXISTA_NO_DISPONIBLE,
        ESTADO_INVALIDO,
        YA_CALIFICADO,
        NO_DISPONIBLE
    }

    public final int status;
    public final Codigo codigo;

    public TaxiApiException(int status, Codigo codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static TaxiApiException noDisponible() {
        return new TaxiApiException(
                0, Codigo.NO_DISPONIBLE, "El servicio de taxis no está disponible");
    }
}
