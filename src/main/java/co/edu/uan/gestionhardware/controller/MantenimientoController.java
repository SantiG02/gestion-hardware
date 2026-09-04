package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Mantenimiento;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.EquipoService;
import co.edu.uan.gestionhardware.service.IncidenciaService;
import co.edu.uan.gestionhardware.service.MantenimientoService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;
    private final IncidenciaService incidenciaService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    public MantenimientoController(MantenimientoService mantenimientoService,
                                   IncidenciaService incidenciaService,
                                   EquipoService equipoService,
                                   UsuarioService usuarioService) {
        this.mantenimientoService = mantenimientoService;
        this.incidenciaService = incidenciaService;
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("equipos")
    public List<Equipo> cargarEquipos() {
        return equipoService.listarActivos();
    }

    @ModelAttribute("incidenciasAbiertas")
    public List<Incidencia> cargarIncidencias() {
        return incidenciaService.listarAbiertas();
    }

    @ModelAttribute("tecnicos")
    public List<Usuario> cargarTecnicos() {
        return usuarioService.listarActivos();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("mantenimientos", mantenimientoService.listarTodos());
        return "mantenimiento/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setTipo("PREVENTIVO");
        model.addAttribute("mantenimiento", mantenimiento);
        return "mantenimiento/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return mantenimientoService.buscarPorId(id)
                .map(mantenimiento -> {
                    model.addAttribute("mantenimiento", mantenimiento);
                    return "mantenimiento/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El mantenimiento no existe");
                    return "redirect:/mantenimientos";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("mantenimiento") Mantenimiento mantenimiento,
                          BindingResult result,
                          RedirectAttributes flash) {

        if ("CORRECTIVO".equals(mantenimiento.getTipo())
                && mantenimiento.getIncidencia() == null) {
            result.rejectValue("incidencia", "incidencia.requerida",
                    "Un mantenimiento correctivo debe asociarse a una incidencia");
        }

        if (mantenimiento.getFechaProgramada() != null
                && mantenimiento.getFechaEjecucion() != null
                && mantenimiento.getFechaEjecucion().isBefore(mantenimiento.getFechaProgramada())) {
            result.rejectValue("fechaEjecucion", "ejecucion.anterior",
                    "La fecha de ejecucion no puede ser anterior a la programada");
        }

        if (mantenimiento.getFechaEjecucion() != null
                && (mantenimiento.getResultado() == null || mantenimiento.getResultado().isBlank())) {
            result.rejectValue("resultado", "resultado.requerido",
                    "Debe indicar el resultado de la intervencion ejecutada");
        }

        if (result.hasErrors()) {
            return "mantenimiento/formulario";
        }

        boolean esNuevo = (mantenimiento.getId() == null);
        Mantenimiento guardado = mantenimientoService.guardar(mantenimiento);

        boolean correctivoConIncidenciaAbierta =
                guardado.getIncidencia() != null
                && !"CERRADA".equals(guardado.getIncidencia().getEstado())
                && "FINALIZADO".equals(guardado.getEstado());

        if (correctivoConIncidenciaAbierta) {
            flash.addFlashAttribute("aviso",
                    "Mantenimiento registrado. La incidencia asociada sigue abierta: recuerde cerrarla si la falla quedo resuelta.");
            flash.addFlashAttribute("incidenciaPendienteId", guardado.getIncidencia().getId());
        } else {
            flash.addFlashAttribute("exito",
                    esNuevo ? "Mantenimiento registrado correctamente"
                            : "Mantenimiento actualizado correctamente");
        }

        return "redirect:/mantenimientos";
    }

    @GetMapping("/{id}/finalizar")
    public String mostrarFormularioFinalizar(@PathVariable Long id,
                                             Model model,
                                             RedirectAttributes flash) {
        return mantenimientoService.buscarPorId(id)
                .map(mantenimiento -> {
                    if ("FINALIZADO".equals(mantenimiento.getEstado())) {
                        flash.addFlashAttribute("error", "Este mantenimiento ya fue finalizado");
                        return "redirect:/mantenimientos";
                    }
                    model.addAttribute("mantenimiento", mantenimiento);
                    return "mantenimiento/finalizar";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El mantenimiento no existe");
                    return "redirect:/mantenimientos";
                });
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEjecucion,
                            @RequestParam String actividades,
                            @RequestParam String resultado,
                            @RequestParam(required = false) String observaciones,
                            RedirectAttributes flash) {

        if (actividades == null || actividades.isBlank()) {
            flash.addFlashAttribute("error", "Debe describir las actividades realizadas");
            return "redirect:/mantenimientos/" + id + "/finalizar";
        }

        return mantenimientoService
                .finalizar(id, fechaEjecucion, actividades, resultado, observaciones)
                .map(mantenimiento -> {
                    flash.addFlashAttribute("exito", "Mantenimiento finalizado correctamente");
                    return "redirect:/mantenimientos";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El mantenimiento no existe");
                    return "redirect:/mantenimientos";
                });
    }
}