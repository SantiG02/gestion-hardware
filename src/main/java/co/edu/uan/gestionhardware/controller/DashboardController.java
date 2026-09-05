package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.IndicadorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final IndicadorService indicadorService;

    public DashboardController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("resumen", indicadorService.construirResumen());
        return "dashboard/panel";
    }
}