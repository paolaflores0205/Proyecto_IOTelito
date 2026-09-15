package com.example.proyecto_iotelito.model;

/**
 * Modelo de conversación para la pestaña Mensajes.
 * Representa la interacción con los distintos hoteles o estadías del cliente.
 */
public class Conversacion {

    public enum Categoria {
        TODOS,
        HOSPEDADOS,
        POR_LLEGAR,
        PASADOS
    }

    public final int id;
    public final int hotelId;
    public final String nombreContacto;
    public final String habitacion;
    public final String ultimoMensaje;
    public final String hora;
    public int mensajesSinLeer;
    public final Categoria categoria;
    public final String avatarInicial;

    public Conversacion(int id, int hotelId, String nombreContacto, String habitacion,
                        String ultimoMensaje, String hora, int mensajesSinLeer,
                        Categoria categoria) {
        this.id = id;
        this.hotelId = hotelId;
        this.nombreContacto = nombreContacto;
        this.habitacion = habitacion;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.mensajesSinLeer = mensajesSinLeer;
        this.categoria = categoria;
        this.avatarInicial = (nombreContacto != null && !nombreContacto.trim().isEmpty())
                ? nombreContacto.trim().substring(0, 1).toUpperCase()
                : "H";
    }

    public void marcarComoLeido() {
        this.mensajesSinLeer = 0;
    }
}
