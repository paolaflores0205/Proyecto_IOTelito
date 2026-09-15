package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.*;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.iotelito.taxiweb.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TaxistaService {

    private final TaxistaRepository taxistaRepository;
    private final UsuarioRepository usuarioRepository;

    public TaxistaService(
            TaxistaRepository taxistaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.taxistaRepository = taxistaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Taxista registrarTaxista(TaxistaRegistroForm form) {

        if (usuarioRepository.existsByCorreo(form.getCorreo())) {
            throw new RuntimeException(
                    "Ya existe un usuario registrado con ese correo"
            );
        }

        if (taxistaRepository.existsByDni(form.getDni())) {
            throw new RuntimeException(
                    "Ya existe una solicitud con ese DNI"
            );
        }

        if (taxistaRepository.existsByNumeroLicencia(
                form.getNumeroLicencia()
        )) {
            throw new RuntimeException(
                    "Ya existe una solicitud con esa licencia"
            );
        }

        if (taxistaRepository.existsByPlacaVehiculo(
                form.getPlacaVehiculo()
        )) {
            throw new RuntimeException(
                    "Ya existe un vehículo registrado con esa placa"
            );
        }

        // =====================
        // CREAR USUARIO
        // =====================

        Usuario usuario = new Usuario();

        usuario.setNombres(form.getNombres());
        usuario.setApellidos(form.getApellidos());
        usuario.setCorreo(form.getCorreo());
        usuario.setTelefono(form.getTelefono());

        usuario.setRol(Rol.TAXISTA);

        // Todavía pendiente de aprobación
        usuario.setActivo(false);

        /*
         * Tu Figma no pide contraseña en el registro.
         * Por ahora generamos una clave interna.
         * Después, al aprobarlo, se puede enviar un flujo
         * para crear contraseña.
         */
        usuario.setPassword(UUID.randomUUID().toString());
        usuario.marcarCreacion();

        usuario = usuarioRepository.save(usuario);


        // =====================
        // CREAR TAXISTA
        // =====================

        Taxista taxista = new Taxista();

        taxista.setUsuario(usuario);

        taxista.setDni(form.getDni());

        taxista.setFechaNacimiento(
                form.getFechaNacimiento()
        );

        taxista.setDomicilio(
                form.getDomicilio()
        );

        taxista.setNumeroLicencia(
                form.getNumeroLicencia()
        );

        taxista.setCategoriaLicencia(
                form.getCategoriaLicencia()
        );

        taxista.setFechaVencimientoLicencia(
                form.getFechaVencimientoLicencia()
        );

        taxista.setMarcaVehiculo(
                form.getMarcaVehiculo()
        );

        taxista.setModeloVehiculo(
                form.getModeloVehiculo()
        );

        taxista.setAnioVehiculo(
                form.getAnioVehiculo()
        );

        taxista.setColorVehiculo(
                form.getColorVehiculo()
        );

        taxista.setPlacaVehiculo(
                form.getPlacaVehiculo()
        );

        taxista.setAceptaTerminos(
                Boolean.TRUE.equals(form.getAceptaTerminos())
        );

        taxista.setEstadoSolicitud(
                EstadoSolicitudTaxista.PENDIENTE
        );

        taxista.setDisponible(false);
        taxista.marcarSolicitud();

        return taxistaRepository.save(taxista);
    }

    public List<Taxista> listarTaxistas(String estado, String busqueda) {
        List<Taxista> taxistas;

        if (busqueda != null && !busqueda.isBlank()) {
            String termino = busqueda.trim();
            taxistas = taxistaRepository
                    .findByDniContainingIgnoreCaseOrPlacaVehiculoContainingIgnoreCaseOrUsuarioNombresContainingIgnoreCaseOrUsuarioApellidosContainingIgnoreCaseOrderByFechaSolicitudDesc(
                            termino,
                            termino,
                            termino,
                            termino
                    );
        } else if (estado != null && !estado.isBlank()) {
            taxistas = taxistaRepository.findByEstadoSolicitudOrderByFechaSolicitudDesc(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase())
            );
        } else {
            taxistas = taxistaRepository.findAllByOrderByFechaSolicitudDesc();
        }

        if (estado == null || estado.isBlank() || busqueda == null || busqueda.isBlank()) {
            return taxistas;
        }

        EstadoSolicitudTaxista estadoSolicitud =
                EstadoSolicitudTaxista.valueOf(estado.toUpperCase());

        return taxistas.stream()
                .filter(taxista -> taxista.getEstadoSolicitud() == estadoSolicitud)
                .toList();
    }

    public Page<Taxista> listarTaxistasPaginado(
            String estado,
            String busqueda,
            Pageable pageable
    ) {
        Page<Taxista> taxistas;

        if (busqueda != null && !busqueda.isBlank()
                && estado != null && !estado.isBlank()) {
            String termino = busqueda.trim();
            taxistas = taxistaRepository.buscarPorEstadoYTermino(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase()),
                    termino,
                    pageable
            );
        } else if (busqueda != null && !busqueda.isBlank()) {
            String termino = busqueda.trim();
            taxistas = taxistaRepository
                    .findByDniContainingIgnoreCaseOrPlacaVehiculoContainingIgnoreCaseOrUsuarioNombresContainingIgnoreCaseOrUsuarioApellidosContainingIgnoreCase(
                            termino,
                            termino,
                            termino,
                            termino,
                            pageable
                    );
        } else if (estado != null && !estado.isBlank()) {
            taxistas = taxistaRepository.findByEstadoSolicitud(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase()),
                    pageable
            );
        } else {
            taxistas = taxistaRepository.findAll(pageable);
        }

        return taxistas;
    }

    public List<Taxista> listarPorDisponibilidad(Boolean disponible) {
        if (disponible == null) {
            return taxistaRepository.findAllByOrderByFechaSolicitudDesc();
        }

        return taxistaRepository.findByDisponibleOrderByFechaSolicitudDesc(disponible);
    }

    public Page<Taxista> listarPorDisponibilidadPaginado(
            Boolean disponible,
            Pageable pageable
    ) {
        if (disponible == null) {
            return taxistaRepository.findAll(pageable);
        }

        return taxistaRepository.findByDisponible(disponible, pageable);
    }

    public List<Taxista> listarPendientesRecientes() {
        return taxistaRepository
                .findByEstadoSolicitudOrderByFechaSolicitudDesc(
                        EstadoSolicitudTaxista.PENDIENTE
                )
                .stream()
                .limit(5)
                .toList();
    }

    public List<Taxista> listarSolicitudesPendientes() {
        return taxistaRepository.findByEstadoSolicitudOrderByFechaSolicitudDesc(
                EstadoSolicitudTaxista.PENDIENTE
        );
    }

    public Page<Taxista> listarSolicitudesPendientesPaginado(Pageable pageable) {
        return taxistaRepository.findByEstadoSolicitud(
                EstadoSolicitudTaxista.PENDIENTE,
                pageable
        );
    }

    public Taxista obtenerTaxista(Long id) {
        return taxistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontro el taxista solicitado"));
    }

    public Taxista aprobarTaxista(Long id) {
        Taxista taxista = obtenerTaxista(id);

        taxista.setEstadoSolicitud(EstadoSolicitudTaxista.APROBADO);
        taxista.setDisponible(true);
        taxista.getUsuario().setActivo(true);
        taxista.getUsuario().marcarActualizacion();
        usuarioRepository.save(taxista.getUsuario());

        return taxistaRepository.save(taxista);
    }

    public Taxista rechazarTaxista(Long id) {
        Taxista taxista = obtenerTaxista(id);

        taxista.setEstadoSolicitud(EstadoSolicitudTaxista.RECHAZADO);
        taxista.setDisponible(false);
        taxista.getUsuario().setActivo(false);
        taxista.getUsuario().marcarActualizacion();
        usuarioRepository.save(taxista.getUsuario());

        return taxistaRepository.save(taxista);
    }

    public Taxista cambiarDisponibilidad(Long id, boolean disponible) {
        Taxista taxista = obtenerTaxista(id);

        taxista.setDisponible(disponible);

        return taxistaRepository.save(taxista);
    }

    public long contarTodos() {
        return taxistaRepository.count();
    }

    public long contarPendientes() {
        return taxistaRepository.countByEstadoSolicitud(EstadoSolicitudTaxista.PENDIENTE);
    }

    public long contarAprobados() {
        return taxistaRepository.countByEstadoSolicitud(EstadoSolicitudTaxista.APROBADO);
    }

    public long contarRechazados() {
        return taxistaRepository.countByEstadoSolicitud(EstadoSolicitudTaxista.RECHAZADO);
    }

    public long contarActivos() {
        return taxistaRepository.countByUsuarioActivoTrue();
    }

    public long contarInactivos() {
        return taxistaRepository.countByUsuarioActivoFalse();
    }

    public double promedioCalificacion() {
        return taxistaRepository.findAll()
                .stream()
                .map(Taxista::getCalificacion)
                .filter(calificacion -> calificacion != null)
                .mapToDouble(calificacion -> calificacion.doubleValue())
                .average()
                .orElse(0);
    }

    public int totalServicios() {
        return taxistaRepository.findAll()
                .stream()
                .map(Taxista::getCantidadServicios)
                .filter(cantidad -> cantidad != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public List<Taxista> mejoresTaxistas() {
        return taxistaRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Taxista::getCalificacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
    }
}
