package com.example.proyecto_iotelito.data.taxiapi;

import static org.junit.Assert.*;

import org.junit.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpTaxiApiTest {
    // AGP compila con módulos Android, que no exponen jdk.httpserver al compilador.
    // Reflexión únicamente en el test: en ejecución se usa el HttpServer real del JDK.
    private static final class HttpServer {
        private static final Class<?> TYPE = type("com.sun.net.httpserver.HttpServer");
        private final Object value;

        private HttpServer(Object value) {
            this.value = value;
        }

        static HttpServer create(InetSocketAddress a, int b) {
            return new HttpServer(
                    invoke(
                            TYPE,
                            null,
                            "create",
                            new Class[] {InetSocketAddress.class, int.class},
                            a,
                            b));
        }

        void setExecutor(java.util.concurrent.Executor e) {
            invoke(
                    TYPE,
                    value,
                    "setExecutor",
                    new Class[] {java.util.concurrent.Executor.class},
                    e);
        }

        void start() {
            invoke(TYPE, value, "start", new Class[] {});
        }

        void stop(int delay) {
            invoke(TYPE, value, "stop", new Class[] {int.class}, delay);
        }

        InetSocketAddress getAddress() {
            return (InetSocketAddress) invoke(TYPE, value, "getAddress", new Class[] {});
        }

        void createContext(String path, Handler handler) {
            Class<?> h = type("com.sun.net.httpserver.HttpHandler");
            Object proxy =
                    java.lang.reflect.Proxy.newProxyInstance(
                            h.getClassLoader(),
                            new Class[] {h},
                            (p, m, a) -> {
                                if (m.getName().equals("handle")) {
                                    handler.handle(new HttpExchange(a[0]));
                                    return null;
                                }
                                if (m.getName().equals("toString")) return "TestHandler";
                                return null;
                            });
            invoke(TYPE, value, "createContext", new Class[] {String.class, h}, path, proxy);
        }
    }

    private interface Handler {
        void handle(HttpExchange e) throws Exception;
    }

    private static final class HttpExchange {
        private static final Class<?> TYPE = type("com.sun.net.httpserver.HttpExchange");
        private final Object value;

        HttpExchange(Object v) {
            value = v;
        }

        String getRequestMethod() {
            return (String) invoke(TYPE, value, "getRequestMethod", new Class[] {});
        }

        Headers getRequestHeaders() {
            return new Headers(invoke(TYPE, value, "getRequestHeaders", new Class[] {}));
        }

        Headers getResponseHeaders() {
            return new Headers(invoke(TYPE, value, "getResponseHeaders", new Class[] {}));
        }

        InputStream getRequestBody() {
            return (InputStream) invoke(TYPE, value, "getRequestBody", new Class[] {});
        }

        OutputStream getResponseBody() {
            return (OutputStream) invoke(TYPE, value, "getResponseBody", new Class[] {});
        }

        void sendResponseHeaders(int status, long length) {
            invoke(
                    TYPE,
                    value,
                    "sendResponseHeaders",
                    new Class[] {int.class, long.class},
                    status,
                    length);
        }

        void close() {
            invoke(TYPE, value, "close", new Class[] {});
        }
    }

    private static final class Headers {
        private final Object value;

        Headers(Object v) {
            value = v;
        }

        String getFirst(String k) {
            return (String)
                    invoke(
                            type("com.sun.net.httpserver.Headers"),
                            value,
                            "getFirst",
                            new Class[] {String.class},
                            k);
        }

        void set(String k, String v) {
            invoke(
                    type("com.sun.net.httpserver.Headers"),
                    value,
                    "set",
                    new Class[] {String.class, String.class},
                    k,
                    v);
        }
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object invoke(
            Class<?> type, Object target, String name, Class<?>[] args, Object... values) {
        try {
            return type.getMethod(name, args).invoke(target, values);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private HttpServer server;
    private ExecutorService pool;
    private String base;

    @Before
    public void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        pool = Executors.newCachedThreadPool();
        server.setExecutor(pool);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
    }

    @After
    public void close() {
        server.stop(0);
        pool.shutdownNow();
    }

    private void reply(HttpExchange e, int status, String text) throws java.io.IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        e.sendResponseHeaders(status, status == 204 ? -1 : b.length);
        if (status != 204) e.getResponseBody().write(b);
        e.close();
    }

    private HttpTaxiApi api() {
        return new HttpTaxiApi(base, (refresh, ms) -> "test-token", 1000, true);
    }

    @Test
    public void enviaBearerYJsonDelContrato() {
        server.createContext(
                "/api/v1/servicios",
                e -> {
                    assertEquals("POST", e.getRequestMethod());
                    assertEquals(
                            "Bearer test-token", e.getRequestHeaders().getFirst("Authorization"));
                    org.json.JSONObject b =
                            new org.json.JSONObject(
                                    new String(
                                            e.getRequestBody().readAllBytes(),
                                            StandardCharsets.UTF_8));
                    assertEquals(123, b.getLong("reservaId"));
                    assertEquals("CUZ", b.getString("aeropuertoCodigo"));
                    reply(e, 201, "{\"id\":123,\"estado\":\"SOLICITADO\"}");
                });
        assertEquals(123, api().crearServicio(123, "CUZ", "15:30", 2, 1).optLong("id"));
    }

    @Test
    public void renuevaTokenUnaVezCon401() {
        AtomicInteger requests = new AtomicInteger();
        List<Boolean> refreshes = new ArrayList<>();
        server.createContext(
                "/api/v1/taxistas/me",
                e -> {
                    int n = requests.incrementAndGet();
                    if (n == 1)
                        reply(
                                e,
                                401,
                                "{\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Sesión vencida\"}");
                    else {
                        assertEquals(
                                "Bearer fresh", e.getRequestHeaders().getFirst("Authorization"));
                        reply(e, 200, "{\"id\":7}");
                    }
                });
        HttpTaxiApi a =
                new HttpTaxiApi(
                        base,
                        (refresh, ms) -> {
                            refreshes.add(refresh);
                            return refresh ? "fresh" : "old";
                        },
                        1000,
                        true);
        assertEquals(7, a.miPerfilTaxista().optInt("id"));
        assertEquals(Arrays.asList(false, true), refreshes);
        assertEquals(2, requests.get());
    }

    @Test
    public void segundo401NoGeneraBucle() {
        AtomicInteger n = new AtomicInteger();
        server.createContext(
                "/api/v1/taxistas/me",
                e -> {
                    n.incrementAndGet();
                    reply(e, 401, "{\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Inicia sesión\"}");
                });
        TaxiApiException e = assertThrows(TaxiApiException.class, () -> api().miPerfilTaxista());
        assertEquals(401, e.status);
        assertEquals(2, n.get());
    }

    @Test
    public void conserva409YCodigo() {
        server.createContext(
                "/api/v1/servicios/123/aceptar",
                e ->
                        reply(
                                e,
                                409,
                                "{\"codigo\":\"SERVICIO_NO_DISPONIBLE\",\"mensaje\":\"Otro taxista"
                                    + " ya aceptó este pedido\"}"));
        TaxiApiException e = assertThrows(TaxiApiException.class, () -> api().aceptarServicio(123));
        assertEquals(409, e.status);
        assertEquals(TaxiApiException.Codigo.SERVICIO_NO_DISPONIBLE, e.codigo);
        assertEquals("Otro taxista ya aceptó este pedido", e.getMessage());
    }

    @Test
    public void timeoutEsNoDisponible() {
        server.createContext(
                "/api/v1/health",
                e -> {
                    try {
                        Thread.sleep(500);
                        reply(e, 200, "{\"ok\":true}");
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                });
        HttpTaxiApi a = new HttpTaxiApi(base, (r, m) -> "x", 100, true);
        TaxiApiException e = assertThrows(TaxiApiException.class, a::health);
        assertEquals(TaxiApiException.Codigo.NO_DISPONIBLE, e.codigo);
        assertEquals("El servicio de taxis no está disponible", e.getMessage());
    }

    @Test
    public void sinRedEsNoDisponible() throws Exception {
        server.stop(0);
        TaxiApiException e = assertThrows(TaxiApiException.class, () -> api().health());
        assertEquals(0, e.status);
        assertEquals(TaxiApiException.Codigo.NO_DISPONIBLE, e.codigo);
    }

    @Test
    public void respuesta204EsNull() {
        server.createContext("/api/v1/taxistas/me/servicio-activo", e -> reply(e, 204, ""));
        assertNull(api().miServicioActivo());
    }

    @Test
    public void catalogoNoEnviaToken() {
        server.createContext(
                "/api/v1/aeropuertos",
                e -> {
                    assertNull(e.getRequestHeaders().getFirst("Authorization"));
                    reply(e, 200, "{\"aeropuertos\":[]}");
                });
        assertEquals(0, api().listarAeropuertos().optJSONArray("aeropuertos").length());
    }

    @Test
    public void noSigueRedireccionNiFiltraToken() {
        server.createContext(
                "/api/v1/taxistas/me",
                e -> {
                    e.getResponseHeaders().set("Location", "http://127.0.0.1:1/otra");
                    reply(e, 302, "{}");
                });
        assertThrows(TaxiApiException.class, () -> api().miPerfilTaxista());
    }

    @Test
    public void produccionRechazaHttp() {
        assertThrows(IllegalArgumentException.class, () -> new HttpTaxiApi(base, (r, m) -> "x"));
    }

    @Test
    public void jsonInvalidoSeInformaSinRomper() {
        server.createContext("/api/v1/health", e -> reply(e, 200, "<html>error</html>"));
        assertEquals(
                TaxiApiException.Codigo.NO_DISPONIBLE,
                assertThrows(TaxiApiException.class, () -> api().health()).codigo);
    }
}
