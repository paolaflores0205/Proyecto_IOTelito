package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.UserProfile;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Conversión y validación del perfil. No infiere documentos, cumpleaños ni apellidos antiguos. */
public final class ClientProfileCodec {
    public static final String[] DOCUMENTS = {"DNI", "Carnet de Extranjería", "Pasaporte"};
    private static final DateTimeFormatter BIRTH = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private ClientProfileCodec() { }
    private static String text(Map<String,Object> data, String key) {
        Object value = data.get(key); return value instanceof String ? ((String)value).trim() : "";
    }
    public static UserProfile decode(Map<String,Object> data) {
        String first = text(data,"firstNames"), last = text(data,"lastNames"), name = text(data,"name");
        String birthday = text(data,"birthDate");
        try { birthday = LocalDate.parse(birthday).format(BIRTH); } catch (DateTimeParseException ignored) { }
        UserProfile profile = new UserProfile(name, text(data,"email"), text(data,"phone"), text(data,"documentType"),
                text(data,"documentNumber"), birthday, text(data,"address"));
        profile.firstNames = first.isEmpty() ? name : first;
        profile.lastNames = last;
        profile.photoUrl = text(data,"photoUrl");
        return profile;
    }
    public static void validarDocumento(String tipo, String numero) {
        String pattern = "DNI".equals(tipo) ? "[0-9]{8}" : "Carnet de Extranjería".equals(tipo) ? "[A-Za-z0-9]{9,12}"
                : "Pasaporte".equals(tipo) ? "[A-Za-z0-9]{6,12}" : null;
        if (pattern == null || !numero.matches(pattern)) throw new IllegalArgumentException(
                "DNI".equals(tipo) ? "El DNI debe tener 8 dígitos." : "Revisa el número del documento seleccionado.");
    }
    /** Campos de registro validados antes de crear Auth. Editar perfil nunca usa este método. */
    public static Map<String,Object> registro(String first, String last, String tipo, String numero,
            String nacimiento, String phone, String address, LocalDate hoy) {
        Map<String,Object> fields = new HashMap<>(editables(first,last,phone,address));
        String documento = numero.trim().toUpperCase(Locale.ROOT);
        validarDocumento(tipo,documento);
        fields.put("documentType",tipo);
        fields.put("documentNumber",documento);
        fields.put("birthDate",nacimientoIso(nacimiento,hoy));
        fields.put("role","CLIENT"); fields.put("active",true); fields.put("hotelId",null);
        return fields;
    }
    public static String nacimientoIso(String value, LocalDate hoy) {
        try {
            LocalDate date = LocalDate.parse(value, BIRTH);
            if (date.isAfter(hoy) || Period.between(date,hoy).getYears() < 18 || date.isBefore(hoy.minusYears(120)))
                throw new IllegalArgumentException();
            return date.toString();
        } catch (RuntimeException error) { throw new IllegalArgumentException("Ingresa una fecha válida de una persona mayor de edad (DD/MM/AAAA)."); }
    }
    public static Map<String,Object> editables(String first, String last, String phone, String address) {
        first = first.trim(); last = last.trim(); phone = phone.replace(" ", "").trim(); address = address.trim();
        if (first.isEmpty() || last.isEmpty() || first.length() > 80 || last.length() > 79)
            throw new IllegalArgumentException("Completa nombres y apellidos (máximo 80 y 79 caracteres).");
        if (!phone.matches("[0-9]{9}")) throw new IllegalArgumentException("El teléfono debe tener 9 dígitos.");
        if (address.length() < 8 || address.length() > 500) throw new IllegalArgumentException("El domicilio debe tener entre 8 y 500 caracteres.");
        Map<String,Object> data = new HashMap<>(); data.put("name",first + " " + last);
        data.put("firstNames",first);data.put("lastNames",last);data.put("phone",phone);data.put("address",address);
        return data;
    }
}
