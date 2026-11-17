package com.iotelito.taxiweb;

import com.iotelito.taxiweb.api.FirestoreLector;
import com.iotelito.taxiweb.firebase.MensajeriaFirebase;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La API contra el contrato docs/api-taxis-v1.yaml. Firestore se reemplaza por documentos en
 * memoria y el token de Firebase por uno simulado; MySQL es H2 con db/schema.sql y db/data.sql.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:iotelito_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Transactional
@Import(TaxiApiTest.FirestoreEnMemoria.class)
class TaxiApiTest {

    @TestConfiguration
    static class FirestoreEnMemoria {
        static final Map<String, Map<String, Object>> DOCUMENTOS = new ConcurrentHashMap<>();

        @Bean
        @Primary
        FirestoreLector firestoreEnMemoria() {
            return (ruta, token) -> Optional.ofNullable(DOCUMENTOS.get(ruta));
        }
    }

    private static final String API = "/api/v1";
    private static final String CLIENTE = "uid-cliente";
    private static final String MARIA = "demo-uid-maria";
    private static final String ANDREA = "demo-uid-andrea";
    private static final String FERNANDO = "demo-uid-fernando";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TaxistaRepository taxistaRepository;

    @Autowired private JdbcTemplate db;
    @Autowired private ApplicationContext contexto;
    @MockitoBean private MensajeriaFirebase fcm;

    @BeforeEach
    void documentosDeFirestore() {
        Map<String, Map<String, Object>> d = FirestoreEnMemoria.DOCUMENTOS;
        d.clear();
        perfil(CLIENTE, "CLIENT", true, null);
        perfil("uid-otro", "CLIENT", true, null);
        perfil("uid-inactivo", "CLIENT", false, null);
        perfil(MARIA, "TAXISTA", true, null);
        perfil(ANDREA, "TAXISTA", true, null);
        perfil(FERNANDO, "TAXISTA", true, null);
        perfil("uid-carlos", "TAXISTA", true, null);
        perfil("uid-taxista-sin-registro", "TAXISTA", true, null);
        perfil("uid-admin-1", "HOTEL_ADMIN", true, 1L);
        perfil("uid-admin-2", "HOTEL_ADMIN", true, 2L);
        perfil("uid-super", "SUPERADMIN", true, null);
        d.put("hotels/1", Map.of("name", "Hotel Miraflores Park", "address", "Av. Malecón de la Reserva 1035",
                "city", "Lima", "active", true, "administratorUid", "uid-admin-1"));
        d.put("hotels/2", Map.of("name", "Hotel Andino Cusco", "address", "Calle Saphi 123",
                "city", "Cusco", "active", true, "administratorUid", "uid-admin-2"));
        d.put("reservations/777", Map.of("clientUid", CLIENTE, "hotelId", 1L));
        d.put("reservations/779", Map.of("clientUid", "uid-otro", "hotelId", 1L));
    }

