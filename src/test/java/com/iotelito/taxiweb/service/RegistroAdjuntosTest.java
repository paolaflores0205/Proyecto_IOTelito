package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.model.DocumentoTaxista;
import com.iotelito.taxiweb.model.EstadoDocumento;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.TipoArchivoTaxista;
import com.iotelito.taxiweb.repository.DocumentoTaxistaRepository;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iotelito_adjuntos;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "iotelito.taxistas.s3.bucket=${TAXI_DOCS_BUCKET:}"
})
@AutoConfigureMockMvc
class RegistroAdjuntosTest {

    private static final String ADMIN = "admin@iotelito.com";
    private static final URI ENLACE_DE_PRUEBA = URI.create("https://storage.example.test/adjunto");
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l1sAAAAASUVORK5CYII=");
    private static final byte[] PDF = Base64.getDecoder().decode(
            "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCAyMDAgODBdIC9Db250ZW50cyA0IDAgUiA+PgplbmRvYmoKNCAwIG9iago8PCAvTGVuZ3RoIDI3ID4+CnN0cmVhbQowIDAgMSByZyAxNSAxNSAxNzAgNTAgcmUgZgplbmRzdHJlYW0KZW5kb2JqCnhyZWYKMCA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAwOSAwMDAwMCBuIAowMDAwMDAwMDU4IDAwMDAwIG4gCjAwMDAwMDAxMTUgMDAwMDAgbiAKMDAwMDAwMDIwMSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDUgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjI3NwolJUVPRgo=");

    @Autowired private MockMvc mvc;
    @Autowired private DocumentoTaxistaRepository documentos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private DocumentosTaxistaService servicioDocumentos;
    @MockitoSpyBean private AlmacenArchivosTaxistaS3 almacenamiento;

