package com.example.proyecto_iotelito.data.taxiapi;

import static com.example.proyecto_iotelito.data.taxiapi.JsonTaxiApi.json;
import static com.example.proyecto_iotelito.data.taxiapi.TaxiApiException.Codigo.*;

import org.json.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Simulador explícito de debug. Datos aislados en memoria; nunca escribe en Firebase. Cada
 * operación completa mantiene el monitor del almacén, incluyendo las comprobaciones.
 */
public final class FakeTaxiApi implements TaxiApi {
    public static final class Caller {
        public final String uid, role, name, email;
        public final Integer hotelId;

        public Caller(String uid, String role, String name, String email, Integer hotelId) {
            this.uid = uid;
            this.role = role;
            this.name = name;
            this.email = email;
            this.hotelId = hotelId;
        }
    }

    public static final class Booking {
        public final String owner, name;
        public final JSONObject hotel;

        public Booking(String owner, String name, JSONObject hotel) {
            this.owner = owner;
            this.name = name;
            this.hotel = hotel;
        }
    }

    public interface Bookings {
        Booking obtener(long id, Caller caller);
    }

    public static final class Store {
        private final Map<Long, Trip> trips = new HashMap<>();
        private final Map<String, JSONObject> drivers = new HashMap<>();
        private long nextDriver = 1;
    }

    private static final class Trip {
        JSONObject data;
        String owner, driver, qr;
        final List<Mensaje> messages = new ArrayList<>();
    }

    private static final class Mensaje {
        final String uid, uuid, texto;

        Mensaje(String uid, String uuid, String texto) {
            this.uid = uid;
            this.uuid = uuid;
            this.texto = texto;
        }
    }

    @Override
    public JSONObject mensajes(long id) {
        synchronized (store) {
            Caller c = caller();
            return conversacion(chat(id, c), c);
        }
    }

    @Override
    public JSONObject enviarMensaje(long id, String uuid, String texto) {
        synchronized (store) {
            Caller c = caller();
            Trip t = chat(id, c);
            if (!active(t)) throw error(409, ESTADO_INVALIDO, "El viaje terminó.");
            String clean = texto == null ? "" : texto.trim();
            if (clean.isEmpty() || clean.length() > 1000)
                throw error(400, VALIDACION, "Escribe un mensaje de hasta 1000 caracteres.");
            String key;
            try {
                key = UUID.fromString(uuid).toString();
                if (uuid.length() != 36) throw new IllegalArgumentException();
            } catch (IllegalArgumentException | NullPointerException ex) {
                throw error(400, VALIDACION, "El identificador debe ser un UUID válido.");
            }
            if (t.messages.stream().noneMatch(m -> m.uid.equals(c.uid) && m.uuid.equals(key))) {
                t.messages.add(new Mensaje(c.uid, key, clean));
            }
            return conversacion(t, c);
        }
    }

    @Override
    public void registrarDispositivo(String binding, String token) {
        // El simulador no registra dispositivos ni envía avisos remotos.
    }

    private Trip chat(long id, Caller c) {
        Trip t = trip(id);
        if ("TAXISTA".equals(c.role)) {
            assigned(t, c);
        } else {
            owner(t, c);
            if (t.driver == null) throw error(409, ESTADO_INVALIDO, "El chat se abre al asignar un taxista.");
        }
        return t;
    }

    private JSONObject conversacion(Trip t, Caller c) {
        JSONArray rows = new JSONArray();
        for (int i = Math.max(0, t.messages.size() - 100); i < t.messages.size(); i++) {
            Mensaje m = t.messages.get(i);
            rows.put(json("id", i + 1, "texto", m.texto, "mio", m.uid.equals(c.uid)));
        }
        return json("mensajes", rows, "permiteEnviar", active(t));
    }

    private final Store store;
    private final Supplier<Caller> identity;
    private final Bookings bookings;

