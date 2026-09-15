package com.iotelito.taxiweb.controller;

import com.iotelito.taxiweb.model.Rol;
import com.iotelito.taxiweb.repository.UsuarioRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;

    public InicioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/")
    public String inicio() {

        return "inicio";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String correo,
            @RequestParam String password,
            Model model
    ) {
        boolean credencialesValidas = usuarioRepository
                .findByCorreoAndPassword(correo, password)
                .filter(usuario -> usuario.getRol() == Rol.SUPERADMIN)
                .isPresent();

        if (credencialesValidas) {
            return "redirect:/superadmin/dashboard";
        }

        model.addAttribute("error", "Correo o contrasena incorrectos");

        return "inicio";
    }
}
