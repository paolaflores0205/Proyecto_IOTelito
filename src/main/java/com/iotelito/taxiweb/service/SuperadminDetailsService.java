package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.model.Rol;
import com.iotelito.taxiweb.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Solo un superadmin activo con contraseña puede entrar al panel web. */
@Service
public class SuperadminDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public SuperadminDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        String normalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByCorreo(normalizado)
                .filter(usuario -> usuario.getRol() == Rol.SUPERADMIN && usuario.getPasswordHash() != null)
                .map(usuario -> User.withUsername(usuario.getCorreo())
                        .password(usuario.getPasswordHash())
                        .roles(Rol.SUPERADMIN.name())
                        .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
