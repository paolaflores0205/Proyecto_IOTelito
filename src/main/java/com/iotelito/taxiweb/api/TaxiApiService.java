package com.iotelito.taxiweb.api;

import com.iotelito.taxiweb.api.ApiDtos.CalificacionNueva;
import com.iotelito.taxiweb.api.ApiDtos.CambioActivo;
import com.iotelito.taxiweb.api.ApiDtos.CambioEstado;
import com.iotelito.taxiweb.api.ApiDtos.Disponibilidad;
import com.iotelito.taxiweb.api.ApiDtos.Finalizacion;
import com.iotelito.taxiweb.api.ApiDtos.QrServicio;
import com.iotelito.taxiweb.api.ApiDtos.Servicio;
import com.iotelito.taxiweb.api.ApiDtos.ServicioLista;
import com.iotelito.taxiweb.api.ApiDtos.SolicitudNueva;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaAdmin;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaAdminLista;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaPerfil;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaPublico;
import com.iotelito.taxiweb.api.ApiDtos.UbicacionNueva;
import com.iotelito.taxiweb.model.CalificacionTaxista;
import com.iotelito.taxiweb.model.CodigoAeropuerto;
import com.iotelito.taxiweb.model.EstadoServicioTaxi;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxi;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.LogSistema;
import com.iotelito.taxiweb.model.ServicioTaxi;
import com.iotelito.taxiweb.model.SolicitudTaxi;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.UbicacionTaxista;
import com.iotelito.taxiweb.model.Usuario;
import com.iotelito.taxiweb.repository.CalificacionTaxistaRepository;
import com.iotelito.taxiweb.repository.LogSistemaRepository;
import com.iotelito.taxiweb.repository.ServicioTaxiRepository;
import com.iotelito.taxiweb.repository.SolicitudTaxiRepository;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.iotelito.taxiweb.repository.UbicacionTaxistaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.iotelito.taxiweb.api.Llamante.ADMIN_HOTEL;
import static com.iotelito.taxiweb.api.Llamante.CLIENTE;
import static com.iotelito.taxiweb.api.Llamante.SUPERADMIN;
import static com.iotelito.taxiweb.api.Llamante.TAXISTA;
import static com.iotelito.taxiweb.model.EstadoServicioTaxi.ASIGNADO;
import static com.iotelito.taxiweb.model.EstadoServicioTaxi.EN_CAMINO;
import static com.iotelito.taxiweb.model.EstadoServicioTaxi.EN_TRASLADO;
import static com.iotelito.taxiweb.model.EstadoServicioTaxi.FINALIZADO;

/** Reglas de la API de taxis (docs/api-taxis-v1.yaml). El id público de un servicio es su reserva. */
@Service
public class TaxiApiService {

    /** Las fechas se guardan en hora de Lima y la API las entrega en UTC. */
    static final ZoneId LIMA = ZoneId.of("America/Lima");

