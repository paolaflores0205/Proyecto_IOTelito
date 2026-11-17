package com.iotelito.taxiweb.api;

import com.iotelito.taxiweb.api.ApiDtos.AeropuertoLista;
import com.iotelito.taxiweb.api.ApiDtos.CalificacionNueva;
import com.iotelito.taxiweb.api.ApiDtos.CambioActivo;
import com.iotelito.taxiweb.api.ApiDtos.CambioEstado;
import com.iotelito.taxiweb.api.ApiDtos.Disponibilidad;
import com.iotelito.taxiweb.api.ApiDtos.Finalizacion;
import com.iotelito.taxiweb.api.ApiDtos.QrServicio;
import com.iotelito.taxiweb.api.ApiDtos.Salud;
import com.iotelito.taxiweb.api.ApiDtos.Servicio;
import com.iotelito.taxiweb.api.ApiDtos.ServicioLista;
import com.iotelito.taxiweb.api.ApiDtos.SolicitudNueva;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaAdmin;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaAdminLista;
import com.iotelito.taxiweb.api.ApiDtos.TaxistaPerfil;
import com.iotelito.taxiweb.api.ApiDtos.UbicacionNueva;
import com.iotelito.taxiweb.model.CodigoAeropuerto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/** API para la app móvil: una ruta por operación de docs/api-taxis-v1.yaml. */
@RestController
@RequestMapping("/api/v1")
public class TaxiApiController {

    private final TaxiApiService api;

    public TaxiApiController(TaxiApiService api) {
        this.api = api;
    }

    @GetMapping("/health")
    public Salud health() {
        return new Salud(true);
    }

    @GetMapping("/aeropuertos")
    public AeropuertoLista aeropuertos() {
        return new AeropuertoLista(Arrays.stream(CodigoAeropuerto.values())
                .map(a -> new ApiDtos.Aeropuerto(a.name(), a.getNombre(), a.getCiudad(), a.getCiudadesHotel()))
                .toList());
    }

    // Taxista

    @GetMapping("/taxistas/me")
    public TaxistaPerfil miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return api.miPerfil(api.llamante(jwt));
    }

    @PutMapping("/taxistas/me/disponibilidad")
    public TaxistaPerfil disponibilidad(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) Disponibilidad cuerpo) {
        return api.cambiarDisponibilidad(api.llamante(jwt), cuerpo);
    }

    @GetMapping("/taxistas/me/servicio-activo")
    public ResponseEntity<Servicio> servicioActivo(@AuthenticationPrincipal Jwt jwt) {
        return api.miServicioActivo(api.llamante(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/taxistas/me/servicios")
    public ServicioLista historial(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "50") int limite) {
        return api.miHistorial(api.llamante(jwt), limite);
    }

    @GetMapping("/servicios/disponibles")
    public ServicioLista disponibles(@AuthenticationPrincipal Jwt jwt) {
        return api.disponibles(api.llamante(jwt));
    }

    @PostMapping("/servicios/{id}/aceptar")
    public Servicio aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return api.aceptar(api.llamante(jwt), id);
    }

    @PostMapping("/servicios/{id}/estado")
    public Servicio estado(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                           @RequestBody(required = false) CambioEstado cuerpo) {
        return api.avanzar(api.llamante(jwt), id, cuerpo);
    }

    @PostMapping("/servicios/{id}/finalizar")
    public Servicio finalizar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                              @RequestBody(required = false) Finalizacion cuerpo) {
        return api.finalizar(api.llamante(jwt), id, cuerpo);
    }

    @PutMapping("/servicios/{id}/ubicacion")
    public ResponseEntity<Void> ubicacion(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                          @RequestBody(required = false) UbicacionNueva cuerpo) {
        api.ubicacion(api.llamante(jwt), id, cuerpo);
        return ResponseEntity.noContent().build();
    }

    // Cliente y compartidos

    @PostMapping("/servicios")
    public ResponseEntity<Servicio> crear(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) SolicitudNueva cuerpo) {
        TaxiApiService.Creacion creacion = api.crear(api.llamante(jwt), cuerpo);
        return ResponseEntity.status(creacion.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(creacion.servicio());
    }

    @GetMapping("/servicios/{id}")
    public Servicio ver(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return api.ver(api.llamante(jwt), id);
    }

    @GetMapping("/servicios/{id}/mensajes")
    public ChatTaxi.Conversacion mensajes(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return api.mensajes(api.llamante(jwt), id);
    }

    @PostMapping("/servicios/{id}/mensajes")
    public ChatTaxi.Conversacion enviarMensaje(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                              @RequestBody(required = false) ChatTaxi.NuevoMensaje cuerpo) {
        return api.enviarMensaje(api.llamante(jwt), id, cuerpo);
    }

    @PutMapping("/notificaciones/dispositivo")
    public ResponseEntity<Void> registrarDispositivo(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody(required = false) AvisosTaxi.Dispositivo cuerpo) {
        api.registrarDispositivo(api.llamante(jwt), cuerpo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/servicios/{id}/qr")
    public QrServicio qr(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return api.qr(api.llamante(jwt), id);
    }

    @PostMapping("/servicios/{id}/calificacion")
    public ResponseEntity<Servicio> calificar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                              @RequestBody(required = false) CalificacionNueva cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(api.calificar(api.llamante(jwt), id, cuerpo));
    }

    // Superadmin desde la app

    @GetMapping("/taxistas")
    public TaxistaAdminLista taxistas(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(required = false) String estadoSolicitud) {
        return api.taxistas(api.llamante(jwt), estadoSolicitud);
    }

    @PatchMapping("/taxistas/{taxistaId}/activo")
    public TaxistaAdmin cambiarActivo(@AuthenticationPrincipal Jwt jwt, @PathVariable long taxistaId,
                                      @RequestBody(required = false) CambioActivo cuerpo) {
        return api.cambiarActivo(api.llamante(jwt), taxistaId, cuerpo);
    }
}
