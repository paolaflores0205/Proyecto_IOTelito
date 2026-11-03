package com.example.proyecto_iotelito.data.taxista;

import com.example.proyecto_iotelito.model.auth.UserRole;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaxiStorageRouteTest {
    @Test public void conservaElPedidoLocalDeUnClienteQueAhoraEntraPorFirebase() {
        assertTrue(TaxiStorageRoute.usarLocal(true,UserRole.CLIENT,"cliente@example.test","cliente@example.test"));
    }
    @Test public void unPedidoLocalDeOtraCuentaNoOcultaElPedidoRemotoConEseId() {
        assertFalse(TaxiStorageRoute.usarLocal(true,UserRole.CLIENT,"nuevo@example.test","otro@example.test"));
    }
    @Test public void unClienteSinPedidoPrevioConsultaFirebase() {
        assertFalse(TaxiStorageRoute.usarLocal(true,UserRole.CLIENT,"cliente@example.test",null));
    }
    @Test public void taxistaRemotoNoUsaUnPedidoLocalPorCoincidenciaDeCorreo() {
        assertFalse(TaxiStorageRoute.usarLocal(true,UserRole.TAXISTA,"taxista@example.test","taxista@example.test"));
    }
    @Test public void mantieneElRecorridoLocalDeDemostracion() {
        assertTrue(TaxiStorageRoute.usarLocal(false,UserRole.TAXISTA,"taxista@example.test","cliente@example.test"));
    }
}
