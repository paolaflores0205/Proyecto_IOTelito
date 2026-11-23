package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;

import java.util.Locale;

/**
 * Beneficio de taxi gratuito al aeropuerto. Cada hotel define su monto mínimo de alojamiento
 * (enunciado: "un monto mínimo establecido por cada hotel"); si aún no lo configuró se usa
 * S/ 1000. Se pide con el checkout solicitado o finalizado, sin importar el número de noches.
 */
public final class TaxiBenefitPolicy {
    /** Monto que aplica cuando el hotel no configuró el suyo. */
    public static final double MONTO_MINIMO=1000.0;
    /** Tope razonable para lo que el administrador del hotel puede configurar. */
    public static final double MONTO_MAXIMO=100000.0;
    private TaxiBenefitPolicy() { }

    /** Monto mínimo vigente para el hotel (el suyo si es válido; si no, el monto por defecto). */
    public static double minimo(Hotel hotel){
        return minimo(hotel==null?null:hotel.taxiMinimo);
    }
    public static double minimo(Double configurado){
        return configurado!=null&&valido(configurado)?configurado:MONTO_MINIMO;
    }
    /** Lo que el administrador del hotel puede guardar: de 0 (siempre gratis) a {@link #MONTO_MAXIMO}. */
    public static boolean valido(double monto){
        return Double.isFinite(monto)&&monto>=0&&monto<=MONTO_MAXIMO;
    }
    public static String texto(Hotel hotel){
        double monto=minimo(hotel);
        return monto==Math.rint(monto)?String.format(Locale.ROOT,"S/ %.0f",monto):String.format(Locale.ROOT,"S/ %.2f",monto);
    }

    public static boolean cumpleMonto(double importe){
        return cumpleMonto(importe,MONTO_MINIMO);
    }
    public static boolean cumpleMonto(double importe,double minimo){
        return Double.isFinite(importe)&&importe>0&&importe>=minimo;
    }
    public static boolean cumpleMonto(Reserva reserva){
        return cumpleMonto(reserva,null);
    }
    public static boolean cumpleMonto(Reserva reserva,Hotel hotel){
        return reserva!=null&&cumpleMonto(reserva.precioTotal,minimo(hotel));
    }

    public static boolean puedeSolicitar(Reserva reserva){
        return puedeSolicitar(reserva,null);
    }
    public static boolean puedeSolicitar(Reserva reserva,Hotel hotel){
        return reserva!=null&&puedeSolicitar(reserva.estado.name(),reserva.precioTotal,minimo(hotel));
    }
    public static boolean puedeSolicitar(String estado,double importe){
        return puedeSolicitar(estado,importe,MONTO_MINIMO);
    }
    public static boolean puedeSolicitar(String estado,double importe,double minimo){
        return cumpleMonto(importe,minimo)&&("EN_CHECKOUT".equals(estado)||CheckoutPolicy.finalizada(estado));
    }
}
