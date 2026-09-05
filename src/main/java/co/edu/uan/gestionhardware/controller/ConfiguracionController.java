package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
import co.edu.uan.gestionhardware.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("configuracion", configuracionService.obtener());
        return "configuracion/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("configuracion") ConfiguracionSistema configuracion,
                          BindingResult result,
                          RedirectAttributes flash) {

        if (result.hasErrors()) {
            return "configuracion/formulario";
        }

        configuracionService.guardar(configuracion);
        flash.addFlashAttribute("exito", "Configuracion actualizada correctamente");
        return "redirect:/configuracion";
    }
}