    public FakeTaxiApi(Store store, Supplier<Caller> identity, Bookings bookings) {
        this.store = store;
        this.identity = identity;
        this.bookings = bookings;
    }

    private static String now() {
        return Instant.now().toString();
    }

    private static JSONObject copy(JSONObject j) {
        try {
            return new JSONObject(j.toString());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TaxiApiException error(int status, TaxiApiException.Codigo code, String text) {
        return new TaxiApiException(status, code, text);
    }

    private Caller caller() {
        Caller c = identity.get();
        if (c == null || c.uid == null)
            throw error(401, NO_AUTENTICADO, "Inicia sesión nuevamente.");
        return c;
    }

    private Caller role(String role) {
        Caller c = caller();
        if (!role.equals(c.role))
            throw error(403, PROHIBIDO, "Tu cuenta no puede realizar esta operación.");
        return c;
    }

    private JSONObject driver(Caller c) {
        if (!"TAXISTA".equals(c.role))
            throw error(403, PROHIBIDO, "Se requiere una cuenta de taxista.");
        JSONObject p = store.drivers.get(c.uid);
        if (p == null) {
            p =
                    json(
                            "id",
                            store.nextDriver++,
                            "nombres",
                            c.name,
                            "apellidos",
                            "Prueba",
                            "telefono",
                            "",
                            "fotoUrl",
                            null,
                            "vehiculo",
                            json(
                                    "placa",
                                    "DEMO-001",
                                    "marca",
                                    "Toyota",
                                    "modelo",
                                    "Yaris",
                                    "color",
                                    "Blanco",
                                    "anio",
                                    2020,
                                    "fotoUrl",
                                    null),
                            "calificacion",
                            0,
                            "cantidadServicios",
                            0,
                            "correo",
                            c.email,
                            "estadoSolicitud",
                            "APROBADO",
                            "activo",
                            true,
                            "disponible",
                            false,
                            "servicioActivoId",
                            null);
            store.drivers.put(c.uid, p);
        }
        if (!p.optBoolean("activo") || !"APROBADO".equals(p.optString("estadoSolicitud")))
            throw error(403, TAXISTA_NO_HABILITADO, "Tu cuenta de taxista no está habilitada.");
        return p;
    }

    private Trip trip(long id) {
        Trip t = store.trips.get(id);
        if (t == null) throw error(404, NO_ENCONTRADO, "La reserva no tiene servicio de taxi.");
        return t;
    }

    private static boolean active(Trip t) {
        return Arrays.asList("ASIGNADO", "EN_CAMINO", "EN_TRASLADO")
                .contains(t.data.optString("estado"));
    }

    private void assigned(Trip t, Caller c) {
        driver(c);
        if (!c.uid.equals(t.driver))
            throw error(403, PROHIBIDO, "El servicio pertenece a otro taxista.");
    }

    private void owner(Trip t, Caller c) {
        if (!("CLIENT".equals(c.role) && c.uid.equals(t.owner)))
            throw error(403, PROHIBIDO, "El servicio pertenece a otra cuenta.");
    }

    private boolean hotelAdmin(Trip t, Caller c) {
        return "HOTEL_ADMIN".equals(c.role)
                && c.hotelId != null
                && c.hotelId == t.data.optJSONObject("hotel").optInt("id");
    }

    private JSONObject publicDriver(JSONObject p) {
        JSONObject j = copy(p);
        for (String k :
                new String[] {
                    "correo", "estadoSolicitud", "activo", "disponible", "servicioActivoId"
                }) j.remove(k);
        return j;
    }

    private JSONObject view(Trip t) {
        JSONObject j = copy(t.data);
        if (t.driver != null)
            try {
                j.put("taxista", publicDriver(store.drivers.get(t.driver)));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        return j;
    }

    private void state(Trip t, String estado) {
        try {
            t.data.put("estado", estado);
            String field =
                    estado.equals("ASIGNADO")
                            ? "asignado"
                            : estado.equals("EN_CAMINO")
                                    ? "enCamino"
                                    : estado.equals("EN_TRASLADO") ? "enTraslado" : "finalizado";
            t.data.getJSONObject("fechas").put(field, now());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public JSONObject health() {
        return json("ok", true);
    }

    public JSONObject listarAeropuertos() {
        JSONArray a = new JSONArray();
        String[][] rows = {
            {
                "LIM",
                "Aeropuerto Internacional Jorge Chávez",
                "Callao",
                "lima",
                "lima metropolitana",
                "callao"
            },
            {"CUZ", "Aeropuerto Internacional Alejandro Velasco Astete", "Cusco", "cusco", "cuzco"},
            {"AQP", "Aeropuerto Internacional Alfredo Rodríguez Ballón", "Arequipa", "arequipa"},
            {
                "TRU",
                "Aeropuerto Internacional Capitán FAP Carlos Martínez de Pinillos",
                "Trujillo",
                "trujillo"
            },
            {"PIO", "Aeropuerto Capitán FAP Renán Elías Olivera", "Pisco", "paracas", "pisco"}
        };
        for (String[] r : rows) {
            JSONArray cities = new JSONArray();
            for (int i = 3; i < r.length; i++) cities.put(r[i]);
            a.put(json("codigo", r[0], "nombre", r[1], "ciudad", r[2], "ciudadesHotel", cities));
        }
        return json("aeropuertos", a);
    }

    public JSONObject miPerfilTaxista() {
        synchronized (store) {
            return copy(driver(role("TAXISTA")));
        }
    }

    public JSONObject cambiarDisponibilidad(boolean v) {
        synchronized (store) {
            JSONObject p = driver(role("TAXISTA"));
            try {
                p.put("disponible", v);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return copy(p);
        }
    }

    public JSONObject miServicioActivo() {
        synchronized (store) {
            Caller c = role("TAXISTA");
            driver(c);
            for (Trip t : store.trips.values())
                if (c.uid.equals(t.driver) && active(t)) return view(t);
            return null;
        }
    }

    public JSONObject miHistorial(int limite) {
        synchronized (store) {
            Caller c = role("TAXISTA");
            driver(c);
            if (limite < 1 || limite > 100)
                throw error(400, VALIDACION, "El límite debe estar entre 1 y 100.");
            List<Trip> list = new ArrayList<>();
            for (Trip t : store.trips.values())
                if (c.uid.equals(t.driver) && "FINALIZADO".equals(t.data.optString("estado")))
                    list.add(t);
            list.sort(
                    (a, b) ->
                            b.data.optJSONObject("fechas")
                                    .optString("finalizado")
                                    .compareTo(
                                            a.data.optJSONObject("fechas")
                                                    .optString("finalizado")));
            JSONArray a = new JSONArray();
            for (int i = 0; i < Math.min(limite, list.size()); i++) a.put(view(list.get(i)));
            return json("servicios", a);
        }
    }

    public JSONObject listarDisponibles() {
        synchronized (store) {
            driver(role("TAXISTA"));
            List<Trip> list = new ArrayList<>();
            for (Trip t : store.trips.values())
                if ("SOLICITADO".equals(t.data.optString("estado"))) list.add(t);
            list.sort(Comparator.comparing(t -> t.data.optString("hora")));
            JSONArray a = new JSONArray();
            for (Trip t : list) a.put(view(t));
            return json("servicios", a);
        }
    }

    public JSONObject crearServicio(
            long id, String airport, String hora, int pasajeros, int maletas) {
        synchronized (store) {
            Caller c = role("CLIENT");
            Trip old = store.trips.get(id);
            if (old != null) {
                owner(old, c);
                return view(old);
            }
            if (id < 1
                    || hora == null
                    || !hora.matches("([01][0-9]|2[0-3]):[0-5][0-9]")
                    || pasajeros < 1
                    || maletas < 0)
                throw error(400, VALIDACION, "Revisa la hora, los pasajeros y las maletas.");
            Booking b = bookings.obtener(id, c);
            if (b == null) throw error(404, NO_ENCONTRADO, "La reserva no existe.");
            if (!c.uid.equals(b.owner))
                throw error(403, PROHIBIDO, "La reserva pertenece a otra cuenta.");
            JSONObject destino = null;
            JSONArray airports = listarAeropuertos().optJSONArray("aeropuertos");
            String city = b.hotel.optString("ciudad").trim().toLowerCase(Locale.ROOT);
            for (int i = 0; i < airports.length(); i++) {
                JSONObject a = airports.optJSONObject(i);
                if (!a.optString("codigo").equals(airport)) continue;
                JSONArray cities = a.optJSONArray("ciudadesHotel");
                for (int n = 0; n < cities.length(); n++)
                    if (city.equals(cities.optString(n)))
                        destino =
                                json(
                                        "codigo",
                                        a.optString("codigo"),
                                        "nombre",
                                        a.optString("nombre"),
                                        "ciudad",
                                        a.optString("ciudad"));
            }
            if (destino == null)
                throw error(
                        400,
                        AEROPUERTO_NO_CORRESPONDE,
                        "El aeropuerto no corresponde a la ciudad del hotel.");
            if (!c.uid.equals(caller().uid)) throw error(401, NO_AUTENTICADO, "La sesión cambió.");
            Trip t = new Trip();
            t.owner = c.uid;
            t.data =
                    json(
                            "id",
                            id,
                            "estado",
                            "SOLICITADO",
                            "hotel",
                            copy(b.hotel),
                            "aeropuerto",
                            destino,
                            "hora",
                            hora,
                            "pasajeros",
                            pasajeros,
                            "maletas",
                            maletas,
                            "clienteNombre",
                            b.name,
                            "taxista",
                            null,
                            "ubicacion",
                            null,
                            "fechas",
                            json(
                                    "solicitado",
                                    now(),
                                    "asignado",
                                    null,
                                    "enCamino",
                                    null,
                                    "enTraslado",
                                    null,
                                    "finalizado",
                                    null),
                            "calificacion",
                            null);
            store.trips.put(id, t);
            return view(t);
        }
    }

    public JSONObject verServicio(long id) {
        synchronized (store) {
            Caller c = caller();
            Trip t = trip(id);
            boolean allowed =
                    ("CLIENT".equals(c.role) && c.uid.equals(t.owner))
                            || hotelAdmin(t, c)
                            || "SUPERADMIN".equals(c.role);
            if ("TAXISTA".equals(c.role)) {
                driver(c);
                allowed = c.uid.equals(t.driver) || "SOLICITADO".equals(t.data.optString("estado"));
            }
            if (!allowed) throw error(403, PROHIBIDO, "El servicio pertenece a otra cuenta.");
            return view(t);
        }
    }

    public JSONObject aceptarServicio(long id) {
        synchronized (store) {
            Caller c = role("TAXISTA");
            JSONObject p = driver(c);
            Trip t = trip(id);
            if (c.uid.equals(t.driver)) return view(t);
            if (!"SOLICITADO".equals(t.data.optString("estado")))
                throw error(409, SERVICIO_NO_DISPONIBLE, "Otro taxista ya aceptó este pedido");
            for (Trip other : store.trips.values())
                if (c.uid.equals(other.driver) && active(other))
                    throw error(409, SERVICIO_ACTIVO, "Ya tienes un servicio activo.");
            if (!p.optBoolean("disponible"))
                throw error(
                        409,
                        TAXISTA_NO_DISPONIBLE,
                        "Activa tu disponibilidad para aceptar pedidos.");
            t.driver = c.uid;
            t.qr = "TX1-" + UUID.randomUUID();
            state(t, "ASIGNADO");
            try {
                p.put("servicioActivoId", id);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return view(t);
        }
    }

    public JSONObject avanzarServicio(long id, String next) {
        synchronized (store) {
            Caller c = role("TAXISTA");
            Trip t = trip(id);
            assigned(t, c);
            if (!Arrays.asList("EN_CAMINO", "EN_TRASLADO").contains(next))
                throw error(400, VALIDACION, "El estado solicitado no es válido.");
            String current = t.data.optString("estado");
            if (next.equals(current)) return view(t);
            if (!(current.equals("ASIGNADO") && next.equals("EN_CAMINO")
                    || current.equals("EN_CAMINO") && next.equals("EN_TRASLADO")))
                throw error(409, ESTADO_INVALIDO, "El servicio no permite ese cambio de estado.");
            state(t, next);
            return view(t);
        }
    }

    public JSONObject finalizarServicio(long id, String qr) {
        synchronized (store) {
            Caller c = role("TAXISTA");
            Trip t = trip(id);
            assigned(t, c);
            if ("FINALIZADO".equals(t.data.optString("estado"))) return view(t);
            if (!"EN_TRASLADO".equals(t.data.optString("estado")))
                throw error(409, ESTADO_INVALIDO, "El servicio todavía no está en traslado.");
            if (qr == null || !qr.equals(t.qr))
                throw error(400, QR_INVALIDO, "El código QR no corresponde a este servicio.");
            state(t, "FINALIZADO");
            JSONObject p = store.drivers.get(c.uid);
            try {
                p.put("servicioActivoId", JSONObject.NULL);
                p.put("cantidadServicios", p.optInt("cantidadServicios") + 1);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return view(t);
        }
    }

    public void enviarUbicacion(long id, double lat, double lng, Double precision) {
        synchronized (store) {
            Caller c = role("TAXISTA");
            Trip t = trip(id);
            assigned(t, c);
            if (!active(t)) throw error(409, ESTADO_INVALIDO, "El servicio ya no está activo.");
            if (!Double.isFinite(lat)
                    || !Double.isFinite(lng)
                    || lat < -90
                    || lat > 90
                    || lng < -180
                    || lng > 180
                    || (precision != null && !Double.isFinite(precision)))
                throw error(400, VALIDACION, "La ubicación no es válida.");
            try {
                t.data.put(
                        "ubicacion",
                        json(
                                "lat",
                                lat,
                                "lng",
                                lng,
                                "precisionM",
                                precision,
                                "registradaEn",
                                now()));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public JSONObject verQr(long id) {
        synchronized (store) {
            Caller c = caller();
            Trip t = trip(id);
            if (!hotelAdmin(t, c)) owner(t, c);
            if (!active(t))
                throw error(409, ESTADO_INVALIDO, "El QR requiere un servicio asignado y activo.");
            return json("codigo", t.qr);
        }
    }

    public JSONObject calificarServicio(long id, int score, String comment) {
        synchronized (store) {
            Caller c = role("CLIENT");
            Trip t = trip(id);
            owner(t, c);
            if (score < 1 || score > 5 || (comment != null && comment.length() > 500))
                throw error(400, VALIDACION, "Selecciona entre 1 y 5 estrellas.");
            if (!"FINALIZADO".equals(t.data.optString("estado")))
                throw error(409, ESTADO_INVALIDO, "El servicio aún no ha finalizado.");
            if (!t.data.isNull("calificacion"))
                throw error(409, YA_CALIFICADO, "Ya calificaste este servicio.");
            try {
                t.data.put(
                        "calificacion",
                        json("puntuacion", score, "comentario", comment, "creadaEn", now()));
                double total = 0;
                int count = 0;
                for (Trip other : store.trips.values())
                    if (t.driver.equals(other.driver) && !other.data.isNull("calificacion")) {
                        total += other.data.getJSONObject("calificacion").getInt("puntuacion");
                        count++;
                    }
                store.drivers.get(t.driver).put("calificacion", total / count);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return view(t);
        }
    }
}