    private static void perfil(String uid, String rol, boolean activo, Long hotelId) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("uid", uid);
        datos.put("role", rol);
        datos.put("active", activo);
        datos.put("name", "Nombre " + uid);
        if (hotelId != null) {
            datos.put("hotelId", hotelId);
        }
        FirestoreEnMemoria.DOCUMENTOS.put("users/" + uid, datos);
    }

    private static RequestPostProcessor como(String uid) {
        String correo = switch (uid) {
            case MARIA -> "maria.demo@iotelito.com";
            case "uid-carlos" -> "carlos.demo@iotelito.com";
            default -> uid + "@example.test";
        };
        return jwt().jwt(token -> token.subject(uid).claim("email", correo));
    }

    private ResultActions llamar(MockHttpServletRequestBuilder peticion, String uid, String cuerpo) throws Exception {
        if (uid != null) {
            peticion.with(como(uid));
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(peticion);
    }

    private static String pedido(long reserva, String aeropuerto) {
        return "{\"reservaId\":" + reserva + ",\"aeropuertoCodigo\":\"" + aeropuerto
                + "\",\"hora\":\"14:30\",\"pasajeros\":2,\"maletas\":1}";
    }

    @Test
    void conversacionPrivadaConMensajesIdempotentesYLecturaAlFinalizar() throws Exception {
        String viaje = API + "/servicios/777";
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "LIM")).andExpect(status().isCreated());
        llamar(get(viaje + "/mensajes"), CLIENTE, null).andExpect(status().isConflict());
        llamar(post(viaje + "/aceptar"), MARIA, null).andExpect(status().isOk());
        for (String uid : new String[]{"uid-otro", ANDREA, "uid-admin-1", "uid-super"}) {
            llamar(get(viaje + "/mensajes"), uid, null).andExpect(status().isForbidden());
        }
        llamar(get(viaje + "/mensajes"), null, null).andExpect(status().isUnauthorized());
        String mensaje = "{\"uuid\":\"9fecadc3-74ad-4df5-8e42-66b2285936db\",\"texto\":\" Hola, espero en recepción. \"}";
        for (int i = 0; i < 2; i++) {
            llamar(post(viaje + "/mensajes"), CLIENTE, mensaje).andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensajes", hasSize(1)));
        }
        llamar(get(viaje + "/mensajes"), MARIA, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes[0].texto").value("Hola, espero en recepción."))
                .andExpect(jsonPath("$.mensajes[0].mio").value(false));
        llamar(post(viaje + "/mensajes"), MARIA, mensaje.replace("Hola, espero en recepción.", "Estoy en camino."))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensajes[1].mio").value(true));
        llamar(post(viaje + "/mensajes"), MARIA, "{\"uuid\":\"inválido\",\"texto\":\"Hola\"}")
                .andExpect(status().isBadRequest());
        llamar(post(viaje + "/estado"), MARIA, "{\"estado\":\"EN_CAMINO\"}").andExpect(status().isOk());
        llamar(post(viaje + "/estado"), MARIA, "{\"estado\":\"EN_TRASLADO\"}").andExpect(status().isOk());
        String qr = JsonPath.read(llamar(get(viaje + "/qr"), CLIENTE, null).andReturn().getResponse().getContentAsString(), "$.codigo");
        llamar(post(viaje + "/finalizar"), MARIA, "{\"codigoQr\":\"" + qr + "\"}").andExpect(status().isOk());
        llamar(get(viaje + "/mensajes"), CLIENTE, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteEnviar").value(false))
                .andExpect(jsonPath("$.mensajes", hasSize(2)));
        llamar(post(viaje + "/mensajes"), CLIENTE, mensaje).andExpect(status().isConflict());
    }

    @Test
    void avisosAlDisponibleYAlClienteCorrectoConReintento() throws Exception {
        String binding = "55c650a6-a92b-4e10-859c-FF2D84798402";
        for (String uid : new String[]{CLIENTE, MARIA, FERNANDO, "uid-otro"}) {
            llamar(put(API + "/notificaciones/dispositivo"), uid,
                    "{\"binding\":\"" + binding + "\",\"token\":\"fcm-token-de-prueba-" + uid + "\"}")
                    .andExpect(status().isNoContent());
        }
        llamar(put(API + "/notificaciones/dispositivo"), "uid-admin-1", "{}").andExpect(status().isForbidden());
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "LIM")).andExpect(status().isCreated());
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "LIM")).andExpect(status().isOk());
        assertThat(db.queryForList("SELECT uid FROM avisos_taxi WHERE evento = 'OFERTA'", String.class))
                .containsExactly(MARIA);
        when(fcm.habilitada()).thenReturn(true);
        when(fcm.enviar(eq("fcm-token-de-prueba-" + MARIA), anyMap(), eq(true)))
                .thenReturn(MensajeriaFirebase.Resultado.ENVIADO);
        Object emisor = contexto.getBean("avisosTaxi");
        ReflectionTestUtils.invokeMethod(emisor, "despachar");
        verify(fcm).enviar(eq("fcm-token-de-prueba-" + MARIA), anyMap(), eq(true));
        assertThat(db.queryForObject("SELECT completado FROM avisos_taxi WHERE evento = 'OFERTA'", Boolean.class)).isTrue();

        llamar(post(API + "/servicios/777/aceptar"), MARIA, null).andExpect(status().isOk());
        llamar(post(API + "/servicios/777/aceptar"), MARIA, null).andExpect(status().isOk());
        assertThat(db.queryForList("SELECT uid FROM avisos_taxi WHERE evento = 'ASIGNADO'", String.class))
                .containsExactly(CLIENTE);
        when(fcm.enviar(eq("fcm-token-de-prueba-" + CLIENTE), anyMap(), eq(false)))
                .thenReturn(MensajeriaFirebase.Resultado.REINTENTAR, MensajeriaFirebase.Resultado.ENVIADO);
        ReflectionTestUtils.invokeMethod(emisor, "despachar");
        assertThat(db.queryForObject("SELECT completado FROM avisos_taxi WHERE evento = 'ASIGNADO'", Boolean.class)).isFalse();
        db.update("UPDATE avisos_taxi SET siguiente_intento = CURRENT_TIMESTAMP WHERE evento = 'ASIGNADO'");
        ReflectionTestUtils.invokeMethod(emisor, "despachar");
        assertThat(db.queryForObject("SELECT completado FROM avisos_taxi WHERE evento = 'ASIGNADO'", Boolean.class)).isTrue();
    }

    @Test
    void rutasPublicasYErroresDeSesion() throws Exception {
        llamar(get(API + "/health"), null, null).andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        llamar(get(API + "/aeropuertos"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aeropuertos", hasSize(5)))
                .andExpect(jsonPath("$.aeropuertos[0].nombre").value("Aeropuerto Internacional Jorge Chávez"))
                .andExpect(jsonPath("$.aeropuertos[0].ciudadesHotel", hasItem("lima metropolitana")));
        llamar(get(API + "/taxistas/me"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get(API + "/taxistas/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        llamar(get(API + "/servicios/900001"), "uid-inactivo", null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
        llamar(get(API + "/servicios/900001"), "uid-sin-perfil", null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
        llamar(get(API + "/taxistas/me"), CLIENTE, null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void perfilDelTaxistaYServicioActivo() throws Exception {
        llamar(get(API + "/taxistas/me"), MARIA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("APROBADO"))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.vehiculo.placa").value("XYZ-987"))
                .andExpect(jsonPath("$.calificacion").value(5.0))
                .andExpect(jsonPath("$.servicioActivoId").value(nullValue()));
        llamar(get(API + "/taxistas/me"), FERNANDO, null)
                .andExpect(jsonPath("$.servicioActivoId").value(900003));
        llamar(get(API + "/taxistas/me/servicio-activo"), FERNANDO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(900003))
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"))
                .andExpect(jsonPath("$.ubicacion.lat").value(-12.1131));
        llamar(get(API + "/taxistas/me/servicio-activo"), MARIA, null).andExpect(status().isNoContent());
        llamar(put(API + "/taxistas/me/disponibilidad"), MARIA, "{\"disponible\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    void laCuentaDeLaAppSeEnlazaAlTaxistaAprobadoPorCorreo() throws Exception {
        llamar(get(API + "/taxistas/me"), "uid-carlos", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TAXISTA_NO_HABILITADO"));
        llamar(get(API + "/taxistas/me"), "uid-taxista-sin-registro", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TAXISTA_NO_REGISTRADO"));

        Taxista carlos = taxistaRepository.findByUsuarioCorreo("carlos.demo@iotelito.com").orElseThrow();
        carlos.setEstadoSolicitud(EstadoSolicitudTaxista.APROBADO);
        carlos.getUsuario().setActivo(true);

        llamar(get(API + "/taxistas/me"), "uid-carlos", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("carlos.demo@iotelito.com"));
        org.assertj.core.api.Assertions.assertThat(
                taxistaRepository.findByUsuarioFirebaseUid("uid-carlos")).isPresent();
    }

    @Test
    void flujoCompletoDelPedidoALaCalificacion() throws Exception {
        // El cliente pide el taxi: el aeropuerto debe corresponder a la ciudad del hotel.
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "CUZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AEROPUERTO_NO_CORRESPONDE"));
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "LIM"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(777))
                .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.hotel.nombre").value("Hotel Miraflores Park"))
                .andExpect(jsonPath("$.hotel.direccion").value("Av. Malecón de la Reserva 1035"))
                .andExpect(jsonPath("$.aeropuerto.ciudad").value("Callao"))
                .andExpect(jsonPath("$.clienteNombre").value("Nombre uid-cliente"))
                .andExpect(jsonPath("$.taxista").value(nullValue()))
                .andExpect(jsonPath("$.fechas.solicitado", notNullValue()));
        llamar(post(API + "/servicios"), CLIENTE, pedido(777, "LIM")).andExpect(status().isOk());
        llamar(post(API + "/servicios"), "uid-otro", pedido(777, "LIM")).andExpect(status().isForbidden());
        llamar(post(API + "/servicios"), CLIENTE, pedido(779, "LIM")).andExpect(status().isForbidden());
        llamar(post(API + "/servicios"), CLIENTE, pedido(780, "LIM"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        llamar(get(API + "/servicios/777"), "uid-otro", null).andExpect(status().isForbidden());

        // Los taxistas lo ven; gana uno solo y nadie toma dos a la vez.
        llamar(get(API + "/servicios/disponibles"), MARIA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios[*].id", hasItem(777)));
        llamar(post(API + "/servicios/777/aceptar"), FERNANDO, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SERVICIO_ACTIVO"));
        llamar(post(API + "/servicios/777/aceptar"), MARIA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.taxista.vehiculo.placa").value("XYZ-987"));
        llamar(post(API + "/servicios/777/aceptar"), MARIA, null).andExpect(status().isOk());
        llamar(post(API + "/servicios/777/aceptar"), ANDREA, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));

        // Estados en orden y solo el taxista asignado.
        llamar(post(API + "/servicios/777/estado"), MARIA, "{\"estado\":\"EN_TRASLADO\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
        llamar(post(API + "/servicios/777/estado"), ANDREA, "{\"estado\":\"EN_CAMINO\"}")
                .andExpect(status().isForbidden());
        llamar(post(API + "/servicios/777/estado"), MARIA, "{\"estado\":\"EN_CAMINO\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.fechas.enCamino", notNullValue()));
        llamar(put(API + "/servicios/777/ubicacion"), MARIA, "{\"lat\":-12.12,\"lng\":-77.03,\"precisionM\":6.5}")
                .andExpect(status().isNoContent());
        llamar(put(API + "/servicios/777/ubicacion"), MARIA, "{\"lat\":-120,\"lng\":-77.03}")
                .andExpect(status().isBadRequest());

        // El cliente y el hotel ven el servicio y el QR; otro hotel no.
        llamar(get(API + "/servicios/777"), CLIENTE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"))
                .andExpect(jsonPath("$.ubicacion.lat").value(-12.12))
                .andExpect(jsonPath("$.taxista.nombres").value("Maria"));
        llamar(get(API + "/servicios/777"), "uid-admin-1", null).andExpect(status().isOk());
        llamar(get(API + "/servicios/777"), "uid-admin-2", null).andExpect(status().isForbidden());
        llamar(get(API + "/servicios/777/qr"), "uid-admin-1", null).andExpect(status().isOk());
        String qr = JsonPath.read(llamar(get(API + "/servicios/777/qr"), CLIENTE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", startsWith("TX1-")))
                .andReturn().getResponse().getContentAsString(), "$.codigo");

        // Se finaliza solo con el QR correcto y desde EN_TRASLADO.
        llamar(post(API + "/servicios/777/finalizar"), MARIA, "{\"codigoQr\":\"" + qr + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
        llamar(post(API + "/servicios/777/estado"), MARIA, "{\"estado\":\"EN_TRASLADO\"}").andExpect(status().isOk());
        llamar(post(API + "/servicios/777/finalizar"), MARIA, "{\"codigoQr\":\"TX1-falso\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("QR_INVALIDO"));
        llamar(post(API + "/servicios/777/finalizar"), MARIA, "{\"codigoQr\":\"" + qr + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADO"));
        llamar(put(API + "/servicios/777/ubicacion"), MARIA, "{\"lat\":-12.1,\"lng\":-77.0}")
                .andExpect(status().isConflict());
        llamar(get(API + "/servicios/777/qr"), CLIENTE, null).andExpect(status().isConflict());

        // El cliente califica una vez y el promedio se actualiza (5 de antes y 4 ahora).
        llamar(post(API + "/servicios/777/calificacion"), CLIENTE, "{\"puntuacion\":6}")
                .andExpect(status().isBadRequest());
        llamar(post(API + "/servicios/777/calificacion"), "uid-otro", "{\"puntuacion\":4}")
                .andExpect(status().isForbidden());
        llamar(post(API + "/servicios/777/calificacion"), CLIENTE, "{\"puntuacion\":4,\"comentario\":\" Muy bien \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calificacion.puntuacion").value(4))
                .andExpect(jsonPath("$.calificacion.comentario").value("Muy bien"));
        llamar(post(API + "/servicios/777/calificacion"), CLIENTE, "{\"puntuacion\":5}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("YA_CALIFICADO"));
        llamar(get(API + "/taxistas/me"), MARIA, null)
                .andExpect(jsonPath("$.cantidadServicios").value(2))
                .andExpect(jsonPath("$.calificacion").value(4.5));
        llamar(get(API + "/taxistas/me/servicios"), MARIA, null)
                .andExpect(jsonPath("$.servicios", hasSize(2)))
                .andExpect(jsonPath("$.servicios[0].id").value(777));
    }

    @Test
    void superadminListaYDesactivaTaxistas() throws Exception {
        llamar(get(API + "/taxistas").param("estadoSolicitud", "PENDIENTE"), "uid-super", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxistas", hasSize(4)))
                .andExpect(jsonPath("$.taxistas[*].tipoDocumento", hasItem("PASAPORTE")));
        llamar(get(API + "/taxistas"), CLIENTE, null).andExpect(status().isForbidden());

        Long maria = taxistaRepository.findByUsuarioFirebaseUid(MARIA).orElseThrow().getId();
        llamar(patch(API + "/taxistas/" + maria + "/activo"), "uid-super", "{\"activo\":false,\"motivo\":\"Prueba\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.disponible").value(false));
        llamar(get(API + "/taxistas/me"), MARIA, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TAXISTA_NO_HABILITADO"));
    }

    @Test
    void datosInvalidosRespondenValidacion() throws Exception {
        llamar(post(API + "/servicios"), CLIENTE,
                "{\"reservaId\":777,\"aeropuertoCodigo\":\"LIM\",\"hora\":\"25:00\",\"pasajeros\":2,\"maletas\":1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        llamar(post(API + "/servicios"), CLIENTE, "{no es json")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        llamar(get(API + "/servicios/abc"), CLIENTE, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        llamar(get(API + "/servicios/123456"), CLIENTE, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        llamar(get(API + "/taxistas/me/servicios").param("limite", "500"), MARIA, null)
                .andExpect(status().isBadRequest());
    }
}
