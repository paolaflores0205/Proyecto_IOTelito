package com.example.proyecto_iotelito.data.superadmin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Validaciones de escritura; no rechaza la lectura de hoteles históricos incompletos. */
public final class SuperadminFormValidation {
    private SuperadminFormValidation() { }
    public static Map<String, String> hotel(String name, String address, String district,
            String city, String phone, String email, String description) {
        Map<String, String> errors = new LinkedHashMap<>();
        phone = phone == null ? "" : phone;
        email = email == null ? "" : email;
        text(errors, "name", name, 3, 160, false);
        text(errors, "address", address, 5, 500, false);
        text(errors, "district", district, 2, 120, false);
        text(errors, "city", city, 2, 120, false);
        text(errors, "phone", phone, 7, 25, false);
        String digits = phone.trim().replaceAll("\\D", "");
        if (!errors.containsKey("phone") && (!phone.trim().matches("\\+?[0-9() .-]+")
                || digits.length() < 7 || digits.length() > 15)) errors.put("phone", "Ingresa un teléfono de 7 a 15 dígitos.");
        text(errors, "email", email, 3, 254, false);
        if (!errors.containsKey("email") && !email.trim().matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+"))
            errors.put("email", "Ingresa un correo válido, por ejemplo contacto@hotel.pe.");
        text(errors, "description", description, 10, 5000, true);
        return errors;
    }
    private static void text(Map<String, String> errors, String key, String value, int min, int max, boolean multiline) {
        String s = value == null ? "" : value.trim();
        if (s.isEmpty()) errors.put(key, "Completa este campo.");
        else if (s.length() < min || s.length() > max) errors.put(key, "Usa entre " + min + " y " + max + " caracteres.");
        else if (s.codePoints().anyMatch(c -> Character.isISOControl(c) && !(multiline && (c == '\n' || c == '\r' || c == '\t'))))
            errors.put(key, "Elimina los caracteres no válidos.");
        else if (!key.equals("phone") && !key.equals("email") && s.codePoints().noneMatch(Character::isLetter))
            errors.put(key, "Incluye un texto descriptivo, no solo números o símbolos.");
    }
    public static String reason(String value) {
        Map<String, String> errors = new LinkedHashMap<>();
        text(errors, "reason", value, 5, 500, true);
        return errors.get("reason");
    }
}
