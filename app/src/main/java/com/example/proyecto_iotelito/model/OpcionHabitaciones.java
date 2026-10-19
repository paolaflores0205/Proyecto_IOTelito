package com.example.proyecto_iotelito.model;

import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Una habitación o combinación de habitaciones capaz de alojar al grupo. */
public class OpcionHabitaciones implements Serializable {

    private final ArrayList<Habitacion> habitaciones;

    public OpcionHabitaciones(List<Habitacion> habitaciones) {
        this.habitaciones = new ArrayList<>(habitaciones);
    }

    public ArrayList<Habitacion> habitaciones() {
        return new ArrayList<>(habitaciones);
    }

    public int cantidadHabitaciones() {
        return habitaciones.size();
    }

    public int capacidadAdultos() {
        int total = 0;
        for (Habitacion habitacion : habitaciones) total += habitacion.adultos;
        return total;
    }

    public int capacidadNinos() {
        int total = 0;
        for (Habitacion habitacion : habitaciones) total += habitacion.ninos;
        return total;
    }

    public int areaTotalM2() {
        int total = 0;
        for (Habitacion habitacion : habitaciones) total += habitacion.areaM2;
        return total;
    }

    public double precioPorNoche() {
        double total = 0;
        for (Habitacion habitacion : habitaciones) total += habitacion.precioNoche;
        return total;
    }

    public String titulo() {
        if (habitaciones.size() == 1) return habitaciones.get(0).tipo;
        return "Combinación de " + habitaciones.size() + " habitaciones";
    }

    public String resumenTipos() {
        StringBuilder texto = new StringBuilder();
        for (Habitacion habitacion : habitaciones) {
            if (texto.length() > 0) texto.append(" + ");
            texto.append(habitacion.tipo);
        }
        return texto.toString();
    }

    public String resumenCodigos() {
        StringBuilder texto = new StringBuilder();
        for (Habitacion habitacion : habitaciones) {
            if (texto.length() > 0) texto.append(", ");
            texto.append(habitacion.nombreCodigo.replaceFirst("(?i)^Hab\\.\\s*", ""));
        }
        return texto.toString();
    }

    public String aforo() {
        int adultos = capacidadAdultos();
        int ninos = capacidadNinos();
        String texto = adultos + (adultos == 1 ? " adulto" : " adultos");
        if (ninos > 0) texto += ", " + ninos + (ninos == 1 ? " niño" : " niños");
        return texto;
    }
}
