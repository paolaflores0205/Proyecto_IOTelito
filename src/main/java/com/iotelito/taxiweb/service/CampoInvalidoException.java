package com.iotelito.taxiweb.service;

/** Error de un campo concreto del registro (por ejemplo, un correo o una placa ya registrados). */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
