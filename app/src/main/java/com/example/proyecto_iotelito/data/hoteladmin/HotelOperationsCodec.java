package com.example.proyecto_iotelito.data.hoteladmin;

import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Conversión y cálculos sin dependencia de Android ni datos de demostración. */
public final class HotelOperationsCodec {
    private HotelOperationsCodec() { }
    public static ReservaAdmin decode(String id, Map<String,Object> data, int hotelId, LocalDate today) {
        int number=((Number)data.get("id")).intValue();
        if(!id.equals(String.valueOf(number))||((Number)data.get("hotelId")).intValue()!=hotelId)
            throw new IllegalArgumentException("Reserva de otro hotel o ID inconsistente");
        LocalDate entrada=LocalDate.parse((String)data.get("fechaEntrada")),salida=LocalDate.parse((String)data.get("fechaSalida"));
        double total=((Number)data.get("precioTotal")).doubleValue();
        if(!entrada.isBefore(salida)||!Double.isFinite(total)||total<0)throw new IllegalArgumentException("Reserva inválida");
        String estado=(String)data.get("estado");
        if(!Arrays.asList("CONFIRMADA","COMPLETADA","CANCELADA","PENDIENTE_PAGO").contains(estado))throw new IllegalArgumentException("Estado inválido");
        if("CANCELADA".equals(estado)||"PENDIENTE_PAGO".equals(estado))return null;
        String codigo=String.valueOf(data.get("codigo"));
        String nombre=data.get("clientName") instanceof String?(String)data.get("clientName"):"Huésped · "+codigo;
        if(nombre.trim().isEmpty())nombre="Huésped · "+codigo;
        ReservaAdmin row=new ReservaAdmin(number,hotelId,nombre,"Documento no informado · "+codigo,
                String.valueOf(data.get("roomName"))+" · "+String.valueOf(data.get("roomNumber")),
                entrada+" — "+salida,(int)ChronoUnit.DAYS.between(entrada,salida),total,false,
                status(estado,entrada,salida,today),new ArrayList<>());
        row.fechaEntrada=entrada;row.fechaSalida=salida;
        Object ids=data.get("roomIds");row.cantidadHabitaciones=ids instanceof List?Math.max(1,((List<?>)ids).size()):1;
        return row;
    }
    public static ReservaAdmin.EstadoCheckout status(String estado,LocalDate entrada,LocalDate salida,LocalDate hoy) {
        if("COMPLETADA".equals(estado))return ReservaAdmin.EstadoCheckout.FINALIZADA;
        if(hoy.isBefore(entrada))return ReservaAdmin.EstadoCheckout.PROXIMA;
        if(!hoy.isBefore(salida))return ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
        return ReservaAdmin.EstadoCheckout.HOSPEDADO;
    }
    public static double total(double base,double extra) {
        if(!Double.isFinite(base)||!Double.isFinite(extra)||base<0||extra<0||extra>1000000
                ||java.math.BigDecimal.valueOf(extra).stripTrailingZeros().scale()>2)
            throw new IllegalArgumentException("El importe debe ser válido y tener como máximo dos decimales.");
        return base+extra;
    }
    public static final class Receipt {
        public final LocalDateTime time;public final double total;
        public Receipt(LocalDateTime time,double total){if(!Double.isFinite(total)||total<0)throw new IllegalArgumentException();this.time=time;this.total=total;}
    }
    public static final class Report {
        public int bookings;public double income;public Double occupancy;public final double[] buckets=new double[4];
    }
    public static Report report(List<ReservaAdmin> reservas,List<Receipt> receipts,int rooms,LocalDate date,String period) {
        LocalDate start="DAY".equals(period)?date:"YEAR".equals(period)?date.withDayOfYear(1):date.withDayOfMonth(1);
        LocalDate end="DAY".equals(period)?start.plusDays(1):"YEAR".equals(period)?start.plusYears(1):start.plusMonths(1);
        long days=ChronoUnit.DAYS.between(start,end),nights=0;Report out=new Report();
        for(ReservaAdmin row:reservas){
            if(row.fechaEntrada==null||row.fechaSalida==null)continue;
            if(!row.fechaEntrada.isBefore(start)&&row.fechaEntrada.isBefore(end))out.bookings++;
            LocalDate a=row.fechaEntrada.isAfter(start)?row.fechaEntrada:start,b=row.fechaSalida.isBefore(end)?row.fechaSalida:end;
            if(a.isBefore(b))nights+=ChronoUnit.DAYS.between(a,b)*row.cantidadHabitaciones;
        }
        if(rooms>0)out.occupancy=100.0*nights/(rooms*days);
        for(Receipt receipt:receipts){LocalDate d=receipt.time.toLocalDate();if(d.isBefore(start)||!d.isBefore(end))continue;
            out.income+=receipt.total;
            int bucket="DAY".equals(period)?receipt.time.getHour()/6:(int)(ChronoUnit.DAYS.between(start,d)*4/days);
            out.buckets[Math.min(3,bucket)]+=receipt.total;
        }
        return out;
    }
}