    private static final Pattern HORA = Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]");
    private static final Set<String> ROLES = Set.of(CLIENTE, TAXISTA, ADMIN_HOTEL, SUPERADMIN);
    private static final List<EstadoServicioTaxi> ACTIVOS = List.of(ASIGNADO, EN_CAMINO, EN_TRASLADO);

    private final SecureRandom azar = new SecureRandom();
    private final FirestoreLector firestore;
    private final TaxistaRepository taxistas;
    private final SolicitudTaxiRepository solicitudes;
    private final ServicioTaxiRepository servicios;
    private final UbicacionTaxistaRepository ubicaciones;
    private final CalificacionTaxistaRepository calificaciones;
    private final LogSistemaRepository logs;
    private final com.iotelito.taxiweb.firebase.CuentasFirebase cuentasFirebase;

    public TaxiApiService(FirestoreLector firestore, TaxistaRepository taxistas, SolicitudTaxiRepository solicitudes,
                          ServicioTaxiRepository servicios, UbicacionTaxistaRepository ubicaciones,
                          CalificacionTaxistaRepository calificaciones, LogSistemaRepository logs,
                          com.iotelito.taxiweb.firebase.CuentasFirebase cuentasFirebase) {
        this.cuentasFirebase = cuentasFirebase;
        this.firestore = firestore;
        this.taxistas = taxistas;
        this.solicitudes = solicitudes;
        this.servicios = servicios;
        this.ubicaciones = ubicaciones;
        this.calificaciones = calificaciones;
        this.logs = logs;
    }

    // ------------------------------------------------------------------ quién llama

    /** Perfil users/{uid} de Firestore, leído con el token del usuario (mismas reglas que la app). */
    public Llamante llamante(Jwt jwt) {
        String uid = jwt.getSubject();
        Map<String, Object> perfil = firestore.documento("users/" + uid, jwt.getTokenValue())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil en IoTelito."));
        String rol = texto(perfil.get("role"));
        if (!uid.equals(perfil.get("uid")) || !ROLES.contains(rol)) {
            throw ApiException.prohibido("Tu cuenta no tiene un rol válido.");
        }
        if (!Boolean.TRUE.equals(perfil.get("active"))) {
            throw ApiException.prohibido("Tu cuenta está inactiva.");
        }
        String correo = jwt.getClaimAsString("email");
        return new Llamante(uid, correo == null ? null : correo.trim().toLowerCase(Locale.ROOT), rol,
                numero(perfil.get("hotelId")), texto(perfil.get("name")), jwt.getTokenValue());
    }

    // ------------------------------------------------------------------ taxista

    @Transactional
    public TaxistaPerfil miPerfil(Llamante llamante) {
        return perfil(taxistaHabilitado(llamante));
    }

    @Transactional
    public TaxistaPerfil cambiarDisponibilidad(Llamante llamante, Disponibilidad cuerpo) {
        if (cuerpo == null || cuerpo.disponible() == null) {
            throw ApiException.validacion("Indica si estás disponible.");
        }
        Taxista taxista = taxistaHabilitado(llamante);
        taxista.setDisponible(cuerpo.disponible());
        return perfil(taxista);
    }

    @Transactional
    public Optional<Servicio> miServicioActivo(Llamante llamante) {
        return activo(taxistaHabilitado(llamante)).map(s -> servicio(s.getSolicitud(), s));
    }

    @Transactional
    public ServicioLista miHistorial(Llamante llamante, int limite) {
        if (limite < 1 || limite > 100) {
            throw ApiException.validacion("El límite va de 1 a 100.");
        }
        Taxista taxista = taxistaHabilitado(llamante);
        return new ServicioLista(servicios
                .findByTaxistaAndEstadoOrderByHoraFinDesc(taxista, FINALIZADO, PageRequest.of(0, limite))
                .stream().map(s -> servicio(s.getSolicitud(), s)).toList());
    }

    @Transactional
    public ServicioLista disponibles(Llamante llamante) {
        taxistaHabilitado(llamante);
        return new ServicioLista(solicitudes.findByEstadoOrderByHoraRecojoAsc(EstadoSolicitudTaxi.PENDIENTE)
                .stream().map(s -> servicio(s, null)).toList());
    }

    /** Gana un solo taxista: si dos aceptan a la vez, la base rechaza al segundo. */
    @Transactional
    public Servicio aceptar(Llamante llamante, long reservaId) {
        Taxista taxista = taxistaHabilitado(llamante);
        SolicitudTaxi solicitud = solicitud(reservaId);
        Optional<ServicioTaxi> existente = servicios.findBySolicitud(solicitud);
        if (existente.isPresent()) {
            if (existente.get().getTaxista().getId().equals(taxista.getId())) {
                return servicio(solicitud, existente.get());
            }
            throw ApiException.conflicto("SERVICIO_NO_DISPONIBLE", "Otro taxista ya aceptó este pedido.");
        }
        if (solicitud.getEstado() != EstadoSolicitudTaxi.PENDIENTE) {
            throw ApiException.conflicto("SERVICIO_NO_DISPONIBLE", "Este pedido ya no está disponible.");
        }
        if (!Boolean.TRUE.equals(taxista.getDisponible())) {
            throw ApiException.conflicto("TAXISTA_NO_DISPONIBLE", "Activa tu disponibilidad para aceptar pedidos.");
        }
        if (activo(taxista).isPresent()) {
            throw ApiException.conflicto("SERVICIO_ACTIVO", "Ya tienes un servicio en curso.");
        }

        ServicioTaxi nuevo = new ServicioTaxi();
        nuevo.setSolicitud(solicitud);
        nuevo.setTaxista(taxista);
        nuevo.setEstado(ASIGNADO);
        nuevo.setHoraAceptacion(ahora());
        nuevo.setCodigoQr(codigoQr());
        solicitud.setEstado(EstadoSolicitudTaxi.ACEPTADA);
        try {
            servicios.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = String.valueOf(carrera.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detalle.contains("uq_servicios_taxi_activo")) {
                throw ApiException.conflicto("SERVICIO_ACTIVO", "Ya tienes un servicio en curso.");
            }
            throw ApiException.conflicto("SERVICIO_NO_DISPONIBLE", "Otro taxista ya aceptó este pedido.");
        }
        return servicio(solicitud, nuevo);
    }

    @Transactional
    public Servicio avanzar(Llamante llamante, long reservaId, CambioEstado cuerpo) {
        String pedido = cuerpo == null ? null : cuerpo.estado();
        if (!"EN_CAMINO".equals(pedido) && !"EN_TRASLADO".equals(pedido)) {
            throw ApiException.validacion("El estado debe ser EN_CAMINO o EN_TRASLADO.");
        }
        EstadoServicioTaxi siguiente = EstadoServicioTaxi.valueOf(pedido);
        ServicioTaxi servicio = servicioDe(taxistaHabilitado(llamante), reservaId);
        if (servicio.getEstado() == siguiente) {
            return servicio(servicio.getSolicitud(), servicio);
        }
        boolean valido = (servicio.getEstado() == ASIGNADO && siguiente == EN_CAMINO)
                || (servicio.getEstado() == EN_CAMINO && siguiente == EN_TRASLADO);
        if (!valido) {
            throw ApiException.conflicto("ESTADO_INVALIDO",
                    "No se puede pasar de " + servicio.getEstado() + " a " + siguiente + ".");
        }
        servicio.setEstado(siguiente);
        if (siguiente == EN_CAMINO) {
            servicio.setHoraInicio(ahora());
        } else {
            servicio.setHoraTraslado(ahora());
        }
        return servicio(servicio.getSolicitud(), servicio);
    }

    /** FINALIZADO solo con el código del QR que muestra el cliente. */
    @Transactional
    public Servicio finalizar(Llamante llamante, long reservaId, Finalizacion cuerpo) {
        Taxista taxista = taxistaHabilitado(llamante);
        ServicioTaxi servicio = servicioDe(taxista, reservaId);
        if (servicio.getEstado() == FINALIZADO) {
            return servicio(servicio.getSolicitud(), servicio);
        }
        if (servicio.getEstado() != EN_TRASLADO) {
            throw ApiException.conflicto("ESTADO_INVALIDO", "Primero pasa el servicio a EN_TRASLADO.");
        }
        String codigo = cuerpo == null || cuerpo.codigoQr() == null ? "" : cuerpo.codigoQr().trim();
        if (!MessageDigest.isEqual(codigo.getBytes(StandardCharsets.UTF_8),
                servicio.getCodigoQr().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR_INVALIDO", "El código QR no corresponde a este servicio.");
        }
        servicio.setEstado(FINALIZADO);
        servicio.setHoraFin(ahora());
        servicio.setQrValidado(true);
        servicio.getSolicitud().setEstado(EstadoSolicitudTaxi.FINALIZADA);
        taxista.setCantidadServicios((int) servicios.countByTaxistaAndEstado(taxista, FINALIZADO));
        return servicio(servicio.getSolicitud(), servicio);
    }

    @Transactional
    public void ubicacion(Llamante llamante, long reservaId, UbicacionNueva cuerpo) {
        if (cuerpo == null || cuerpo.lat() == null || cuerpo.lng() == null
                || cuerpo.lat() < -90 || cuerpo.lat() > 90 || cuerpo.lng() < -180 || cuerpo.lng() > 180
                || (cuerpo.precisionM() != null && cuerpo.precisionM() < 0)) {
            throw ApiException.validacion("Ubicación inválida.");
        }
        Taxista taxista = taxistaHabilitado(llamante);
        ServicioTaxi servicio = servicioDe(taxista, reservaId);
        if (!servicio.getEstado().activo()) {
            throw ApiException.conflicto("ESTADO_INVALIDO", "El servicio ya no está en curso.");
        }
        UbicacionTaxista punto = new UbicacionTaxista();
        punto.setServicio(servicio);
        punto.setTaxista(taxista);
        punto.setLatitud(cuerpo.lat());
        punto.setLongitud(cuerpo.lng());
        punto.setPrecisionM(cuerpo.precisionM());
        ubicaciones.save(punto);
    }

    // ------------------------------------------------------------------ cliente

    public record Creacion(Servicio servicio, boolean nuevo) {
    }

    /** Hotel y origen salen de Firestore; la app solo elige aeropuerto, hora, pasajeros y maletas. */
    @Transactional
    public Creacion crear(Llamante llamante, SolicitudNueva cuerpo) {
        if (!llamante.es(CLIENTE)) {
            throw ApiException.prohibido("Solo un cliente puede pedir taxi.");
        }
        if (cuerpo == null || cuerpo.reservaId() == null || cuerpo.reservaId() < 1) {
            throw ApiException.validacion("Falta la reserva.");
        }
        CodigoAeropuerto aeropuerto = aeropuerto(cuerpo.aeropuertoCodigo());
        if (cuerpo.hora() == null || !HORA.matcher(cuerpo.hora()).matches()) {
            throw ApiException.validacion("La hora debe tener el formato HH:mm.");
        }
        if (cuerpo.pasajeros() == null || cuerpo.pasajeros() < 1 || cuerpo.maletas() == null || cuerpo.maletas() < 0) {
            throw ApiException.validacion("Revisa la cantidad de pasajeros y maletas.");
        }

        Optional<SolicitudTaxi> existente = solicitudes.findByReservaId(cuerpo.reservaId());
        if (existente.isPresent()) {
            if (!llamante.uid().equals(existente.get().getClienteUid())) {
                throw ApiException.prohibido("Esa reserva no es tuya.");
            }
            SolicitudTaxi solicitud = existente.get();
            return new Creacion(servicio(solicitud, servicios.findBySolicitud(solicitud).orElse(null)), false);
        }

        Map<String, Object> reserva = firestore.documento("reservations/" + cuerpo.reservaId(), llamante.token())
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa reserva."));
        if (!llamante.uid().equals(reserva.get("clientUid"))) {
            throw ApiException.prohibido("Esa reserva no es tuya.");
        }
        Long hotelId = numero(reserva.get("hotelId"));
        if (hotelId == null) {
            throw ApiException.noEncontrado("La reserva no tiene hotel.");
        }
        Map<String, Object> hotel = firestore.documento("hotels/" + hotelId, llamante.token())
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos el hotel de la reserva."));
        if (!Boolean.TRUE.equals(hotel.get("active"))) {
            throw ApiException.validacion("El hotel de la reserva no está activo.");
        }
        String ciudad = texto(hotel.get("city"));
        if (!aeropuerto.atiende(ciudad)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AEROPUERTO_NO_CORRESPONDE",
                    "Ese aeropuerto no corresponde a la ciudad del hotel.");
        }

        SolicitudTaxi solicitud = new SolicitudTaxi();
        solicitud.setReservaId(cuerpo.reservaId());
        solicitud.setClienteUid(llamante.uid());
        solicitud.setClienteNombre(primero(llamante.nombre(), texto(reserva.get("clientName")), "Cliente"));
        solicitud.setHotelId(hotelId);
        solicitud.setHotelNombre(primero(texto(hotel.get("name")), "Hotel"));
        solicitud.setHotelCiudad(ciudad.trim());
        solicitud.setOrigen(primero(texto(hotel.get("address")), texto(hotel.get("name"))));
        solicitud.setAeropuertoCodigo(aeropuerto);
        solicitud.setDestino(aeropuerto.getNombre());
        solicitud.setHoraRecojo(cuerpo.hora());
        solicitud.setCantidadPasajeros(cuerpo.pasajeros());
        solicitud.setCantidadEquipaje(cuerpo.maletas());
        solicitud.setEstado(EstadoSolicitudTaxi.PENDIENTE);
        solicitudes.saveAndFlush(solicitud);
        return new Creacion(servicio(solicitud, null), true);
    }

    @Transactional
    public Servicio ver(Llamante llamante, long reservaId) {
        SolicitudTaxi solicitud = solicitud(reservaId);
        ServicioTaxi servicio = servicios.findBySolicitud(solicitud).orElse(null);
        if (!puedeVer(llamante, solicitud, servicio)) {
            throw ApiException.prohibido("No puedes ver este servicio.");
        }
        return servicio(solicitud, servicio);
    }

    @Transactional
    public QrServicio qr(Llamante llamante, long reservaId) {
        SolicitudTaxi solicitud = solicitud(reservaId);
        boolean dueno = llamante.es(CLIENTE) && llamante.uid().equals(solicitud.getClienteUid());
        if (!dueno && !esAdminDelHotel(llamante, solicitud.getHotelId())) {
            throw ApiException.prohibido("Solo el cliente o el hotel de la reserva ven el QR.");
        }
        ServicioTaxi servicio = servicios.findBySolicitud(solicitud)
                .filter(s -> s.getEstado().activo())
                .orElseThrow(() -> ApiException.conflicto("ESTADO_INVALIDO",
                        "El QR está disponible mientras el servicio tiene un taxista en camino."));
        return new QrServicio(servicio.getCodigoQr());
    }

    @Transactional
    public Servicio calificar(Llamante llamante, long reservaId, CalificacionNueva cuerpo) {
        if (cuerpo == null || cuerpo.puntuacion() == null || cuerpo.puntuacion() < 1 || cuerpo.puntuacion() > 5) {
            throw ApiException.validacion("La calificación va de 1 a 5 estrellas.");
        }
        String comentario = cuerpo.comentario() == null || cuerpo.comentario().isBlank() ? null : cuerpo.comentario().trim();
        if (comentario != null && comentario.length() > 500) {
            throw ApiException.validacion("El comentario admite hasta 500 caracteres.");
        }
        SolicitudTaxi solicitud = solicitud(reservaId);
        if (!llamante.es(CLIENTE) || !llamante.uid().equals(solicitud.getClienteUid())) {
            throw ApiException.prohibido("Solo el cliente de la reserva puede calificar.");
        }
        ServicioTaxi servicio = servicios.findBySolicitud(solicitud)
                .filter(s -> s.getEstado() == FINALIZADO)
                .orElseThrow(() -> ApiException.conflicto("ESTADO_INVALIDO", "Podrás calificar cuando termine el servicio."));
        if (calificaciones.findByServicio(servicio).isPresent()) {
            throw ApiException.conflicto("YA_CALIFICADO", "Ya calificaste este servicio.");
        }

        CalificacionTaxista calificacion = new CalificacionTaxista();
        calificacion.setServicio(servicio);
        calificacion.setTaxista(servicio.getTaxista());
        calificacion.setClienteUid(llamante.uid());
        calificacion.setPuntuacion(cuerpo.puntuacion());
        calificacion.setComentario(comentario);
        calificaciones.saveAndFlush(calificacion);

        Taxista taxista = servicio.getTaxista();
        Double promedio = calificaciones.promedio(taxista);
        taxista.setCalificacion(BigDecimal.valueOf(promedio == null ? 0 : promedio).setScale(2, RoundingMode.HALF_UP));
        return servicio(solicitud, servicio);
    }

    // ------------------------------------------------------------------ superadmin (app)

    @Transactional
    public TaxistaAdminLista taxistas(Llamante llamante, String estadoSolicitud) {
        soloSuperadmin(llamante);
        List<Taxista> lista;
        if (estadoSolicitud == null || estadoSolicitud.isBlank()) {
            lista = taxistas.findAllByOrderByFechaSolicitudDesc();
        } else {
            EstadoSolicitudTaxista estado;
            try {
                estado = EstadoSolicitudTaxista.valueOf(estadoSolicitud);
            } catch (IllegalArgumentException error) {
                throw ApiException.validacion("estadoSolicitud debe ser PENDIENTE, APROBADO o RECHAZADO.");
            }
            lista = taxistas.findByEstadoSolicitudOrderByFechaSolicitudDesc(estado);
        }
        return new TaxistaAdminLista(lista.stream().map(this::admin).toList());
    }

    /** Desactivar le quita el acceso: la API responde TAXISTA_NO_HABILITADO desde ese momento. */
    @Transactional
    public TaxistaAdmin cambiarActivo(Llamante llamante, long taxistaId, CambioActivo cuerpo) {
        soloSuperadmin(llamante);
        if (cuerpo == null || cuerpo.activo() == null) {
            throw ApiException.validacion("Indica si el taxista queda activo.");
        }
        Taxista taxista = taxistas.findById(taxistaId)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese taxista."));
        Usuario usuario = taxista.getUsuario();
        usuario.setActivo(cuerpo.activo());
        if (!cuerpo.activo()) {
            taxista.setDisponible(false);
            cuentasFirebase.bloquear(usuario.getFirebaseUid());
        } else if (taxista.getEstadoSolicitud() == EstadoSolicitudTaxista.APROBADO) {
            cuentasFirebase.prepararCuentaTaxista(com.iotelito.taxiweb.service.TaxistaService.datos(usuario))
                    .ifPresent(usuario::setFirebaseUid);
        }
        String motivo = cuerpo.motivo() == null || cuerpo.motivo().isBlank() ? "" : ": " + cuerpo.motivo().trim();
        logs.save(new LogSistema(null, cuerpo.activo() ? "ACTIVAR_TAXISTA" : "DESACTIVAR_TAXISTA",
                descripcion(taxista) + " por " + llamante.correo() + " desde la app" + motivo));
        return admin(taxista);
    }

    // ------------------------------------------------------------------ reglas internas

    /** El taxista de esta cuenta; la primera vez se enlaza por correo si ya fue aprobado. */
    private Taxista taxistaHabilitado(Llamante llamante) {
        if (!llamante.es(TAXISTA)) {
            throw ApiException.prohibido("Solo un taxista puede hacer esto.");
        }
        Taxista taxista = taxistas.findByUsuarioFirebaseUid(llamante.uid()).orElseGet(() -> enlazarPorCorreo(llamante));
        if (taxista.getEstadoSolicitud() != EstadoSolicitudTaxista.APROBADO
                || !Boolean.TRUE.equals(taxista.getUsuario().getActivo())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TAXISTA_NO_HABILITADO",
                    "Tu solicitud de taxista no está aprobada o tu cuenta está inactiva.");
        }
        return taxista;
    }

    private Taxista enlazarPorCorreo(Llamante llamante) {
        Taxista taxista = llamante.correo() == null ? null : taxistas.findByUsuarioCorreo(llamante.correo()).orElse(null);
        if (taxista == null || taxista.getUsuario().getFirebaseUid() != null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TAXISTA_NO_REGISTRADO",
                    "Tu cuenta no está registrada como taxista. Regístrate en la web de taxistas.");
        }
        if (taxista.getEstadoSolicitud() == EstadoSolicitudTaxista.APROBADO
                && Boolean.TRUE.equals(taxista.getUsuario().getActivo())) {
            Usuario usuario = taxista.getUsuario();
            usuario.setFirebaseUid(llamante.uid());
            logs.save(new LogSistema(usuario, "ENLAZAR_CUENTA_APP", descripcion(taxista)));
        }
        return taxista;
    }

    private ServicioTaxi servicioDe(Taxista taxista, long reservaId) {
        ServicioTaxi servicio = servicios.findBySolicitud(solicitud(reservaId))
                .orElseThrow(() -> ApiException.conflicto("ESTADO_INVALIDO", "Este pedido todavía no tiene taxista."));
        if (!servicio.getTaxista().getId().equals(taxista.getId())) {
            throw ApiException.prohibido("Este servicio es de otro taxista.");
        }
        return servicio;
    }

    private boolean puedeVer(Llamante llamante, SolicitudTaxi solicitud, ServicioTaxi servicio) {
        return switch (llamante.rol()) {
            case SUPERADMIN -> true;
            case CLIENTE -> llamante.uid().equals(solicitud.getClienteUid());
            case ADMIN_HOTEL -> esAdminDelHotel(llamante, solicitud.getHotelId());
            case TAXISTA -> {
                Taxista taxista = taxistaHabilitado(llamante);
                yield servicio == null
                        ? solicitud.getEstado() == EstadoSolicitudTaxi.PENDIENTE
                        : servicio.getTaxista().getId().equals(taxista.getId());
            }
            default -> false;
        };
    }

    /** Igual que hotelAdmin() en las reglas: users.hotelId, hotels.administratorUid y hotel activo. */
    private boolean esAdminDelHotel(Llamante llamante, Long hotelId) {
        if (!llamante.es(ADMIN_HOTEL) || llamante.hotelId() == null || !llamante.hotelId().equals(hotelId)) {
            return false;
        }
        Map<String, Object> hotel = firestore.documento("hotels/" + hotelId, llamante.token()).orElse(null);
        return hotel != null && Boolean.TRUE.equals(hotel.get("active"))
                && llamante.uid().equals(hotel.get("administratorUid"));
    }

    private static void soloSuperadmin(Llamante llamante) {
        if (!llamante.es(SUPERADMIN)) {
            throw ApiException.prohibido("Solo el superadmin puede hacer esto.");
        }
    }

    private SolicitudTaxi solicitud(long reservaId) {
        return solicitudes.findByReservaId(reservaId)
                .orElseThrow(() -> ApiException.noEncontrado("Esa reserva no tiene servicio de taxi."));
    }

    private Optional<ServicioTaxi> activo(Taxista taxista) {
        return servicios.findFirstByTaxistaAndEstadoIn(taxista, ACTIVOS);
    }

    private static CodigoAeropuerto aeropuerto(String codigo) {
        try {
            return CodigoAeropuerto.valueOf(String.valueOf(codigo));
        } catch (IllegalArgumentException error) {
            throw ApiException.validacion("Aeropuerto desconocido.");
        }
    }

    private String codigoQr() {
        byte[] bytes = new byte[10];
        azar.nextBytes(bytes);
        return "TX1-" + HexFormat.of().formatHex(bytes);
    }

    // ------------------------------------------------------------------ JSON

    private Servicio servicio(SolicitudTaxi solicitud, ServicioTaxi servicio) {
        CodigoAeropuerto aeropuerto = solicitud.getAeropuertoCodigo();
        UbicacionTaxista punto = servicio == null ? null
                : ubicaciones.findFirstByServicioOrderByRegistradaEnDesc(servicio).orElse(null);
        CalificacionTaxista calificacion = servicio == null ? null : calificaciones.findByServicio(servicio).orElse(null);
        return new Servicio(
                solicitud.getReservaId(),
                servicio == null ? "SOLICITADO" : servicio.getEstado().name(),
                new ApiDtos.Hotel(solicitud.getHotelId(), solicitud.getHotelNombre(), solicitud.getOrigen(), solicitud.getHotelCiudad()),
                new ApiDtos.AeropuertoDestino(aeropuerto.name(), aeropuerto.getNombre(), aeropuerto.getCiudad()),
                solicitud.getHoraRecojo(),
                solicitud.getCantidadPasajeros(),
                solicitud.getCantidadEquipaje(),
                solicitud.getClienteNombre(),
                servicio == null ? null : publico(servicio.getTaxista()),
                punto == null ? null : new ApiDtos.Ubicacion(punto.getLatitud(), punto.getLongitud(),
                        punto.getPrecisionM(), iso(punto.getRegistradaEn())),
                new ApiDtos.Fechas(
                        iso(solicitud.getFechaSolicitud()),
                        servicio == null ? null : iso(servicio.getHoraAceptacion()),
                        servicio == null ? null : iso(servicio.getHoraInicio()),
                        servicio == null ? null : iso(servicio.getHoraTraslado()),
                        servicio == null ? null : iso(servicio.getHoraFin())),
                calificacion == null ? null : new ApiDtos.Calificacion(calificacion.getPuntuacion(),
                        calificacion.getComentario(), iso(calificacion.getFecha())));
    }

    private static ApiDtos.Vehiculo vehiculo(Taxista taxista) {
        return new ApiDtos.Vehiculo(taxista.getPlacaVehiculo(), taxista.getMarcaVehiculo(), taxista.getModeloVehiculo(),
                taxista.getColorVehiculo(), taxista.getAnioVehiculo(), null);
    }

    private static TaxistaPublico publico(Taxista taxista) {
        Usuario usuario = taxista.getUsuario();
        return new TaxistaPublico(taxista.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getTelefono(),
                null, vehiculo(taxista), taxista.getCalificacion().doubleValue(), taxista.getCantidadServicios());
    }

    private TaxistaPerfil perfil(Taxista taxista) {
        Usuario usuario = taxista.getUsuario();
        return new TaxistaPerfil(taxista.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getTelefono(),
                null, vehiculo(taxista), taxista.getCalificacion().doubleValue(), taxista.getCantidadServicios(),
                usuario.getCorreo(), taxista.getEstadoSolicitud().name(), Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(taxista.getDisponible()),
                activo(taxista).map(s -> s.getSolicitud().getReservaId()).orElse(null));
    }

    private TaxistaAdmin admin(Taxista taxista) {
        TaxistaPerfil p = perfil(taxista);
        return new TaxistaAdmin(p.id(), p.nombres(), p.apellidos(), p.telefono(), p.fotoUrl(), p.vehiculo(),
                p.calificacion(), p.cantidadServicios(), p.correo(), p.estadoSolicitud(), p.activo(), p.disponible(),
                p.servicioActivoId(), taxista.getTipoDocumento().name(), taxista.getNumeroDocumento(),
                iso(taxista.getFechaSolicitud()));
    }

    static String iso(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(LIMA).toInstant().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(LIMA);
    }

    private static String descripcion(Taxista taxista) {
        return taxista.getUsuario().getNombres() + " " + taxista.getUsuario().getApellidos()
                + " (" + taxista.getPlacaVehiculo() + ")";
    }

    private static String texto(Object valor) {
        return valor instanceof String s ? s : null;
    }

    private static Long numero(Object valor) {
        if (valor instanceof Long l) return l;
        if (valor instanceof Integer i) return i.longValue();
        if (valor instanceof Double d && d == Math.rint(d)) return d.longValue();
        return null;
    }

    private static String primero(String... opciones) {
        for (String opcion : opciones) {
            if (opcion != null && !opcion.isBlank()) {
                return opcion.trim();
            }
        }
        return "";
    }
}
