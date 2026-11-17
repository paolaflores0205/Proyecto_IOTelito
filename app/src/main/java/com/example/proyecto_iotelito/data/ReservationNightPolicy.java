package com.example.proyecto_iotelito.data;

import java.time.LocalDate;
import java.util.*;

/** El intervalo de hospedaje incluye ingreso y excluye salida. */
public final class ReservationNightPolicy {
    private ReservationNightPolicy() { }
    public static List<LocalDate> noches(LocalDate entrada,LocalDate salida){
        if(entrada==null||salida==null||!entrada.isBefore(salida))
            throw new IllegalArgumentException("La salida debe ser posterior al ingreso.");
        List<LocalDate> result=new ArrayList<>();
        for(LocalDate day=entrada;day.isBefore(salida);day=day.plusDays(1))result.add(day);
        return result;
    }
    public static boolean bloquea(String estado,LocalDate entrada,LocalDate salida,LocalDate noche){
        boolean vigente=Arrays.asList("CONFIRMADA","ACTIVA","EN_CHECKOUT","PENDIENTE_PAGO").contains(estado);
        if(!vigente){
            if(Arrays.asList("FINALIZADA","COMPLETADA","CANCELADA").contains(estado))return false;
            throw new IllegalArgumentException("No se pudo validar el estado de una reserva anterior.");
        }
        if(entrada==null||salida==null||!entrada.isBefore(salida))
            throw new IllegalArgumentException("No se pudieron validar las fechas de una reserva anterior.");
        return !noche.isBefore(entrada)&&noche.isBefore(salida);
    }
}
