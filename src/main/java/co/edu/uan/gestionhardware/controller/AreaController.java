package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.service.AreaService;
import co.edu.uan.gestionhardware.service.SedeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sedes/{sedeId}/areas")
public class AreaController {

    private final AreaService areaService;
    private final SedeService sedeService;

    public AreaController(AreaService areaService, SedeService sedeService) {
        this.areaService = areaService;
        this.sedeService = sedeService;
    }

    @GetMapping
    public String listar(@PathVariable Long sedeId, Model model, RedirectAttributes flash) {
        return sedeService.buscarPorId(sedeId)
                .map(sede -> {
                    model.addAttribute("sede", sede);
                    model.addAttribute("areas", areaService.listarPorSede(sedeId));
                    return "area/lista";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La sede no existe");
                    return "redirect:/sedes";
                });
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevo(@PathVariable Long sedeId,
                                         Model model,
                                         RedirectAttributes flash) {
        return sedeService.buscarPorId(sedeId)
                .map(sede -> {
                    model.addAttribute("sede", sede);
                    model.addAttribute("area", new Area());
                    return "area/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La sede no existe");
                    return "redirect:/sedes";
                });
    }

    @GetMapping("/{areaId}/editar")
    public String mostrarFormularioEditar(@PathVariable Long sedeId,
                                          @PathVariable Long areaId,
                                          Model model,
                                          RedirectAttributes flash) {
        return areaService.buscarPorId(areaId)
                .map(area -> {
                    model.addAttribute("sede", area.getSede());
                    model.addAttribute("area", area);
                    return "area/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El area no existe");
                    return "redirect:/sedes/" + sedeId + "/areas";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@PathVariable Long sedeId,
                          @Valid @ModelAttribute("area") Area area,
                          BindingResult result,
                          Model model,
                          RedirectAttributes flash) {

        boolean esNueva = (area.getId() == null);

        if (area.getNombre() != null && !area.getNombre().isBlank()
                && areaService.existeNombreEnSede(sedeId, area.getNombre())) {

            boolean esOtraArea = esNueva
                    || areaService.buscarPorId(area.getId())
                        .map(original -> !original.getNombre().equalsIgnoreCase(area.getNombre()))
                        .orElse(true);

            if (esOtraArea) {
                result.rejectValue("nombre", "nombre.duplicado",
                        "Ya existe un area con ese nombre en esta sede");
            }
        }

        if (result.hasErrors()) {
            sedeService.buscarPorId(sedeId).ifPresent(sede -> model.addAttribute("sede", sede));
            return "area/formulario";
        }

        sedeService.buscarPorId(sedeId).ifPresent(area::setSede);
        areaService.guardar(area);

        flash.addFlashAttribute("exito",
                esNueva ? "Area registrada correctamente"
                        : "Area actualizada correctamente");

        return "redirect:/sedes/" + sedeId + "/areas";
    }

    @PostMapping("/{areaId}/desactivar")
    public String desactivar(@PathVariable Long sedeId,
                             @PathVariable Long areaId,
                             RedirectAttributes flash) {

        long equipos = areaService.contarEquiposActivos(areaId);
        areaService.desactivar(areaId);

        if (equipos > 0) {
            flash.addFlashAttribute("error",
                    "Area inactivada. Tiene " + equipos + " equipo(s) activo(s) asociado(s) que requieren reubicacion.");
        } else {
            flash.addFlashAttribute("exito", "Area inactivada correctamente");
        }

        return "redirect:/sedes/" + sedeId + "/areas";
    }

    @PostMapping("/{areaId}/activar")
    public String activar(@PathVariable Long sedeId,
                          @PathVariable Long areaId,
                          RedirectAttributes flash) {
        areaService.activar(areaId);
        flash.addFlashAttribute("exito", "Area activada correctamente");
        return "redirect:/sedes/" + sedeId + "/areas";
    }
}