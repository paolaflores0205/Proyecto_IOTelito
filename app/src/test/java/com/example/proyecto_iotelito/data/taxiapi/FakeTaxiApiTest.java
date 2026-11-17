package com.example.proyecto_iotelito.data.taxiapi;

import static com.example.proyecto_iotelito.data.taxiapi.TaxiApiException.Codigo.*;

import static org.junit.Assert.*;

import org.json.*;
import org.junit.*;

import java.util.*;
import java.util.concurrent.*;

public class FakeTaxiApiTest {
    private FakeTaxiApi.Store store;
    private TaxiApi client, driver, other;

    private FakeTaxiApi api(String uid, String role) {
        return new FakeTaxiApi(
                store,
                () ->
                        new FakeTaxiApi.Caller(
                                uid,
                                role,
                                uid,
                                uid + "@example.test",
                                role.equals("HOTEL_ADMIN") ? 7 : null),
                (id, c) ->
                        new FakeTaxiApi.Booking(
                                "client",
                                "Pasajero",
                                JsonTaxiApi.json(
                                        "id",
                                        7,
                                        "nombre",
                                        "Hotel de prueba",
                                        "direccion",
                                        "Dirección de prueba",
                                        "ciudad",
                                        "  Lima  ")));
    }

    @Before
    public void setup() {
        store = new FakeTaxiApi.Store();
        client = api("client", "CLIENT");
        driver = api("driver", "TAXISTA");
        other = api("other", "TAXISTA");
        driver.cambiarDisponibilidad(true);
        other.cambiarDisponibilidad(true);
        client.crearServicio(123, "LIM", "15:30", 2, 1);
    }

    private void stateToFinal() {
        driver.aceptarServicio(123);
        driver.avanzarServicio(123, "EN_CAMINO");
        driver.avanzarServicio(123, "EN_TRASLADO");
    }

    @Test public void chatSoloEntreParticipantesYReintentosSinDuplicados() {
        assertEquals(409, assertThrows(TaxiApiException.class, () -> client.mensajes(123)).status);
        driver.aceptarServicio(123);
        String uuid=UUID.randomUUID().toString();
        client.enviarMensaje(123,uuid,"Nos vemos en recepción.");
        client.enviarMensaje(123,uuid,"Nos vemos en recepción.");
        assertEquals(1,driver.mensajes(123).optJSONArray("mensajes").length());
        assertFalse(driver.mensajes(123).optJSONArray("mensajes").optJSONObject(0).optBoolean("mio"));
        driver.enviarMensaje(123,UUID.randomUUID().toString(),"Estoy en camino.");
        assertEquals(2,client.mensajes(123).optJSONArray("mensajes").length());
        assertEquals(403,assertThrows(TaxiApiException.class,()->other.mensajes(123)).status);
        assertEquals(403,assertThrows(TaxiApiException.class,()->api("another","CLIENT").mensajes(123)).status);
        assertEquals(400,assertThrows(TaxiApiException.class,()->client.enviarMensaje(123,UUID.randomUUID().toString()," ")).status);
    }

    @Test public void chatFinalizadoConservaHistorialPeroNoAceptaNuevosMensajes() {
        stateToFinal();
        client.enviarMensaje(123,UUID.randomUUID().toString(),"Gracias por el viaje.");
        String qr=client.verQr(123).optString("codigo");
        driver.finalizarServicio(123,qr);
        assertFalse(client.mensajes(123).optBoolean("permiteEnviar"));
        assertEquals(1,client.mensajes(123).optJSONArray("mensajes").length());
        assertEquals(409,assertThrows(TaxiApiException.class,()->driver.enviarMensaje(123,UUID.randomUUID().toString(),"Otro mensaje")).status);
    }

