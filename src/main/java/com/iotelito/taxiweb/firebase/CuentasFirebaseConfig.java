package com.iotelito.taxiweb.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Con IOTELITO_FIREBASE_CREDENCIALES (ruta al .json de la cuenta de servicio, fuera del repo)
 * las cuentas se crean solas. Sin ella, aprobar funciona igual pero la cuenta se crea a mano.
 */
@Configuration
public class CuentasFirebaseConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CuentasFirebaseConfig.class);

    @Bean
    CuentasFirebase cuentasFirebase(
            @Value("${iotelito.firebase.credenciales:}") String ruta,
            @Value("${iotelito.firebase.project-id}") String proyecto,
            @Value("${iotelito.firebase.web-api-key:}") String claveWeb,
            ObjectMapper json
    ) throws IOException {
        if (ruta == null || ruta.isBlank()) {
            LOG.info("Sin cuenta de servicio de Firebase: las cuentas de taxista se crean a mano");
            return new CuentasFirebase() {
                @Override
                public Optional<String> prepararCuentaTaxista(DatosTaxista taxista) {
                    return Optional.empty();
                }

                @Override
                public void bloquear(String uid) {
                }
            };
        }
        try (InputStream archivo = Files.newInputStream(Path.of(ruta))) {
            return new CuentasFirebaseRest(archivo, proyecto, claveWeb, json);
        }
    }
}
