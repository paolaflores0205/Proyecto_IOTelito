package com.example.proyecto_iotelito.data.superadmin;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.proyecto_iotelito.data.superadmin.FirebaseHotelsRepository.SnapshotStatus.*;

public class FirebaseHotelsSnapshotTest {
    @Test public void respuestaInicialDeCacheEsEsperaNoError() {
        assertEquals(WAITING_FOR_SERVER, FirebaseHotelsRepository.snapshotStatus(false, true, true));
    }
    @Test public void respuestaDelServidorPermiteMostrarHoteles() {
        assertEquals(READY, FirebaseHotelsRepository.snapshotStatus(false, true, false));
    }
    @Test public void errorRealNoSeOcultaAunqueHayaCache() {
        assertEquals(ERROR, FirebaseHotelsRepository.snapshotStatus(true, true, true));
        assertEquals(ERROR, FirebaseHotelsRepository.snapshotStatus(true, true, false));
    }
    @Test public void respuestaAusenteSigueSiendoError() {
        assertEquals(ERROR, FirebaseHotelsRepository.snapshotStatus(false, false, false));
    }
}
