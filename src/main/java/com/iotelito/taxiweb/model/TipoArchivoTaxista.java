package com.iotelito.taxiweb.model;

/** Archivos que sube el taxista al registrarse. */
public enum TipoArchivoTaxista {
    FOTO_CONDUCTOR("Foto del conductor"),
    FOTO_VEHICULO("Foto del vehículo"),
    DOCUMENTO_IDENTIDAD("Documento de identidad"),
    LICENCIA("Licencia de conducir"),
    SOAT("SOAT"),
    TARJETA_PROPIEDAD("Tarjeta de propiedad");

    private final String etiqueta;

    TipoArchivoTaxista(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
