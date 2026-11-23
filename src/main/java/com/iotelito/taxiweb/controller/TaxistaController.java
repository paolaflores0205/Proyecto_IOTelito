package com.iotelito.taxiweb.controller;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.service.TaxistaService;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/taxistas")
public class TaxistaController {

    private final TaxistaService taxistaService;

    public TaxistaController(
            TaxistaService taxistaService
    ) {
        this.taxistaService = taxistaService;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "form",
                new TaxistaRegistroForm()
        );

        return "registro-taxista";
    }


    @PostMapping("/registro")
    public String registrarTaxista(
            @ModelAttribute("form")
            TaxistaRegistroForm form,
            Model model
    ) {

        // Errores de formato: se muestran todos a la vez, en rojo debajo de cada campo.
        java.util.Map<String, String> errores = com.iotelito.taxiweb.service.RegistroTaxistaValidador.validar(
                form, java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")));
        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            model.addAttribute("error", "Revisa los campos marcados en rojo.");
            return "registro-taxista";
        }

        try {

            Taxista taxista =
                    taxistaService.registrarTaxista(form);

            model.addAttribute(
                    "taxista",
                    taxista
            );

            return "registro-exitoso";

        } catch (DataAccessException | TransactionException e) {
            model.addAttribute("error", "No pudimos guardar la solicitud. Revisa los datos e intenta de nuevo.");
            return "registro-taxista";
        } catch (com.iotelito.taxiweb.service.CampoInvalidoException e) {
            model.addAttribute("errores", java.util.Map.of(e.getCampo(), e.getMessage()));
            model.addAttribute("error", "Revisa los campos marcados en rojo.");
            return "registro-taxista";
        } catch (RuntimeException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "registro-taxista";
        }
    }

}
