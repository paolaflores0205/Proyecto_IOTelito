package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;

/**
 * Conversación de la bandeja de mensajes del hotel. El hilo de mensajes en sí
 * reutiliza el modelo {@link com.example.proyecto_iotelito.model.Mensaje}.
 * {@link #noLeidos} no es final: se pone en 0 al abrir el chat.
 */
public class Conversacion implements Serializable {

    public final int id;
    public final String clienteNombre;
    public final String habitacion;
    public final String ultimoMensaje;
    public final String hora;
    public final String inicial;
    public final boolean hospedado;
    public int noLeidos;

    public Conversacion(int id, String clienteNombre, String habitacion, String ultimoMensaje,
                        String hora, String inicial, boolean hospedado, int noLeidos) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.habitacion = habitacion;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.inicial = inicial;
        this.hospedado = hospedado;
        this.noLeidos = noLeidos;
    }
}
