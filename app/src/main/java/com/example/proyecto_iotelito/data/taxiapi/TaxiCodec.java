package com.example.proyecto_iotelito.data.taxiapi;

import org.json.*;

import java.time.Instant;

/** Adaptador a las vistas heredadas; no cambia los cuerpos del contrato. */
public final class TaxiCodec {
    private TaxiCodec() {}

    public static long timestamp(JSONObject j, String campo) {
        try {
            return Instant.parse(j.optString(campo)).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    public static JSONObject legacy(JSONObject s, String clienteEmail, boolean cliente) {
        if (s == null) return null;
        JSONObject hotel = s.optJSONObject("hotel"),
                airport = s.optJSONObject("aeropuerto"),
                driver = s.optJSONObject("taxista"),
                dates = s.optJSONObject("fechas");
        String estado = s.optString("estado");
        if (cliente && "EN_TRASLADO".equals(estado)) estado = "EN_CAMINO";
        return JsonTaxiApi.json(
                "id",
                Math.toIntExact(s.optLong("id")),
                "reserva",
                Math.toIntExact(s.optLong("id")),
                "cliente",
                clienteEmail,
                "nombre",
                s.optString("clienteNombre"),
                "hotel",
                hotel.optString("nombre"),
                "origen",
                hotel.optString("direccion"),
                "destino",
                airport.optString("nombre"),
                "hora",
                s.optString("hora"),
                "pasajeros",
                s.optInt("pasajeros"),
                "maletas",
                s.optInt("maletas"),
                "estado",
                estado,
                "creado",
                timestamp(dates, "solicitado"),
                "finalizado",
                timestamp(dates, "finalizado"),
                "taxista",
                driver == null ? "" : String.valueOf(driver.optLong("id")),
                "conductor",
                driver == null
                        ? ""
                        : driver.optString("nombres") + " " + driver.optString("apellidos"));
    }
}
