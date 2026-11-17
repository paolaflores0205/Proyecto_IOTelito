package com.example.proyecto_iotelito.data.taxiapi;

import org.json.JSONObject;

/**
 * Operaciones síncronas de api-taxis-v1.yaml. Invocar siempre fuera del hilo UI. Los objetos
 * conservan exactamente los nombres JSON del contrato.
 */
public interface TaxiApi {
    JSONObject health();
    JSONObject listarTaxistas(String estadoSolicitud);
    JSONObject cambiarActivoTaxista(long id, boolean activo, String motivo);

    JSONObject listarAeropuertos();

    JSONObject miPerfilTaxista();

    JSONObject cambiarDisponibilidad(boolean disponible);

    JSONObject miServicioActivo(); // null corresponde a HTTP 204

    JSONObject miHistorial(int limite);

    JSONObject listarDisponibles();

    JSONObject crearServicio(
            long reservaId, String aeropuertoCodigo, String hora, int pasajeros, int maletas);

    JSONObject verServicio(long id);

    JSONObject aceptarServicio(long id);

    JSONObject avanzarServicio(long id, String estado);

    JSONObject finalizarServicio(long id, String codigoQr);

    void enviarUbicacion(long id, double lat, double lng, Double precisionM);

    JSONObject verQr(long id);

    JSONObject calificarServicio(long id, int puntuacion, String comentario);
}
