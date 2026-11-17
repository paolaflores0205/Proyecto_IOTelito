package com.iotelito.taxiweb.api;

import java.util.List;

/** Cuerpos JSON de la API, campo por campo como en docs/api-taxis-v1.yaml. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record Error(String codigo, String mensaje) {
    }

    public record Salud(boolean ok) {
    }

    public record Aeropuerto(String codigo, String nombre, String ciudad, List<String> ciudadesHotel) {
    }

    public record AeropuertoLista(List<Aeropuerto> aeropuertos) {
    }

    public record AeropuertoDestino(String codigo, String nombre, String ciudad) {
    }

    public record Vehiculo(String placa, String marca, String modelo, String color, Integer anio, String fotoUrl) {
    }

    public record TaxistaPublico(Long id, String nombres, String apellidos, String telefono, String fotoUrl,
                                 Vehiculo vehiculo, double calificacion, int cantidadServicios) {
    }

    public record TaxistaPerfil(Long id, String nombres, String apellidos, String telefono, String fotoUrl,
                                Vehiculo vehiculo, double calificacion, int cantidadServicios,
                                String correo, String estadoSolicitud, boolean activo, boolean disponible,
                                Long servicioActivoId) {
    }

    public record TaxistaAdmin(Long id, String nombres, String apellidos, String telefono, String fotoUrl,
                               Vehiculo vehiculo, double calificacion, int cantidadServicios,
                               String correo, String estadoSolicitud, boolean activo, boolean disponible,
                               Long servicioActivoId, String tipoDocumento, String numeroDocumento,
                               String fechaSolicitud) {
    }

    public record TaxistaAdminLista(List<TaxistaAdmin> taxistas) {
    }

    public record Hotel(Long id, String nombre, String direccion, String ciudad) {
    }

    public record Ubicacion(double lat, double lng, Double precisionM, String registradaEn) {
    }

    public record Fechas(String solicitado, String asignado, String enCamino, String enTraslado, String finalizado) {
    }

    public record Calificacion(int puntuacion, String comentario, String creadaEn) {
    }

    public record Servicio(long id, String estado, Hotel hotel, AeropuertoDestino aeropuerto, String hora,
                           int pasajeros, int maletas, String clienteNombre, TaxistaPublico taxista,
                           Ubicacion ubicacion, Fechas fechas, Calificacion calificacion) {
    }

    public record ServicioLista(List<Servicio> servicios) {
    }

    public record QrServicio(String codigo) {
    }

    // Cuerpos que envía la app. Todo llega como objeto para distinguir "falta" de "cero".

    public record SolicitudNueva(Long reservaId, String aeropuertoCodigo, String hora, Integer pasajeros,
                                 Integer maletas) {
    }

    public record Disponibilidad(Boolean disponible) {
    }

    public record CambioEstado(String estado) {
    }

    public record Finalizacion(String codigoQr) {
    }

    public record UbicacionNueva(Double lat, Double lng, Double precisionM) {
    }

    public record CalificacionNueva(Integer puntuacion, String comentario) {
    }

    public record CambioActivo(Boolean activo, String motivo) {
    }
}
