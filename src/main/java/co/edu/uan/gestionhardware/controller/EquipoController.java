package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.EquipoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("equipos", equipoService.listarActivos());
        return "equipo/lista";
    }
}