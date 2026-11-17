package com.iotelito.taxiweb.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Página de inicio con el login del superadmin. El POST /login lo atiende Spring Security. */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String salio,
            Authentication autenticacion,
            Model model
    ) {
        if (autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            return "redirect:/superadmin/dashboard";
        }
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos");
        } else if (salio != null) {
            model.addAttribute("mensaje", "Cerraste sesión");
        }
        return "inicio";
    }
}
