package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    // Se ejecuta antes de CUALQUIER metodo de este controlador.
    // Deja las listas disponibles en todas las vistas sin repetir codigo.
    @ModelAttribute("areas")
    public Object cargarAreas() {
        return equipoService.listarAreas();
    }

    @ModelAttribute("tipos")
    public Object cargarTipos() {
        return equipoService.listarTipos();
    }

    @ModelAttribute("estados")
    public Object cargarEstados() {
        return equipoService.listarEstados();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("equipos", equipoService.listarActivos());
        return "equipo/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("equipo", new Equipo());
        return "equipo/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return equipoService.buscarPorId(id)
                .map(equipo -> {
                    model.addAttribute("equipo", equipo);
                    return "equipo/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El equipo no existe");
                    return "redirect:/equipos";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("equipo") Equipo equipo,
                          BindingResult result,
                          RedirectAttributes flash) {

        if (result.hasErrors()) {
            return "equipo/formulario";
        }

        boolean esNuevo = (equipo.getId() == null);
        equipoService.guardar(equipo);

        flash.addFlashAttribute("exito",
                esNuevo ? "Equipo registrado correctamente"
                        : "Equipo actualizado correctamente");

        return "redirect:/equipos";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        equipoService.desactivar(id);
        flash.addFlashAttribute("exito", "Equipo dado de baja del inventario");
        return "redirect:/equipos";
    }
}