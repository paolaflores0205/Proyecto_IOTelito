package com.example.proyecto_iotelito.data;

import java.time.YearMonth;
import java.util.*;

/** Solo conserva datos enmascarados y un token ficticio. Nunca contiene PAN ni CVV. */
public final class TarjetaSimulada {
    public final String tipo, ultimos4, token;
    private TarjetaSimulada(String tipo, String ultimos4, String token) {
        this.tipo = tipo; this.ultimos4 = ultimos4; this.token = token;
    }
    public static TarjetaSimulada validar(String numero, String vencimiento, String cvv, YearMonth hoy) {
        String digits = numero.replace(" ", "");
        if (!digits.matches("[0-9]{13,19}")) throw new IllegalArgumentException("Revisa el número de la tarjeta de prueba.");
        int sum = 0; boolean twice = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (twice) { n *= 2; if (n > 9) n -= 9; }
            sum += n; twice = !twice;
        }
        if (sum % 10 != 0 || digits.chars().allMatch(c -> c == '0'))
            throw new IllegalArgumentException("Número inválido. Puedes usar la tarjeta de prueba 4242 4242 4242 4242.");
        try {
            if (!vencimiento.matches("(0[1-9]|1[0-2])/[0-9]{2}")) throw new IllegalArgumentException();
            YearMonth expiry = YearMonth.of(2000 + Integer.parseInt(vencimiento.substring(3)), Integer.parseInt(vencimiento.substring(0, 2)));
            if (expiry.isBefore(hoy)) throw new IllegalArgumentException();
        } catch (RuntimeException error) { throw new IllegalArgumentException("Ingresa un vencimiento vigente en formato MM/AA."); }
        boolean amex = digits.startsWith("34") || digits.startsWith("37");
        if (!cvv.matches(amex ? "[0-9]{4}" : "[0-9]{3}"))
            throw new IllegalArgumentException(amex ? "El código de prueba debe tener 4 dígitos." : "El código de prueba debe tener 3 dígitos.");
        String type = digits.startsWith("4") ? "Visa" : amex ? "Amex" : digits.startsWith("5") || digits.startsWith("2") ? "Mastercard" : "Tarjeta";
        return new TarjetaSimulada(type, digits.substring(digits.length() - 4), "DEMO_" + UUID.randomUUID());
    }
    public static TarjetaSimulada leer(Map<String,Object> data) {
        Object type = data.get("cardType"), last = data.get("cardLast4"), token = data.get("cardToken");
        if (!(type instanceof String) || !(last instanceof String) || !(token instanceof String)
                || !Arrays.asList("Visa", "Mastercard", "Amex", "Tarjeta").contains(type)
                || !((String)last).matches("[0-9]{4}") || !((String)token).matches("DEMO_[a-zA-Z0-9-]{1,64}")
                || !"SIMULATED".equals(data.get("paymentMode"))) return null;
        return new TarjetaSimulada((String)type, (String)last, (String)token);
    }
    public Map<String,Object> campos() {
        Map<String,Object> result = new HashMap<>();
        result.put("cardType", tipo); result.put("cardLast4", ultimos4);
        result.put("cardToken", token); result.put("paymentMode", "SIMULATED");
        return result;
    }
    public String etiqueta() { return tipo + " terminada en " + ultimos4 + " · Prueba"; }
}
