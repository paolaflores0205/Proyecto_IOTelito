package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Reserva;

/** Beneficio de taxi desde S/ 1000 de alojamiento, independiente del número de noches. */
public final class TaxiBenefitPolicy {
    public static final double MONTO_MINIMO=1000.0;
    private TaxiBenefitPolicy() { }
    public static boolean cumpleMonto(double importe){
        return Double.isFinite(importe)&&importe>=MONTO_MINIMO;
    }
    public static boolean cumpleMonto(Reserva reserva){
        return reserva!=null&&cumpleMonto(reserva.precioTotal);
    }
    public static boolean puedeSolicitar(Reserva reserva){
        return reserva!=null&&puedeSolicitar(reserva.estado.name(),reserva.precioTotal);
    }
    public static boolean puedeSolicitar(String estado,double importe){
        return cumpleMonto(importe)&&("EN_CHECKOUT".equals(estado)||CheckoutPolicy.finalizada(estado));
    }
}
