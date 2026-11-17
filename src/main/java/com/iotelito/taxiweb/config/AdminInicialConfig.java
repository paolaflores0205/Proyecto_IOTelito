package com.iotelito.taxiweb.config;

import com.iotelito.taxiweb.model.Rol;
import com.iotelito.taxiweb.model.Usuario;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Al desplegar se define IOTELITO_ADMIN_PASSWORD (y opcionalmente IOTELITO_ADMIN_CORREO):
 * la web guarda su hash BCrypt para el superadmin, creándolo si no existe. Así la
 * contraseña de desarrollo de data.sql no llega a producción y ninguna contraseña real
 * queda en el repositorio. Sin la variable no hace nada.
 */
@Component
public class AdminInicialConfig implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicialConfig.class);
    static final int LONGITUD_MINIMA = 12;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public AdminInicialConfig(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${iotelito.admin.correo:admin@iotelito.com}") String correo,
            @Value("${iotelito.admin.password:}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            return;
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException(
                    "IOTELITO_ADMIN_PASSWORD debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }

        Usuario admin = usuarioRepository.findByCorreo(correo).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setNombres("Super");
            nuevo.setApellidos("Administrador");
            nuevo.setCorreo(correo);
            nuevo.setRol(Rol.SUPERADMIN);
            return nuevo;
        });
        if (admin.getRol() != Rol.SUPERADMIN) {
            throw new IllegalStateException("La cuenta " + correo + " existe y no es superadmin");
        }
        if (admin.getPasswordHash() != null && passwordEncoder.matches(password, admin.getPasswordHash())
                && Boolean.TRUE.equals(admin.getActivo())) {
            return;
        }

        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setActivo(true);
        usuarioRepository.save(admin);
        LOG.info("Contraseña del superadmin {} actualizada desde la configuración", correo);
    }
}
