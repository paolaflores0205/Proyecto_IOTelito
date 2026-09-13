package com.example.proyecto_iotelito.model;

/**
 * Mensaje del chat estático con el hotel (sin backend todavía).
 */
public class Mensaje {

    public final String texto;
    public final boolean deHuesped;
    public final String hora;

    public Mensaje(String texto, boolean deHuesped, String hora) {
        this.texto = texto;
        this.deHuesped = deHuesped;
        this.hora = hora;
    }
}
