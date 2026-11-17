package com.iotelito.taxiweb;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.TipoDocumento;
import com.iotelito.taxiweb.service.TaxistaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistroTaxistaTest {

    @Autowired
    private TaxistaService taxistaService;

    @Autowired
    private MockMvc mvc;

    private static TaxistaRegistroForm formulario(TipoDocumento tipo, String numero) {
        TaxistaRegistroForm form = new TaxistaRegistroForm();
        form.setNombres("Lucía");
        form.setApellidos("Prado Gómez");
        form.setCorreo("  Lucia.Prado@Example.test ");
        form.setTelefono("987000222");
        form.setTipoDocumento(tipo);
        form.setNumeroDocumento(numero);
        form.setFechaNacimiento(LocalDate.of(1995, 6, 1));
        form.setDomicilio("Av. Pardo 500, Miraflores");
        form.setNumeroLicencia("q99887766");
        form.setCategoriaLicencia("A-IIb");
        form.setFechaVencimientoLicencia(LocalDate.of(2030, 1, 1));
        form.setMarcaVehiculo("Toyota");
        form.setModeloVehiculo("Yaris");
        form.setAnioVehiculo(2023);
        form.setColorVehiculo("Blanco");
        form.setPlacaVehiculo("abc-999");
        form.setAceptaTerminos(true);
        return form;
    }

    @Test
    void registraConPasaporteSinContrasenaYPendiente() {
        Taxista taxista = taxistaService.registrarTaxista(formulario(TipoDocumento.PASAPORTE, "AB123456"));

        assertThat(taxista.getId()).isNotNull();
        assertThat(taxista.getDocumento()).isEqualTo("Pasaporte AB123456");
        assertThat(taxista.getEstadoSolicitud()).isEqualTo(EstadoSolicitudTaxista.PENDIENTE);
        assertThat(taxista.getPlacaVehiculo()).isEqualTo("ABC-999");
        assertThat(taxista.getNumeroLicencia()).isEqualTo("Q99887766");
        assertThat(taxista.getUsuario().getCorreo()).isEqualTo("lucia.prado@example.test");
        assertThat(taxista.getUsuario().getPasswordHash()).isNull();
        assertThat(taxista.getUsuario().getActivo()).isFalse();
    }

    @Test
    void validaElFormatoSegunElTipoDeDocumento() {
        RuntimeException dni = assertThrows(RuntimeException.class,
                () -> taxistaService.registrarTaxista(formulario(TipoDocumento.DNI, "1234567A")));
        RuntimeException carne = assertThrows(RuntimeException.class,
                () -> taxistaService.registrarTaxista(formulario(TipoDocumento.CARNET_EXTRANJERIA, "12345")));

        assertThat(dni.getMessage()).isEqualTo("El DNI debe tener 8 dígitos");
        assertThat(carne.getMessage()).contains("carné de extranjería");
    }

    @Test
    void noRepiteDocumentoPeroSiAceptaElMismoNumeroEnOtroTipo() {
        RuntimeException repetido = assertThrows(RuntimeException.class,
                () -> taxistaService.registrarTaxista(formulario(TipoDocumento.DNI, "72148965")));

        assertThat(repetido.getMessage()).isEqualTo("Ya existe una solicitud con ese documento");
        assertThat(taxistaService.registrarTaxista(formulario(TipoDocumento.PASAPORTE, "72148965")).getId()).isNotNull();
    }

    @Test
    void elFormularioWebRegistraConTokenCsrf() throws Exception {
        mvc.perform(multipart("/taxistas/registro").with(csrf())
                        .param("nombres", "Lucía").param("apellidos", "Prado Gómez")
                        .param("correo", "lucia.web@example.test").param("telefono", "987000333")
                        .param("tipoDocumento", "CARNET_EXTRANJERIA").param("numeroDocumento", "000123456")
                        .param("fechaNacimiento", "1995-06-01").param("domicilio", "Av. Pardo 500")
                        .param("numeroLicencia", "Q11223344").param("categoriaLicencia", "A-IIb")
                        .param("fechaVencimientoLicencia", "2030-01-01").param("marcaVehiculo", "Kia")
                        .param("modeloVehiculo", "Rio").param("anioVehiculo", "2022").param("colorVehiculo", "Gris")
                        .param("placaVehiculo", "WEB-123").param("aceptaTerminos", "true"))
                .andExpect(view().name("registro-exitoso"))
                .andExpect(model().attributeExists("taxista"));
    }
}
