package com.example.proyecto_iotelito.data.taxista;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.proyecto_iotelito.data.taxista.DisponibilidadEstado.Problema.*;

public class DisponibilidadEstadoTest {
    @Test public void noAceptaSolicitudesAntesDeConsultarServidor() {
        DisponibilidadEstado estado=DisponibilidadEstado.inicial();
        assertFalse(estado.editable());
        assertFalse(estado.permiteAceptar());
        assertThrows(IllegalStateException.class,estado::iniciarCambio);
    }

    @Test public void disponibilidadRemotaControlaLaAceptacion() {
        assertTrue(DisponibilidadEstado.inicial().servidor(true,true).permiteAceptar());
        assertFalse(DisponibilidadEstado.inicial().servidor(true,false).permiteAceptar());
    }

    @Test public void bloqueaPulsacionesYServiciosDuranteUnaEscritura() {
        DisponibilidadEstado estado=DisponibilidadEstado.inicial().servidor(true,true).iniciarCambio();
        assertTrue(estado.disponible);
        assertFalse(estado.editable());
        assertFalse(estado.permiteAceptar());
        assertThrows(IllegalStateException.class,estado::iniciarCambio);
        // Un evento del listener no debe liberar el botón antes de la confirmación de escritura.
        assertFalse(estado.servidor(true,false).editable());
        assertTrue(estado.confirmar(false).editable());
    }

    @Test public void escrituraFallidaConservaElValorConfirmado() {
        DisponibilidadEstado estado=DisponibilidadEstado.inicial().servidor(true,false)
                .iniciarCambio().fallar(GUARDADO);
        assertFalse(estado.disponible);
        assertTrue(estado.editable());
        assertEquals(GUARDADO,estado.problema);
    }

    @Test public void cacheSinConexionNoAutorizaAceptarYSeRecupera() {
        DisponibilidadEstado estado=DisponibilidadEstado.inicial().servidor(true,true).desconectar(CONEXION);
        assertTrue(estado.disponible);
        assertFalse(estado.permiteAceptar());
        assertTrue(estado.servidor(true,true).permiteAceptar());
    }

    @Test public void perfilInactivoOValoresInvalidosNoHabilitanElInterruptor() {
        for(Object activo:new Object[]{false,null,"true"}) {
            assertFalse(DisponibilidadEstado.inicial().servidor(activo,true).editable());
        }
        for(Object disponible:new Object[]{null,"true",1}) {
            assertFalse(DisponibilidadEstado.inicial().servidor(true,disponible).editable());
        }
    }

    @Test public void perdidaDePermisosOSesionNoHabilitaUnaDisponibilidadVieja() {
        for(DisponibilidadEstado.Problema problema:new DisponibilidadEstado.Problema[]{PERMISOS,SESION,PERFIL}) {
            DisponibilidadEstado estado=DisponibilidadEstado.inicial().servidor(true,true)
                    .iniciarCambio().fallar(problema);
            assertFalse(estado.editable());
            assertFalse(estado.permiteAceptar());
        }
    }

    @Test public void cuentaDemoConservaSuDisponibilidadLocal() {
        assertTrue(DisponibilidadEstado.local(true).permiteAceptar());
        assertTrue(DisponibilidadEstado.local(false).editable());
        assertFalse(DisponibilidadEstado.local(false).permiteAceptar());
    }
}
