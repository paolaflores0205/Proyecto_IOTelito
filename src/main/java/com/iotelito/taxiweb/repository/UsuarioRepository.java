package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCorreoAndPassword(String correo, String password);
}
