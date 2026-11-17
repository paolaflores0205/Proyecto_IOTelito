package com.iotelito.taxiweb;

import com.iotelito.taxiweb.firebase.CuentaFirebaseException;
import com.iotelito.taxiweb.firebase.CuentasFirebase;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.repository.TaxistaRepository;
import com.iotelito.taxiweb.service.TaxistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Aprobar crea la cuenta de la app y rechazar la bloquea, sin pasar por la consola de Firebase. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:iotelito_cuentas;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@Transactional
@Import(CuentasFirebaseFlujoTest.FirebaseFalso.class)
class CuentasFirebaseFlujoTest {

    @TestConfiguration
    static class FirebaseFalso {
        static final List<String> LLAMADAS = new ArrayList<>();
        static volatile boolean fallar;

        @Bean
        @Primary
        CuentasFirebase cuentasFalsas() {
            return new CuentasFirebase() {
                @Override
                public Optional<String> prepararCuentaTaxista(DatosTaxista taxista) {
                    if (fallar) {
                        throw new CuentaFirebaseException("El correo ya tiene una cuenta de CLIENT en la app.");
                    }
                    LLAMADAS.add("preparar " + taxista.correo());
                    return Optional.of("uid-" + taxista.correo());
                }

                @Override
                public void bloquear(String uid) {
                    LLAMADAS.add("bloquear " + uid);
                }
            };
        }
    }

    @Autowired
    private TaxistaService taxistaService;

    @Autowired
    private TaxistaRepository taxistaRepository;

    @BeforeEach
    void limpiar() {
        FirebaseFalso.LLAMADAS.clear();
        FirebaseFalso.fallar = false;
    }

    private Taxista porCorreo(String correo) {
        return taxistaRepository.findByUsuarioCorreo(correo).orElseThrow();
    }

    @Test
    void aprobarCreaLaCuentaYGuardaSuUid() {
        Taxista carlos = taxistaService.aprobarTaxista(porCorreo("carlos.demo@iotelito.com").getId(), "admin@iotelito.com");

        assertThat(FirebaseFalso.LLAMADAS).containsExactly("preparar carlos.demo@iotelito.com");
        assertThat(carlos.getUsuario().getFirebaseUid()).isEqualTo("uid-carlos.demo@iotelito.com");
        assertThat(carlos.getEstadoSolicitud()).isEqualTo(EstadoSolicitudTaxista.APROBADO);
    }

    @Test
    void siFirebaseFallaNoSeAprueba() {
        Long id = porCorreo("diego.demo@iotelito.com").getId();
        FirebaseFalso.fallar = true;

        CuentaFirebaseException error = assertThrows(CuentaFirebaseException.class,
                () -> taxistaService.aprobarTaxista(id, "admin@iotelito.com"));

        assertThat(error.getMessage()).contains("CLIENT");
    }

    @Test
    void rechazarBloqueaLaCuenta() {
        taxistaService.rechazarTaxista(porCorreo("maria.demo@iotelito.com").getId(), "admin@iotelito.com");

        assertThat(FirebaseFalso.LLAMADAS).containsExactly("bloquear demo-uid-maria");
    }
}
