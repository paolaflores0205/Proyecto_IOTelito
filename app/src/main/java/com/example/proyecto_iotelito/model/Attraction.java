package com.example.proyecto_iotelito.model;

import java.io.Serializable;

/**
 * Atracción cercana a un hotel, mostrada como tarjeta con foto en el
 * carrusel de "Atracciones cercanas" (ver {@link Hotel}).
 */
public class Attraction implements Serializable {

    public final String name;
    public final String distance;

    public Attraction(String name, String distance) {
        this.name = name;
        this.distance = distance;
    }
}
