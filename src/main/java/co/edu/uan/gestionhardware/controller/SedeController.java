package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Sede;
import co.edu.uan.gestionhardware.service.SedeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/sedes")

public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService){

        this.sedeService = sedeService;
    }
    
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("sedes", sedeService.listarTodos());
        return "sede/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("sede", new Sede());
        return "sede/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        return sedeService.buscarPorId(id)
            .map(sede -> {
                model.addAttribute("sede", sede);
                return "sede/formulario";
            })
            .orElseGet(() -> {
                flash.addFlashAttribute("error", "La Sede no existe");
                return "redirect:/sedes";
            });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("sede") Sede sede, BindingResult result,
    RedirectAttributes flash) {
    
        if (result.hasErrors()) {
            return "sede/formulario";
        }

        boolean esNuevo = (sede.getId() == null);
        sedeService.guardar(sede);
        flash.addFlashAttribute("exito",
            esNuevo ? "Sede registrada correctamente"
                : "Sede actualizada correctamente");
        return "redirect:/sedes";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        sedeService.desactivar(id);
        flash.addFlashAttribute("exito", "Sede inactivada correctamente");
        return "redirect:/sedes";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes flash) {
        sedeService.activar(id);
        flash.addFlashAttribute("exito", "Sede activada correctamente");
        return "redirect:/sedes";
}







}
