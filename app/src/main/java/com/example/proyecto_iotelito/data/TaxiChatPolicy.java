package com.example.proyecto_iotelito.data;

public final class TaxiChatPolicy {
    private TaxiChatPolicy() { }
    public static boolean asignado(String uid,String estado){return uid!=null&&!uid.isEmpty()&&
        java.util.Arrays.asList("ASIGNADO","EN_CAMINO","EN_TRASLADO","FINALIZADO").contains(estado);}
    public static boolean permiteEnviar(String uid,String estado){return asignado(uid,estado)&&!"FINALIZADO".equals(estado);}
    public static String validar(String text){
        String value=text==null?"":text.trim();
        if(value.isEmpty()||value.length()>4000)throw new IllegalArgumentException("Escribe entre 1 y 4000 caracteres.");
        return value;
    }
}
