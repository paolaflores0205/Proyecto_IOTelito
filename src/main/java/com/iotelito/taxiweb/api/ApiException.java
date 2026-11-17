package com.iotelito.taxiweb.api;

import org.springframework.http.HttpStatus;

/** Error de la API con el código estable del contrato y un mensaje para mostrar. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }

    public String getCodigo() { return codigo; }

    public static ApiException validacion(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, "PROHIBIDO", mensaje);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException conflicto(String codigo, String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ApiException noDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "NO_DISPONIBLE",
                "El servicio de taxis no está disponible. Intenta de nuevo en unos minutos.");
    }
}
