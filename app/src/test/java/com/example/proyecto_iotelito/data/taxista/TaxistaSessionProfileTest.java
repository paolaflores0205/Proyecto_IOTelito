package com.example.proyecto_iotelito.data.taxista;

import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaxistaSessionProfileTest {
    @Test public void clienteSinDocumentoConservaSuRol() {
        assertEquals(UserRole.CLIENT,
                TaxistaSessionProfile.resolver("cliente@example.test", "Cliente", null).getRole());
    }

    @Test public void taxistaActivoUsaElNombreDeFirestore() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("activo", true);
        profile.put("nombre", " Adrian ");
        AuthenticatedUser user = TaxistaSessionProfile.resolver("taxista@example.test", null, profile);
        assertEquals(UserRole.TAXISTA, user.getRole());
        assertEquals("Adrian", user.getDisplayName());
    }

    @Test public void perfilInactivoOIncompletoNoEntraComoCliente() {
        for (Object activo : new Object[]{false, null, "true"}) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("activo", activo);
            assertThrows(TaxistaSessionProfile.TaxistaInactivoException.class,
                    () -> TaxistaSessionProfile.resolver("taxista@example.test", null, profile));
        }
    }

    @Test public void nombreVacioNoImpideIdentificarAlTaxista() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("activo", true);
        profile.put("nombre", " ");
        assertEquals("taxista@example.test",
                TaxistaSessionProfile.resolver("taxista@example.test", null, profile).getDisplayName());
    }
}
