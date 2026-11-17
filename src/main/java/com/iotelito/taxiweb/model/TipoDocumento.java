package com.iotelito.taxiweb.model;

/** Documento de identidad del taxista: el plan acepta DNI, pasaporte y carné de extranjería. */
public enum TipoDocumento {
    DNI("DNI", "[0-9]{8}"),
    PASAPORTE("Pasaporte", "[A-Za-z0-9]{6,12}"),
    CARNET_EXTRANJERIA("Carné de extranjería", "[A-Za-z0-9]{9,12}");

    private final String etiqueta;
    private final String formato;

    TipoDocumento(String etiqueta, String formato) {
        this.etiqueta = etiqueta;
        this.formato = formato;
    }

    public String getEtiqueta() { return etiqueta; }

    /** Mismas reglas que usa la app para el documento del cliente. */
    public boolean valida(String numero) {
        return numero != null && numero.matches(formato);
    }
}
