package com.example.proyecto_iotelito.data;

import java.util.*;

/** Catálogo de destinos por ciudad del hotel; no representa rutas ni disponibilidad de vuelos. */
public final class AeropuertosCliente {
    public static final class Aeropuerto {
        public final String codigo, nombre, ciudad;
        private Aeropuerto(String codigo,String nombre,String ciudad){this.codigo=codigo;this.nombre=nombre;this.ciudad=ciudad;}
        public String etiqueta(){return nombre+" ("+codigo+")";}
    }
    private static final Aeropuerto LIM=new Aeropuerto("LIM","Aeropuerto Internacional Jorge Chávez","Callao");
    private static final Aeropuerto CUZ=new Aeropuerto("CUZ","Aeropuerto Internacional Alejandro Velasco Astete","Cusco");
    private static final Aeropuerto AQP=new Aeropuerto("AQP","Aeropuerto Internacional Alfredo Rodríguez Ballón","Arequipa");
    private static final Aeropuerto TRU=new Aeropuerto("TRU","Aeropuerto Internacional Capitán FAP Carlos Martínez de Pinillos","Trujillo");
    private static final Aeropuerto PIO=new Aeropuerto("PIO","Aeropuerto Capitán FAP Renán Elías Olivera","Pisco");
    private AeropuertosCliente() { }
    public static List<Aeropuerto> paraCiudad(String ciudad){
        String value=ciudad==null?"":ciudad.trim().toLowerCase(Locale.ROOT);
        Aeropuerto airport;
        switch(value){
            case "lima":case "lima metropolitana":case "callao":airport=LIM;break;
            case "cusco":case "cuzco":airport=CUZ;break;
            case "arequipa":airport=AQP;break;
            case "trujillo":airport=TRU;break;
            case "paracas":case "pisco":airport=PIO;break;
            default:return Collections.emptyList();
        }
        return Collections.singletonList(airport);
    }
    public static Aeropuerto elegir(String ciudad,String codigo){
        return paraCiudad(ciudad).stream().filter(a->a.codigo.equals(codigo)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Selecciona un aeropuerto disponible para la ciudad del hotel."));
    }
}
