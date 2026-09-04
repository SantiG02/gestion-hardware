package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.CategoriaFalla;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.EquipoService;
import co.edu.uan.gestionhardware.service.IncidenciaService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    public IncidenciaController(IncidenciaService incidenciaService,
                                EquipoService equipoService,
                                UsuarioService usuarioService) {
        this.incidenciaService = incidenciaService;
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("equipos")
    public List<Equipo> cargarEquipos() {
        return equipoService.listarActivos();
    }

    @ModelAttribute("categorias")
    public List<CategoriaFalla> cargarCategorias() {
        return incidenciaService.listarCategorias();
    }

    @ModelAttribute("usuarios")
    public List<Usuario> cargarUsuarios() {
        return usuarioService.listarActivos();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("incidencias", incidenciaService.listarTodas());
        return "incidencia/lista";
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevo(Model model) {
        Incidencia incidencia = new Incidencia();
        incidencia.setFechaReporte(LocalDateTime.now());
        model.addAttribute("incidencia", incidencia);
        return "incidencia/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return incidenciaService.buscarPorId(id)
                .map(incidencia -> {
                    model.addAttribute("incidencia", incidencia);
                    return "incidencia/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La incidencia no existe");
                    return "redirect:/incidencias";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("incidencia") Incidencia incidencia,
                          BindingResult result,
                          RedirectAttributes flash) {

        if (Boolean.TRUE.equals(incidencia.getGeneraIndisponibilidad())
                && incidencia.getFechaInicioIndisponibilidad() == null) {
            result.rejectValue("fechaInicioIndisponibilidad", "inicio.requerido",
                    "Debe indicar desde cuando el equipo esta fuera de servicio");
        }

        if (incidencia.getFechaInicioIndisponibilidad() != null
                && incidencia.getFechaFinIndisponibilidad() != null
                && incidencia.getFechaFinIndisponibilidad()
                        .isBefore(incidencia.getFechaInicioIndisponibilidad())) {
            result.rejectValue("fechaFinIndisponibilidad", "fin.anterior",
                    "La fecha de fin no puede ser anterior a la de inicio");
        }

        if (result.hasErrors()) {
            return "incidencia/formulario";
        }

        boolean esNueva = (incidencia.getId() == null);

        boolean yaTenia = esNueva
                && incidencia.getEquipo() != null
                && incidenciaService.tieneIncidenciaAbierta(incidencia.getEquipo().getId());

        incidenciaService.guardar(incidencia);

        if (yaTenia) {
            flash.addFlashAttribute("error",
                    "Incidencia registrada. Atencion: este equipo ya tenia otra incidencia sin cerrar.");
        } else {
            flash.addFlashAttribute("exito",
                    esNueva ? "Incidencia registrada correctamente"
                            : "Incidencia actualizada correctamente");
        }

        return "redirect:/incidencias";
    }

    @GetMapping("/{id}/cerrar")
    public String mostrarFormularioCierre(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return incidenciaService.buscarPorId(id)
                .map(incidencia -> {
                    if ("CERRADA".equals(incidencia.getEstado())) {
                        flash.addFlashAttribute("error", "Esta incidencia ya esta cerrada");
                        return "redirect:/incidencias";
                    }
                    model.addAttribute("incidencia", incidencia);
                    return "incidencia/cierre";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La incidencia no existe");
                    return "redirect:/incidencias";
                });
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id,
                         @RequestParam String solucion,
                         @RequestParam(required = false)
                         @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
                         LocalDateTime fechaFin,
                         RedirectAttributes flash) {

        if (solucion == null || solucion.isBlank()) {
            flash.addFlashAttribute("error", "Debe describir la solucion aplicada");
            return "redirect:/incidencias/" + id + "/cerrar";
        }

        return incidenciaService.cerrar(id, solucion, fechaFin)
                .map(incidencia -> {
                    flash.addFlashAttribute("exito", "Incidencia cerrada correctamente");
                    return "redirect:/incidencias";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La incidencia no existe");
                    return "redirect:/incidencias";
                });
    }
}