package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.TipoDocumento;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas del registro de taxistas, campo por campo. Devuelve un mensaje por cada campo inválido
 * (la clave es el nombre del campo en el formulario) para mostrarlo en rojo debajo de él.
 * La pantalla repite estas reglas en JavaScript, pero el servidor es la fuente de verdad.
 */
public final class RegistroTaxistaValidador {

    /** Categorías de licencia del MTC habilitadas para transporte de pasajeros. */
    public static final List<String> CATEGORIAS_LICENCIA =
            List.of("A-I", "A-IIa", "A-IIb", "A-IIIa", "A-IIIb", "A-IIIc");
    public static final int ANIO_VEHICULO_MINIMO = 1950;

    private static final Pattern NOMBRE = Pattern.compile("[\\p{L}][\\p{L} '.-]{1,99}");
    private static final Pattern TELEFONO = Pattern.compile("[0-9]{9}");
    private static final Pattern LICENCIA = Pattern.compile("[A-Z][0-9]{8}");
    private static final Pattern PLACA = Pattern.compile("[A-Z0-9]{3}-[0-9]{3}");
    private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}");
    private static final Pattern TEXTO_VEHICULO = Pattern.compile("[\\p{L}0-9][\\p{L}0-9 .-]{1,59}");

    private RegistroTaxistaValidador() { }

    public static Map<String, String> validar(TaxistaRegistroForm form, LocalDate hoy) {
        Map<String, String> errores = new LinkedHashMap<>();

        nombre(errores, "nombres", form.getNombres(), "Ingresa tus nombres (solo letras, de 2 a 100).");
        nombre(errores, "apellidos", form.getApellidos(), "Ingresa tus apellidos (solo letras, de 2 a 100).");

        TipoDocumento tipo = form.getTipoDocumento();
        String documento = texto(form.getNumeroDocumento()).toUpperCase(Locale.ROOT);
        if (tipo == null) {
            errores.put("tipoDocumento", "Elige el tipo de documento.");
        } else if (!tipo.valida(documento)) {
            errores.put("numeroDocumento", switch (tipo) {
                case DNI -> "El DNI debe tener exactamente 8 dígitos.";
                case PASAPORTE -> "El pasaporte debe tener de 6 a 12 letras o números.";
                case CARNET_EXTRANJERIA -> "El carné de extranjería debe tener de 9 a 12 letras o números.";
            });
        }

        LocalDate nacimiento = form.getFechaNacimiento();
        if (nacimiento == null) {
            errores.put("fechaNacimiento", "Ingresa tu fecha de nacimiento.");
        } else if (nacimiento.isAfter(hoy)) {
            errores.put("fechaNacimiento", "La fecha de nacimiento no puede ser futura.");
        } else if (Period.between(nacimiento, hoy).getYears() < 18) {
            errores.put("fechaNacimiento", "Debes ser mayor de 18 años.");
        } else if (nacimiento.isBefore(hoy.minusYears(100))) {
            errores.put("fechaNacimiento", "Revisa el año de nacimiento.");
        }

        if (!TELEFONO.matcher(normalizarTelefono(form.getTelefono())).matches()) {
            errores.put("telefono", "El teléfono debe tener 9 dígitos.");
        }
        String correo = texto(form.getCorreo());
        if (correo.length() > 150 || !CORREO.matcher(correo).matches()) {
            errores.put("correo", "Ingresa un correo válido, por ejemplo nombre@gmail.com.");
        }
        String domicilio = texto(form.getDomicilio());
        if (domicilio.length() < 8 || domicilio.length() > 255) {
            errores.put("domicilio", "El domicilio debe tener entre 8 y 255 caracteres.");
        }

        if (!LICENCIA.matcher(texto(form.getNumeroLicencia()).toUpperCase(Locale.ROOT)).matches()) {
            errores.put("numeroLicencia", "La licencia debe tener una letra y 8 dígitos, por ejemplo Q12345678.");
        }
        if (!CATEGORIAS_LICENCIA.contains(texto(form.getCategoriaLicencia()))) {
            errores.put("categoriaLicencia", "Elige la categoría de tu licencia.");
        }
        LocalDate vencimiento = form.getFechaVencimientoLicencia();
        if (vencimiento == null) {
            errores.put("fechaVencimientoLicencia", "Ingresa la fecha de vencimiento de la licencia.");
        } else if (vencimiento.isBefore(hoy)) {
            errores.put("fechaVencimientoLicencia", "La licencia está vencida: la fecha debe ser hoy o posterior.");
        } else if (vencimiento.isAfter(hoy.plusYears(15))) {
            errores.put("fechaVencimientoLicencia", "Revisa la fecha de vencimiento.");
        }

        vehiculo(errores, "marcaVehiculo", form.getMarcaVehiculo(), "Ingresa la marca (de 2 a 60 caracteres).");
        vehiculo(errores, "modeloVehiculo", form.getModeloVehiculo(), "Ingresa el modelo (de 2 a 60 caracteres).");
        vehiculo(errores, "colorVehiculo", form.getColorVehiculo(), "Ingresa el color (de 2 a 60 caracteres).");
        Integer anio = form.getAnioVehiculo();
        int maximo = hoy.getYear() + 1;
        if (anio == null || anio < ANIO_VEHICULO_MINIMO || anio > maximo) {
            errores.put("anioVehiculo", "Ingresa un año entre " + ANIO_VEHICULO_MINIMO + " y " + maximo + ".");
        }
        if (!PLACA.matcher(texto(form.getPlacaVehiculo()).toUpperCase(Locale.ROOT)).matches()) {
            errores.put("placaVehiculo", "La placa debe tener el formato ABC-123.");
        }

        if (!Boolean.TRUE.equals(form.getAceptaTerminos())) {
            errores.put("aceptaTerminos", "Debes aceptar los términos para registrarte.");
        }
        return errores;
    }

    /** Deja solo los 9 dígitos del celular: quita espacios, guiones y el código de país +51. */
    public static String normalizarTelefono(String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
        return digitos.length() == 11 && digitos.startsWith("51") ? digitos.substring(2) : digitos;
    }

    private static void nombre(Map<String, String> errores, String campo, String valor, String mensaje) {
        if (!NOMBRE.matcher(texto(valor)).matches()) errores.put(campo, mensaje);
    }

    private static void vehiculo(Map<String, String> errores, String campo, String valor, String mensaje) {
        if (!TEXTO_VEHICULO.matcher(texto(valor)).matches()) errores.put(campo, mensaje);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