    @Test
    public void unSoloGanadorConcurrente() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (TaxiApi a : new TaxiApi[] {driver, other})
                futures.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        a.aceptarServicio(123);
                                        return true;
                                    } catch (TaxiApiException e) {
                                        assertEquals(SERVICIO_NO_DISPONIBLE, e.codigo);
                                        assertEquals(409, e.status);
                                        return false;
                                    }
                                }));
            start.countDown();
            int wins = 0;
            for (Future<Boolean> f : futures) if (f.get(2, TimeUnit.SECONDS)) wins++;
            assertEquals(1, wins);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void unConductorNoAceptaDosPedidosConcurrentes() throws Exception {
        client.crearServicio(124, "LIM", "15:30", 1, 0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> fs = new ArrayList<>();
            for (long id : new long[] {123, 124})
                fs.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        driver.aceptarServicio(id);
                                        return true;
                                    } catch (TaxiApiException e) {
                                        assertEquals(SERVICIO_ACTIVO, e.codigo);
                                        return false;
                                    }
                                }));
            start.countDown();
            int wins = 0;
            for (Future<Boolean> f : fs) if (f.get(2, TimeUnit.SECONDS)) wins++;
            assertEquals(1, wins);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void saltosInvalidosDan409() {
        driver.aceptarServicio(123);
        TaxiApiException e =
                assertThrows(
                        TaxiApiException.class, () -> driver.avanzarServicio(123, "EN_TRASLADO"));
        assertEquals(409, e.status);
        assertEquals(ESTADO_INVALIDO, e.codigo);
    }

    @Test
    public void qrIncorrectoDa400() {
        stateToFinal();
        TaxiApiException e =
                assertThrows(
                        TaxiApiException.class, () -> driver.finalizarServicio(123, "TAXI-123"));
        assertEquals(400, e.status);
        assertEquals(QR_INVALIDO, e.codigo);
        assertNotNull(driver.miServicioActivo());
    }

    @Test
    public void soloUnaCalificacionYActualizaPromedio() {
        stateToFinal();
        String qr = client.verQr(123).optString("codigo");
        driver.finalizarServicio(123, qr);
        client.calificarServicio(123, 5, "Bien");
        assertEquals(5, driver.miPerfilTaxista().optDouble("calificacion"), 0.001);
        assertEquals(
                YA_CALIFICADO,
                assertThrows(TaxiApiException.class, () -> client.calificarServicio(123, 4, null))
                        .codigo);
    }

    @Test
    public void crearAceptarAvanzarFinalizarSonIdempotentes() {
        assertEquals(123, client.crearServicio(123, "CUZ", "15:30", 1, 0).optLong("id"));
        driver.aceptarServicio(123);
        String qr = client.verQr(123).optString("codigo");
        driver.aceptarServicio(123);
        assertEquals(qr, client.verQr(123).optString("codigo"));
        driver.avanzarServicio(123, "EN_CAMINO");
        driver.avanzarServicio(123, "EN_CAMINO");
        driver.avanzarServicio(123, "EN_TRASLADO");
        driver.finalizarServicio(123, qr);
        driver.finalizarServicio(123, qr);
        assertNull(driver.miServicioActivo());
        assertEquals(1, driver.miHistorial(50).optJSONArray("servicios").length());
        assertEquals(1, driver.miPerfilTaxista().optInt("cantidadServicios"));
    }

    @Test
    public void otraCuentaNoConsultaNiCalifica() {
        TaxiApi intruder = api("intruder", "CLIENT");
        assertEquals(
                PROHIBIDO,
                assertThrows(TaxiApiException.class, () -> intruder.verServicio(123)).codigo);
        assertEquals(
                PROHIBIDO,
                assertThrows(
                                TaxiApiException.class,
                                () -> intruder.crearServicio(124, "LIM", "15:30", 1, 0))
                        .codigo);
    }

    @Test
    public void soloAsignadoAvanzaYTaxistaNoLeeQr() {
        driver.aceptarServicio(123);
        assertEquals(
                PROHIBIDO,
                assertThrows(TaxiApiException.class, () -> other.avanzarServicio(123, "EN_CAMINO"))
                        .codigo);
        assertEquals(
                PROHIBIDO, assertThrows(TaxiApiException.class, () -> driver.verQr(123)).codigo);
    }

    @Test
    public void aeropuertoDebeCorresponderALaCiudad() {
        assertEquals(
                AEROPUERTO_NO_CORRESPONDE,
                assertThrows(
                                TaxiApiException.class,
                                () -> client.crearServicio(124, "CUZ", "15:30", 1, 0))
                        .codigo);
        assertEquals(5, client.listarAeropuertos().optJSONArray("aeropuertos").length());
    }

    @Test
    public void disponibilidadNoInterrumpeViaje() {
        driver.aceptarServicio(123);
        driver.cambiarDisponibilidad(false);
        assertEquals("EN_CAMINO", driver.avanzarServicio(123, "EN_CAMINO").optString("estado"));
    }

    @Test
    public void ubicacionSoloDuranteViajeActivo() {
        stateToFinal();
        driver.enviarUbicacion(123, -12.1, -77.1, 8.0);
        assertNotNull(client.verServicio(123).optJSONObject("ubicacion"));
        driver.finalizarServicio(123, client.verQr(123).optString("codigo"));
        assertEquals(
                ESTADO_INVALIDO,
                assertThrows(
                                TaxiApiException.class,
                                () -> driver.enviarUbicacion(123, -12, -77, null))
                        .codigo);
    }

    @Test
    public void noCalificaAntesDeFinalizar() {
        assertEquals(
                ESTADO_INVALIDO,
                assertThrows(TaxiApiException.class, () -> client.calificarServicio(123, 5, null))
                        .codigo);
    }

    @Test
    public void objetosDevueltosNoModificanAlmacen() throws Exception {
        JSONObject s = client.verServicio(123);
        s.put("estado", "FINALIZADO");
        assertEquals("SOLICITADO", client.verServicio(123).optString("estado"));
    }

    @Test
    public void servicioAusenteDa404() {
        assertEquals(
                404, assertThrows(TaxiApiException.class, () -> client.verServicio(999)).status);
    }
}
