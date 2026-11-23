package com.example.proyecto_iotelito.data.hoteladmin;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reglas de los formularios del administrador de hotel. Cada método devuelve un mensaje por campo
 * inválido (vacío si todo está bien) para mostrarlo en rojo debajo del campo. Las reglas de
 * Firestore (validRoom, validService) exigen los mismos límites en el servidor.
 */
public final class InventarioValidacion {
    public static final int AREA_MIN = 5, AREA_MAX = 1000;
    public static final int ADULTOS_MIN = 1, ADULTOS_MAX = 10, NINOS_MAX = 10;
    public static final double PRECIO_HABITACION_MAX = 100000, PRECIO_SERVICIO_MAX = 10000;

    private InventarioValidacion() { }

    public static Map<String, String> habitacion(String codigo, String area, String adultos, String ninos,
                                                 String precio, String descripcion) {
        Map<String, String> errores = new LinkedHashMap<>();
        String cod = texto(codigo);
        if (cod.isEmpty() || cod.length() > 80) errores.put("codigo", "Ingresa el código o nombre de la habitación (máximo 80).");
        Integer m2 = entero(area);
        if (m2 == null || m2 < AREA_MIN || m2 > AREA_MAX)
            errores.put("area", "El área debe estar entre " + AREA_MIN + " y " + AREA_MAX + " m².");
        Integer ad = entero(adultos);
        if (ad == null || ad < ADULTOS_MIN || ad > ADULTOS_MAX)
            errores.put("adultos", "Indica de " + ADULTOS_MIN + " a " + ADULTOS_MAX + " adultos.");
        Integer ni = texto(ninos).isEmpty() ? Integer.valueOf(0) : entero(ninos);
        if (ni == null || ni < 0 || ni > NINOS_MAX) errores.put("ninos", "Indica de 0 a " + NINOS_MAX + " niños.");
        String errorPrecio = precio(precio, false, PRECIO_HABITACION_MAX);
        if (errorPrecio != null) errores.put("precio", errorPrecio);
        if (texto(descripcion).length() > 5000) errores.put("descripcion", "La descripción admite hasta 5000 caracteres.");
        return errores;
    }

    public static Map<String, String> servicio(String nombre, String precio, String descripcion) {
        Map<String, String> errores = new LinkedHashMap<>();
        String n = texto(nombre);
        if (n.length() < 2 || n.length() > 160) errores.put("nombre", "Ingresa el nombre del servicio (de 2 a 160 caracteres).");
        String errorPrecio = precio(precio, true, PRECIO_SERVICIO_MAX);
        if (errorPrecio != null) errores.put("precio", errorPrecio);
        if (texto(descripcion).length() > 5000) errores.put("descripcion", "La descripción admite hasta 5000 caracteres.");
        return errores;
    }

    /** Teléfono del hotel: de 7 a 15 dígitos; admite espacios, +, guiones y paréntesis. Null si es válido. */
    public static String telefono(String telefono) {
        String t = texto(telefono);
        String digitos = t.replaceAll("[^0-9]", "");
        if (!t.matches("[0-9 +()-]*") || digitos.length() < 7 || digitos.length() > 15)
            return "Ingresa un teléfono de 7 a 15 dígitos.";
        return null;
    }

    /** Precio con hasta 2 decimales. Las habitaciones no pueden ser gratis; los servicios sí. */
    static String precio(String valor, boolean admiteCero, double maximo) {
        try {
            BigDecimal precio = new BigDecimal(texto(valor).replace(',', '.'));
            boolean minimo = admiteCero ? precio.signum() >= 0 : precio.signum() > 0;
            if (!minimo || precio.doubleValue() > maximo || precio.stripTrailingZeros().scale() > 2)
                throw new NumberFormatException();
            return null;
        } catch (NumberFormatException invalido) {
            return (admiteCero ? "El precio debe estar entre S/ 0 y S/ " : "El precio debe ser mayor que S/ 0 y hasta S/ ")
                    + String.format(java.util.Locale.US, "%,.0f", maximo) + ", con máximo 2 decimales.";
        }
    }

    private static Integer entero(String valor) {
        try { return Integer.valueOf(texto(valor)); } catch (NumberFormatException e) { return null; }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
