package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.service.TipoEquipoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tipos-equipo")
public class TipoEquipoController {

    private final TipoEquipoService tipoService;

    public TipoEquipoController(TipoEquipoService tipoService) {
        this.tipoService = tipoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", tipoService.listarTodos());
        return "tipo-equipo/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("tipo", new TipoEquipo());
        return "tipo-equipo/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return tipoService.buscarPorId(id)
                .map(tipo -> {
                    model.addAttribute("tipo", tipo);
                    return "tipo-equipo/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El tipo de equipo no existe");
                    return "redirect:/tipos-equipo";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("tipo") TipoEquipo tipo,
                          BindingResult result,
                          RedirectAttributes flash) {

        boolean esNuevo = (tipo.getId() == null);

        if (!result.hasFieldErrors("nombre")
                && tipoService.existeNombreEnOtro(tipo.getNombre().trim(), tipo.getId())) {
            result.rejectValue("nombre", "nombre.duplicado",
                    "Ya existe un tipo de equipo con ese nombre");
        }

        if (result.hasErrors()) {
            return "tipo-equipo/formulario";
        }

        TipoEquipo destino = esNuevo ? tipo : tipoService.buscarPorId(tipo.getId()).orElse(null);
        if (destino == null) {
            flash.addFlashAttribute("error", "El tipo de equipo no existe");
            return "redirect:/tipos-equipo";
        }

        destino.setNombre(tipo.getNombre().trim());
        tipoService.guardar(destino);

        flash.addFlashAttribute("exito",
                esNuevo ? "Tipo de equipo registrado correctamente"
                        : "Tipo de equipo actualizado correctamente");
        return "redirect:/tipos-equipo";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        long equipos = tipoService.contarEquiposActivos(id);

        if (equipos > 0) {
            flash.addFlashAttribute("error",
                    "No se puede inactivar: hay " + equipos
                    + " equipo(s) activo(s) de este tipo. Reasignelos primero.");
            return "redirect:/tipos-equipo";
        }

        tipoService.desactivar(id);
        flash.addFlashAttribute("exito", "Tipo de equipo inactivado correctamente");
        return "redirect:/tipos-equipo";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes flash) {
        tipoService.activar(id);
        flash.addFlashAttribute("exito", "Tipo de equipo activado correctamente");
        return "redirect:/tipos-equipo";
    }
}