    @Test
    void registraLosSeisAdjuntosYElSuperadminPuedeAbrirlos() throws Exception {
        boolean usaS3Real = almacenamiento.habilitado();
        if (!usaS3Real) {
            doReturn(true).when(almacenamiento).habilitado();
            doNothing().when(almacenamiento).guardar(anyString(), any(), anyString());
            doReturn(ENLACE_DE_PRUEBA).when(almacenamiento).enlaceTemporal(anyString(), anyString());
        }

        Taxista taxista = (Taxista) mvc.perform(solicitud("101")
                        .file(archivo("fotoConductorFile", true))
                        .file(archivo("fotoVehiculoFile", true))
                        .file(archivo("dniFile", false))
                        .file(archivo("licenciaFile", false))
                        .file(archivo("soatFile", false))
                        .file(archivo("tarjetaPropiedadFile", false)))
                .andExpect(view().name("registro-exitoso"))
                .andReturn().getModelAndView().getModel().get("taxista");

        List<DocumentoTaxista> adjuntos = documentos.findByTaxistaOrderByTipo(taxista);
        assertThat(adjuntos).extracting(DocumentoTaxista::getTipo)
                .containsExactlyInAnyOrder(TipoArchivoTaxista.values());
        assertThat(adjuntos).allSatisfy(adjunto -> {
            assertThat(adjunto.getArchivoUrl()).startsWith("taxistaDocs/" + taxista.getId() + "/");
            assertThat(adjunto.getEstado()).isEqualTo(EstadoDocumento.PENDIENTE);
        });

        String detalle = "/superadmin/taxistas/" + taxista.getId();
        for (String panel : List.of(detalle, detalle + "/evaluar")) {
            var render = mvc.perform(get(panel).with(user(ADMIN).roles("SUPERADMIN")))
                    .andExpect(status().isOk());
            for (DocumentoTaxista adjunto : adjuntos) {
                render.andExpect(content().string(containsString(ruta(taxista.getId(), adjunto.getId()))));
            }
        }

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        for (DocumentoTaxista adjunto : adjuntos) {
            var respuesta = mvc.perform(get(ruta(taxista.getId(), adjunto.getId()))
                            .with(user(ADMIN).roles("SUPERADMIN")))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andReturn().getResponse();

            boolean esFoto = adjunto.getTipo().name().startsWith("FOTO_");
            assertThat(adjunto.getTamanoBytes()).isEqualTo(esFoto ? PNG.length : PDF.length);
            assertThat(adjunto.getContentType()).isEqualTo(esFoto ? "image/png" : "application/pdf");
            URI enlace = URI.create(respuesta.getHeader("Location"));
            if (usaS3Real) {
                var objeto = http.send(HttpRequest.newBuilder(enlace).timeout(Duration.ofSeconds(15)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                assertThat(objeto.statusCode()).isEqualTo(200);
                assertThat(objeto.body()).isEqualTo(esFoto ? PNG : PDF);
            } else {
                assertThat(enlace).isEqualTo(ENLACE_DE_PRUEBA);
            }
        }

        Long documentoId = adjuntos.get(0).getId();
        mvc.perform(get(ruta(taxista.getId(), documentoId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost/"));
        mvc.perform(get(ruta(taxista.getId(), documentoId)).with(user("taxista").roles("TAXISTA")))
                .andExpect(status().isForbidden());
        mvc.perform(get(ruta(taxista.getId() + 1, documentoId)).with(user(ADMIN).roles("SUPERADMIN")))
                .andExpect(status().isNotFound());
        var fotos = servicioDocumentos.fotos(taxista);
        assertThat(fotos.conductor()).isNotBlank();
        assertThat(fotos.vehiculo()).isNotBlank();
    }

    @Test
    void rechazaUnaFotoConFormatoInvalidoAntesDeRegistrar() throws Exception {
        mvc.perform(solicitud("102").file(new MockMultipartFile("fotoVehiculoFile", "falsa.png",
                        "image/png", new byte[]{1, 2, 3, 4})))
                .andExpect(view().name("registro-taxista"))
                .andExpect(model().attribute("error", containsString("JPG o PNG")));
        assertThat(usuarios.existsByCorreo("adjuntos102@example.test")).isFalse();
        verify(almacenamiento, never()).guardar(anyString(), any(), anyString());
    }

    @Test
    void revierteElRegistroYLimpiaLosObjetosSiFallaLaSegundaSubida() throws Exception {
        doReturn(true).when(almacenamiento).habilitado();
        doNothing().when(almacenamiento).guardar(anyString(), any(), anyString());
        doNothing().when(almacenamiento).eliminar(anyString());
        doThrow(new IllegalStateException("error interno de S3"))
                .when(almacenamiento).guardar(contains("/foto_vehiculo/"), any(), eq("image/png"));
        long documentosAntes = documentos.count();

        mvc.perform(solicitud("103")
                        .file(archivo("fotoConductorFile", true))
                        .file(archivo("fotoVehiculoFile", true)))
                .andExpect(view().name("registro-taxista"))
                .andExpect(model().attribute("error", containsString("No pudimos guardar los archivos")));
        assertThat(usuarios.existsByCorreo("adjuntos103@example.test")).isFalse();
        assertThat(documentos.count()).isEqualTo(documentosAntes);
        verify(almacenamiento, times(1)).eliminar(contains("/foto_conductor/"));
        verify(almacenamiento, times(1)).eliminar(contains("/foto_vehiculo/"));
    }

    private static MockMultipartHttpServletRequestBuilder solicitud(String codigo) {
        var solicitud = multipart("/taxistas/registro");
        solicitud.with(csrf())
                .param("nombres", "Taxista").param("apellidos", "De Prueba")
                .param("correo", "adjuntos" + codigo + "@example.test").param("telefono", "900000001")
                .param("tipoDocumento", "DNI").param("numeroDocumento", "90000" + codigo)
                .param("fechaNacimiento", "1995-06-01").param("domicilio", "Domicilio de prueba")
                .param("numeroLicencia", "Q90000" + codigo).param("categoriaLicencia", "A-IIb")
                .param("fechaVencimientoLicencia", "2030-01-01").param("marcaVehiculo", "Kia")
                .param("modeloVehiculo", "Rio").param("anioVehiculo", "2022").param("colorVehiculo", "Gris")
                .param("placaVehiculo", "S3T-" + codigo).param("aceptaTerminos", "true");
        return solicitud;
    }

    private static MockMultipartFile archivo(String campo, boolean imagen) {
        return new MockMultipartFile(campo, imagen ? "ejemplo.png" : "ejemplo.pdf",
                "application/octet-stream", imagen ? PNG : PDF);
    }

    private static String ruta(Long taxistaId, Long documentoId) {
        return "/superadmin/taxistas/" + taxistaId + "/documentos/" + documentoId;
    }
}
