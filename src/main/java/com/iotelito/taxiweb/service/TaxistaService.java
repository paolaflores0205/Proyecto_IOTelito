package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.firebase.CuentasFirebase;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.LogSistema;
import com.iotelito.taxiweb.model.Rol;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.TipoDocumento;
import com.iotelito.taxiweb.model.Usuario;
import com.iotelito.taxiweb.repository.LogSistemaRepository;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.iotelito.taxiweb.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TaxistaService {

    private final TaxistaRepository taxistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaRepository logRepository;
    private final CuentasFirebase cuentasFirebase;

    public TaxistaService(
            TaxistaRepository taxistaRepository,
            UsuarioRepository usuarioRepository,
            LogSistemaRepository logRepository,
            CuentasFirebase cuentasFirebase
    ) {
        this.taxistaRepository = taxistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logRepository = logRepository;
        this.cuentasFirebase = cuentasFirebase;
    }

    /**
     * Autorregistro desde la web. El taxista queda PENDIENTE y sin acceso a la app hasta que
     * el superadmin lo apruebe. No se le pide ni se le guarda contraseña: al aprobarlo se le
     * crea su cuenta de Firebase para la app.
     */
    @Transactional
    public Taxista registrarTaxista(TaxistaRegistroForm form) {
        TipoDocumento tipo = form.getTipoDocumento();
        String numero = recortar(form.getNumeroDocumento());
        String correo = recortar(form.getCorreo()).toLowerCase(Locale.ROOT);
        String placa = recortar(form.getPlacaVehiculo()).toUpperCase(Locale.ROOT);
        String licencia = recortar(form.getNumeroLicencia()).toUpperCase(Locale.ROOT);

        if (tipo == null) {
            throw new RuntimeException("Elige el tipo de documento de identidad");
        }
        if (!tipo.valida(numero)) {
            throw new RuntimeException(switch (tipo) {
                case DNI -> "El DNI debe tener 8 dígitos";
                case PASAPORTE -> "El pasaporte debe tener de 6 a 12 letras o números";
                case CARNET_EXTRANJERIA -> "El carné de extranjería debe tener de 9 a 12 letras o números";
            });
        }
        if (!Boolean.TRUE.equals(form.getAceptaTerminos())) {
            throw new RuntimeException("Debes aceptar los términos para registrarte");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RuntimeException("Ya existe un usuario registrado con ese correo");
        }
        if (taxistaRepository.existsByTipoDocumentoAndNumeroDocumento(tipo, numero)) {
            throw new RuntimeException("Ya existe una solicitud con ese documento");
        }
        if (taxistaRepository.existsByNumeroLicencia(licencia)) {
            throw new RuntimeException("Ya existe una solicitud con esa licencia");
        }
        if (taxistaRepository.existsByPlacaVehiculo(placa)) {
            throw new RuntimeException("Ya existe un vehículo registrado con esa placa");
        }

        Usuario usuario = new Usuario();
        usuario.setNombres(recortar(form.getNombres()));
        usuario.setApellidos(recortar(form.getApellidos()));
        usuario.setCorreo(correo);
        usuario.setTelefono(recortar(form.getTelefono()));
        usuario.setRol(Rol.TAXISTA);
        usuario.setActivo(false);

        Taxista taxista = new Taxista();
        taxista.setUsuario(usuario);
        taxista.setTipoDocumento(tipo);
        taxista.setNumeroDocumento(numero);
        taxista.setFechaNacimiento(form.getFechaNacimiento());
        taxista.setDomicilio(recortar(form.getDomicilio()));
        taxista.setNumeroLicencia(licencia);
        taxista.setCategoriaLicencia(recortar(form.getCategoriaLicencia()));
        taxista.setFechaVencimientoLicencia(form.getFechaVencimientoLicencia());
        taxista.setMarcaVehiculo(recortar(form.getMarcaVehiculo()));
        taxista.setModeloVehiculo(recortar(form.getModeloVehiculo()));
        taxista.setAnioVehiculo(form.getAnioVehiculo());
        taxista.setColorVehiculo(recortar(form.getColorVehiculo()));
        taxista.setPlacaVehiculo(placa);
        taxista.setAceptaTerminos(true);
        taxista.setEstadoSolicitud(EstadoSolicitudTaxista.PENDIENTE);
        taxista.setDisponible(false);

        return taxistaRepository.save(taxista);
    }

    public List<Taxista> listarTaxistas(String estado, String busqueda) {
        List<Taxista> taxistas;

        if (busqueda != null && !busqueda.isBlank()) {
            taxistas = taxistaRepository.buscarPorTermino(busqueda.trim());
        } else if (estado != null && !estado.isBlank()) {
            taxistas = taxistaRepository.findByEstadoSolicitudOrderByFechaSolicitudDesc(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase(Locale.ROOT))
            );
        } else {
            taxistas = taxistaRepository.findAllByOrderByFechaSolicitudDesc();
        }

        if (estado == null || estado.isBlank() || busqueda == null || busqueda.isBlank()) {
            return taxistas;
        }

        EstadoSolicitudTaxista estadoSolicitud =
                EstadoSolicitudTaxista.valueOf(estado.toUpperCase(Locale.ROOT));

        return taxistas.stream()
                .filter(taxista -> taxista.getEstadoSolicitud() == estadoSolicitud)
                .toList();
    }

    public Page<Taxista> listarTaxistasPaginado(
            String estado,
            String busqueda,
            Pageable pageable
    ) {
        boolean conBusqueda = busqueda != null && !busqueda.isBlank();
        boolean conEstado = estado != null && !estado.isBlank();

        if (conBusqueda && conEstado) {
            return taxistaRepository.buscarPorEstadoYTermino(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase(Locale.ROOT)),
                    busqueda.trim(),
                    pageable
            );
        }
        if (conBusqueda) {
            return taxistaRepository.buscarPorTermino(busqueda.trim(), pageable);
        }
        if (conEstado) {
            return taxistaRepository.findByEstadoSolicitud(
                    EstadoSolicitudTaxista.valueOf(estado.toUpperCase(Locale.ROOT)),
                    pageable
            );
        }
        return taxistaRepository.findAll(pageable);
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
                .findByEstadoSolicitudOrderByFechaSolicitudDesc(EstadoSolicitudTaxista.PENDIENTE)
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
        return taxistaRepository.findByEstadoSolicitud(EstadoSolicitudTaxista.PENDIENTE, pageable);
    }

    public Taxista obtenerTaxista(Long id) {
        return taxistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontro el taxista solicitado"));
    }

    /**
     * Aprueba y deja lista su cuenta de la app: si la automatización está configurada, Firebase
     * crea la cuenta y le envía al taxista un correo para elegir su contraseña. Si Firebase falla,
     * no se aprueba (CuentaFirebaseException con el motivo para el superadmin).
     */
    @Transactional
    public Taxista aprobarTaxista(Long id, String actorCorreo) {
        Taxista taxista = obtenerTaxista(id);
        Usuario usuario = taxista.getUsuario();

        taxista.setEstadoSolicitud(EstadoSolicitudTaxista.APROBADO);
        taxista.setDisponible(true);
        usuario.setActivo(true);
        cuentasFirebase.prepararCuentaTaxista(datos(usuario)).ifPresent(usuario::setFirebaseUid);
        registrar(actorCorreo, "APROBAR_TAXISTA", descripcion(taxista));

        return taxistaRepository.save(taxista);
    }

    @Transactional
    public Taxista rechazarTaxista(Long id, String actorCorreo) {
        Taxista taxista = obtenerTaxista(id);

        taxista.setEstadoSolicitud(EstadoSolicitudTaxista.RECHAZADO);
        taxista.setDisponible(false);
        taxista.getUsuario().setActivo(false);
        cuentasFirebase.bloquear(taxista.getUsuario().getFirebaseUid());
        registrar(actorCorreo, "RECHAZAR_TAXISTA", descripcion(taxista));

        return taxistaRepository.save(taxista);
    }

    public static CuentasFirebase.DatosTaxista datos(Usuario usuario) {
        return new CuentasFirebase.DatosTaxista(usuario.getFirebaseUid(), usuario.getCorreo(),
                usuario.getNombres(), usuario.getApellidos(), usuario.getTelefono());
    }

    @Transactional
    public Taxista cambiarDisponibilidad(Long id, boolean disponible, String actorCorreo) {
        Taxista taxista = obtenerTaxista(id);

        taxista.setDisponible(disponible);
        registrar(actorCorreo, disponible ? "HABILITAR_DISPONIBILIDAD" : "QUITAR_DISPONIBILIDAD", descripcion(taxista));

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

    private void registrar(String actorCorreo, String accion, String detalle) {
        Usuario actor = actorCorreo == null ? null : usuarioRepository.findByCorreo(actorCorreo).orElse(null);
        logRepository.save(new LogSistema(actor, accion, detalle));
    }

    private static String descripcion(Taxista taxista) {
        return taxista.getUsuario().getNombres() + " " + taxista.getUsuario().getApellidos()
                + " (" + taxista.getPlacaVehiculo() + ")";
    }

    private static String recortar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
