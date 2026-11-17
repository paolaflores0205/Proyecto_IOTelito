package com.iotelito.taxiweb;

import com.iotelito.taxiweb.config.AdminInicialConfig;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Con IOTELITO_ADMIN_PASSWORD la contraseña de desarrollo deja de servir. Base H2 propia. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iotelito_admin;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "iotelito.admin.password=Clave-Prueba-2026"
})
class AdminInicialConfigTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void reemplazaLaContrasenaDeDesarrollo() {
        String hash = usuarioRepository.findByCorreo("admin@iotelito.com").orElseThrow().getPasswordHash();

        assertThat(passwordEncoder.matches("Clave-Prueba-2026", hash)).isTrue();
        assertThat(passwordEncoder.matches("admin123", hash)).isFalse();
    }

    @Test
    void rechazaContrasenasCortas() {
        AdminInicialConfig config = new AdminInicialConfig(usuarioRepository, passwordEncoder, "admin@iotelito.com", "corta");

        assertThrows(IllegalStateException.class, () -> config.run(null));
    }
}
