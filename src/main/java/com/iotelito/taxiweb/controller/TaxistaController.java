package com.iotelito.taxiweb.controller;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.service.TaxistaService;

import org.springframework.stereotype.Controller;
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

        try {

            Taxista taxista =
                    taxistaService.registrarTaxista(form);

            model.addAttribute(
                    "taxista",
                    taxista
            );

            return "registro-exitoso";

        } catch (RuntimeException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "registro-taxista";
        }
    }
}