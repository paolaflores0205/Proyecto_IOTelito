package com.iotelito.taxiweb.controller;

import com.iotelito.taxiweb.firebase.CuentaFirebaseException;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.Usuario;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import com.iotelito.taxiweb.service.DocumentosTaxistaService;
import com.iotelito.taxiweb.service.TaxistaService;

import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    private final TaxistaService taxistaService;
    private final UsuarioRepository usuarioRepository;
    private final DocumentosTaxistaService documentos;

    public SuperadminController(
            TaxistaService taxistaService,
            UsuarioRepository usuarioRepository,
            DocumentosTaxistaService documentos
    ) {
        this.taxistaService = taxistaService;
        this.usuarioRepository = usuarioRepository;
        this.documentos = documentos;
    }

    /** El superadmin con la sesión iniciada; las vistas muestran su nombre. */
    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(Principal principal) {
        return usuarioRepository.findByCorreo(principal.getName())
                .orElseThrow(() -> new IllegalStateException("La cuenta de la sesión ya no existe"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        cargarResumen(model);
        model.addAttribute("solicitudesRecientes", taxistaService.listarPendientesRecientes());

        return "web-dashboard-taxistas";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        cargarResumen(model);

        return "web-perfil-superadmin";
    }

    @GetMapping("/notificaciones")
    public String notificaciones(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<Taxista> paginaSolicitudes =
                taxistaService.listarSolicitudesPendientesPaginado(
                        PageRequest.of(Math.max(page, 0), 5, Sort.by("fechaSolicitud").descending())
                );

        cargarResumen(model);
        model.addAttribute("solicitudes", paginaSolicitudes.getContent());
        model.addAttribute("pagina", paginaSolicitudes);

        return "web-notificaciones";
    }

    @GetMapping("/taxistas")
    public String listaTaxistas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<Taxista> paginaTaxistas = taxistaService.listarTaxistasPaginado(
                estado,
                q,
                PageRequest.of(Math.max(page, 0), 5, Sort.by("fechaSolicitud").descending())
        );

        cargarResumen(model);
        model.addAttribute("taxistas", paginaTaxistas.getContent());
        model.addAttribute("pagina", paginaTaxistas);
        model.addAttribute("estadoSeleccionado", estado == null ? "" : estado);
        model.addAttribute("busqueda", q == null ? "" : q);

        return "web-lista-taxistas";
    }

    @GetMapping("/taxistas/detalle")
    public String detalleTaxista() {
        return "redirect:/superadmin/taxistas";
    }

    @GetMapping("/taxistas/{id}")
    public String detalleTaxista(
            @PathVariable Long id,
            Model model
    ) {
        cargarResumen(model);
        cargarDetalleTaxista(id, model);

        return "web-detalle-taxista";
    }

    @GetMapping("/taxistas/{id}/documentos/{documentoId}")
    public ResponseEntity<Void> abrirDocumento(@PathVariable Long id, @PathVariable Long documentoId) {
        try {
            return documentos.abrir(id, documentoId)
                    .map(this::redirigirAArchivo)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException error) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private ResponseEntity<Void> redirigirAArchivo(URI url) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(url)
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .build();
    }

    @GetMapping("/solicitudes/evaluar")
    public String evaluarSolicitud(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<Taxista> paginaSolicitudes =
                taxistaService.listarSolicitudesPendientesPaginado(
                        PageRequest.of(Math.max(page, 0), 5, Sort.by("fechaSolicitud").descending())
                );

        cargarResumen(model);
        model.addAttribute("taxista", null);
        model.addAttribute("solicitudes", paginaSolicitudes.getContent());
        model.addAttribute("pagina", paginaSolicitudes);

        return "web-evaluar-solicitud";
    }

    @GetMapping("/taxistas/{id}/evaluar")
    public String evaluarSolicitud(
            @PathVariable Long id,
            Model model
    ) {
        cargarResumen(model);
        cargarDetalleTaxista(id, model);

        return "web-evaluar-solicitud";
    }

    private void cargarDetalleTaxista(Long id, Model model) {
        Taxista taxista = taxistaService.obtenerTaxista(id);
        model.addAttribute("taxista", taxista);
        model.addAttribute("documentosTaxista", documentos.listarAdjuntos(taxista));
    }

    @PostMapping("/taxistas/{id}/aprobar")
    public String aprobarTaxista(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taxistaService.aprobarTaxista(id, principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Taxista aprobado correctamente");
        } catch (CuentaFirebaseException error) {
            redirectAttributes.addFlashAttribute("mensaje", "No se aprobó: " + error.getMessage());
        }

        return "redirect:/superadmin/taxistas/" + id;
    }

    @PostMapping("/taxistas/{id}/rechazar")
    public String rechazarTaxista(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taxistaService.rechazarTaxista(id, principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada correctamente");
        } catch (CuentaFirebaseException error) {
            redirectAttributes.addFlashAttribute("mensaje", "No se rechazó: " + error.getMessage());
        }

        return "redirect:/superadmin/taxistas/" + id;
    }

    @PostMapping("/taxistas/{id}/disponibilidad")
    public String cambiarDisponibilidad(
            @PathVariable Long id,
            @RequestParam boolean disponible,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        taxistaService.cambiarDisponibilidad(id, disponible, principal.getName());
        redirectAttributes.addFlashAttribute("mensaje", "Disponibilidad actualizada");

        return "redirect:/superadmin/taxistas/flota";
    }

    @GetMapping("/taxistas/flota")
    public String taxistasActivosInactivos(
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<Taxista> paginaTaxistas =
                taxistaService.listarPorDisponibilidadPaginado(
                        disponible,
                        PageRequest.of(Math.max(page, 0), 5, Sort.by("fechaSolicitud").descending())
                );

        cargarResumen(model);
        model.addAttribute("taxistas", paginaTaxistas.getContent());
        model.addAttribute("pagina", paginaTaxistas);
        model.addAttribute("disponibleSeleccionado", disponible);

        return "web-taxistas-activos-inactivos";
    }

    @GetMapping("/reportes")
    public String reportes(Model model) {
        cargarResumen(model);
        model.addAttribute("totalServicios", taxistaService.totalServicios());
        model.addAttribute("promedioCalificacion", taxistaService.promedioCalificacion());
        model.addAttribute("mejoresTaxistas", taxistaService.mejoresTaxistas());

        return "web-reportes-taxistas";
    }

    @GetMapping("/reportes/exportar")
    public ResponseEntity<byte[]> exportarReporte() {
        StringBuilder csv = new StringBuilder();
        csv.append("id,nombres,apellidos,tipo_documento,numero_documento,placa,estado,disponible,calificacion,servicios\n");

        for (Taxista taxista : taxistaService.listarTaxistas(null, null)) {
            csv.append(taxista.getId()).append(",");
            csv.append(escaparCsv(taxista.getUsuario().getNombres())).append(",");
            csv.append(escaparCsv(taxista.getUsuario().getApellidos())).append(",");
            csv.append(taxista.getTipoDocumento()).append(",");
            csv.append(escaparCsv(taxista.getNumeroDocumento())).append(",");
            csv.append(escaparCsv(taxista.getPlacaVehiculo())).append(",");
            csv.append(taxista.getEstadoSolicitud()).append(",");
            csv.append(taxista.getDisponible()).append(",");
            csv.append(taxista.getCalificacion()).append(",");
            csv.append(taxista.getCantidadServicios()).append("\n");
        }

        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=taxistas.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }

    private void cargarResumen(Model model) {
        model.addAttribute("totalTaxistas", taxistaService.contarTodos());
        model.addAttribute("pendientes", taxistaService.contarPendientes());
        model.addAttribute("aprobados", taxistaService.contarAprobados());
        model.addAttribute("rechazados", taxistaService.contarRechazados());
        model.addAttribute("activos", taxistaService.contarActivos());
        model.addAttribute("inactivos", taxistaService.contarInactivos());
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }

        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
