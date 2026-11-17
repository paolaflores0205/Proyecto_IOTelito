package com.iotelito.taxiweb.model;

import java.util.List;
import java.util.Locale;

/**
 * Aeropuertos de destino del contrato docs/api-taxis-v1.yaml, con los mismos nombres y
 * ciudades que validan las reglas de Firestore publicadas el 16/11/2026.
 */
public enum CodigoAeropuerto {
    LIM("Aeropuerto Internacional Jorge Chávez", "Callao", "lima", "lima metropolitana", "callao"),
    CUZ("Aeropuerto Internacional Alejandro Velasco Astete", "Cusco", "cusco", "cuzco"),
    AQP("Aeropuerto Internacional Alfredo Rodríguez Ballón", "Arequipa", "arequipa"),
    TRU("Aeropuerto Internacional Capitán FAP Carlos Martínez de Pinillos", "Trujillo", "trujillo"),
    PIO("Aeropuerto Capitán FAP Renán Elías Olivera", "Pisco", "paracas", "pisco");

    private final String nombre;
    private final String ciudad;
    private final List<String> ciudadesHotel;

    CodigoAeropuerto(String nombre, String ciudad, String... ciudadesHotel) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.ciudadesHotel = List.of(ciudadesHotel);
    }

    public String getNombre() { return nombre; }

    public String getCiudad() { return ciudad; }

    public List<String> getCiudadesHotel() { return ciudadesHotel; }

    /** La ciudad del hotel, en minúsculas y sin espacios al borde, debe estar en la lista. */
    public boolean atiende(String ciudadHotel) {
        return ciudadHotel != null && ciudadesHotel.contains(ciudadHotel.trim().toLowerCase(Locale.ROOT));
    }
}
