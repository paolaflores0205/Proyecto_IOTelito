package com.example.proyecto_iotelito.notification;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClientPushPolicyTest {
    @Test public void descartaAvisosRetrasadosTrasCambiarDeCuentaOCerrarSesion() {
        assertTrue(ClientPushPolicy.accepts("alice","session-a","alice","alice","session-a"));
        assertFalse(ClientPushPolicy.accepts("alice","session-a","bob","bob","session-b"));
        assertFalse(ClientPushPolicy.accepts("alice","session-a",null,null,null));
        assertFalse(ClientPushPolicy.accepts("alice","session-a","alice",null,null));
    }
    @Test public void renovarSesionInvalidaLosMensajesAnterioresAunqueElUidCoincida() {
        assertFalse(ClientPushPolicy.accepts("alice","old-session","alice","alice","new-session"));
        assertFalse(ClientPushPolicy.accepts("alice","","alice","alice",""));
        assertFalse(ClientPushPolicy.accepts(null,null,null,null,null));
    }
}
