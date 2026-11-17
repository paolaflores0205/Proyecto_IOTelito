package com.example.proyecto_iotelito.data.taxiapi;

import org.json.*;

/** Traducción única de las operaciones a rutas/cuerpos del contrato. */
public abstract class JsonTaxiApi implements TaxiApi {
    protected abstract JSONObject request(
            String metodo, String ruta, JSONObject body, boolean autenticada);

    public static JSONObject json(Object... pares) {
        JSONObject j = new JSONObject();
        try {
            for (int i = 0; i < pares.length; i += 2)
                j.put((String) pares[i], pares[i + 1] == null ? JSONObject.NULL : pares[i + 1]);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return j;
    }

    public JSONObject health() {
        return request("GET", "/health", null, false);
    }

    public JSONObject listarAeropuertos() {
        return request("GET", "/aeropuertos", null, false);
    }

    public JSONObject miPerfilTaxista() {
        return request("GET", "/taxistas/me", null, true);
    }

    public JSONObject cambiarDisponibilidad(boolean v) {
        return request("PUT", "/taxistas/me/disponibilidad", json("disponible", v), true);
    }

    public JSONObject miServicioActivo() {
        return request("GET", "/taxistas/me/servicio-activo", null, true);
    }

    public JSONObject miHistorial(int limite) {
        return request("GET", "/taxistas/me/servicios?limite=" + limite, null, true);
    }

    public JSONObject listarDisponibles() {
        return request("GET", "/servicios/disponibles", null, true);
    }

    public JSONObject crearServicio(long id, String a, String h, int p, int m) {
        return request(
                "POST",
                "/servicios",
                json(
                        "reservaId",
                        id,
                        "aeropuertoCodigo",
                        a,
                        "hora",
                        h,
                        "pasajeros",
                        p,
                        "maletas",
                        m),
                true);
    }

    public JSONObject verServicio(long id) {
        return request("GET", "/servicios/" + id, null, true);
    }

    public JSONObject aceptarServicio(long id) {
        return request("POST", "/servicios/" + id + "/aceptar", null, true);
    }

    public JSONObject avanzarServicio(long id, String e) {
        return request("POST", "/servicios/" + id + "/estado", json("estado", e), true);
    }

    public JSONObject finalizarServicio(long id, String c) {
        return request("POST", "/servicios/" + id + "/finalizar", json("codigoQr", c), true);
    }

    public void enviarUbicacion(long id, double lat, double lng, Double p) {
        request(
                "PUT",
                "/servicios/" + id + "/ubicacion",
                json("lat", lat, "lng", lng, "precisionM", p),
                true);
    }

    public JSONObject verQr(long id) {
        return request("GET", "/servicios/" + id + "/qr", null, true);
    }

    public JSONObject calificarServicio(long id, int p, String c) {
        return request(
                "POST",
                "/servicios/" + id + "/calificacion",
                json("puntuacion", p, "comentario", c),
                true);
    }
}
