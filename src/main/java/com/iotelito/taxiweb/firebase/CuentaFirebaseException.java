package com.iotelito.taxiweb.firebase;

/** No se pudo crear o bloquear la cuenta de Firebase; el mensaje es para el superadmin. */
public class CuentaFirebaseException extends RuntimeException {

    public CuentaFirebaseException(String mensaje) {
        super(mensaje);
    }
}
