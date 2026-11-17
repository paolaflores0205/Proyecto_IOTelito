package com.iotelito.taxiweb;

import com.iotelito.taxiweb.repository.LogSistemaRepository;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** El panel del superadmin exige sesión; antes cualquiera podía abrir /superadmin/... */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadPanelTest {

    private static final String ADMIN = "admin@iotelito.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TaxistaRepository taxistaRepository;

    @Autowired
    private LogSistemaRepository logRepository;

    @Test
    void elPanelPideIniciarSesion() throws Exception {
        for (String ruta : new String[]{"/superadmin/dashboard", "/superadmin/taxistas", "/superadmin/reportes/exportar"}) {
            mvc.perform(get(ruta))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/"));
        }
    }

    @Test
    void entraConLaContrasenaCorrecta() throws Exception {
        mvc.perform(formLogin("/login").userParameter("correo").passwordParam("password").user(ADMIN).password("admin123"))
                .andExpect(authenticated().withUsername(ADMIN).withRoles("SUPERADMIN"))
                .andExpect(redirectedUrl("/superadmin/dashboard"));
    }

    @Test
    void noEntraConContrasenaIncorrectaNiConUnTaxista() throws Exception {
        mvc.perform(formLogin("/login").userParameter("correo").passwordParam("password").user(ADMIN).password("otra"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/?error"));
        mvc.perform(formLogin("/login").userParameter("correo").passwordParam("password").user("maria.demo@iotelito.com").password("demo123"))
                .andExpect(unauthenticated());
    }

    @Test
    void elLoginSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/login").param("correo", ADMIN).param("password", "admin123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void conSesionSeVenElPanelYLosDocumentos() throws Exception {
        mvc.perform(get("/superadmin/dashboard").with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("web-dashboard-taxistas"));
        mvc.perform(get("/superadmin/taxistas").param("q", "PE4321").with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pasaporte PE4321698")));
    }

    @Test
    void aprobarExigeTokenCsrfYQuedaRegistrado() throws Exception {
        Long carlos = taxistaRepository.findAll().stream()
                .filter(t -> t.getUsuario().getCorreo().equals("carlos.demo@iotelito.com"))
                .findFirst().orElseThrow().getId();
        long logs = logRepository.count();

        mvc.perform(post("/superadmin/taxistas/{id}/aprobar", carlos).with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/superadmin/taxistas/{id}/aprobar", carlos).with(user(ADMIN).roles("SUPERADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/superadmin/taxistas/" + carlos));

        assertThat(logRepository.count()).isEqualTo(logs + 1);
        assertThat(taxistaRepository.findById(carlos).orElseThrow().getUsuario().getActivo()).isTrue();
    }

    @Test
    void todasLasVistasDelPanelCargan() throws Exception {
        Long id = taxistaRepository.findAll().get(0).getId();
        String[] rutas = {"/superadmin/dashboard", "/superadmin/perfil", "/superadmin/notificaciones",
                "/superadmin/taxistas", "/superadmin/taxistas/" + id, "/superadmin/solicitudes/evaluar",
                "/superadmin/taxistas/" + id + "/evaluar", "/superadmin/taxistas/flota", "/superadmin/reportes",
                "/superadmin/reportes/exportar"};
        for (String ruta : rutas) {
            mvc.perform(get(ruta).with(user(ADMIN).roles("SUPERADMIN")))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/superadmin/reportes/exportar").with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(content().string(containsString("tipo_documento,numero_documento")))
                .andExpect(content().string(containsString("PASAPORTE,\"PE4321698\"")));
    }

    @Test
    void cerrarSesionVuelveAlInicio() throws Exception {
        mvc.perform(get("/logout").with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(redirectedUrl("/?salio"));
    }

    @Test
    void elRegistroDeTaxistasEsPublico() throws Exception {
        mvc.perform(get("/taxistas/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carné de extranjería")));
    }
}
