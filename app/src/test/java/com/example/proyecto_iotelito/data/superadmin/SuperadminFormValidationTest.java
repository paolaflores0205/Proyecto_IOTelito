package com.example.proyecto_iotelito.data.superadmin;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuperadminFormValidationTest {
    private java.util.Map<String,String> check(String phone, String email, String description) {
        return SuperadminFormValidation.hotel("Hotel Perú", "Av. José Pardo 120", "Miraflores", "Lima", phone, email, description);
    }
    @Test public void admiteAcentosTelefonoInternacionalYDescripcionMultilinea() {
        assertTrue(check("+51 999 123 456", "reservas@hotel.pe", "Hotel céntrico.\nHabitaciones familiares.").isEmpty());
    }
    @Test public void identificaTodosLosCamposVacios() {
        assertEquals(7, SuperadminFormValidation.hotel(" ", "", "", "", "", "", "").size());
    }
    @Test public void rechazaContactosInvalidosSinBloquearTextoValido() {
        assertTrue(check("123", "hotel@", "Descripción del hotel.").containsKey("phone"));
        assertTrue(check("999123456", "hotel@", "Descripción del hotel.").containsKey("email"));
        assertTrue(check("999123456", "a@hotel.pe", " ").containsKey("description"));
        assertTrue(check("999123456", "a@hotel.pe", "x".repeat(5001)).containsKey("description"));
    }
    @Test public void controlaLimitesYNoAceptaSoloSimbolos() {
        assertTrue(SuperadminFormValidation.hotel("123", "#####", "--", "22", "999123456", "a@hotel.pe", "Descripción válida.").containsKey("name"));
        assertNotNull(SuperadminFormValidation.reason("   "));
        assertNotNull(SuperadminFormValidation.reason("x".repeat(501)));
        assertNull(SuperadminFormValidation.reason("Solicitud del usuario."));
    }
}
