package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.CategoriaFalla;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.EquipoService;
import co.edu.uan.gestionhardware.service.IncidenciaService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modulo del rol Usuario Final (agregado a solicitud del jurado): reportar
 * incidencias, consultar y editar/eliminar las propias mientras sigan
 * abiertas, y confirmar o rechazar la solucion cuando el tecnico las cierra.
 *
 * El equipo tecnico y el gestor le dan seguimiento a estas incidencias
 * desde el modulo normal de /incidencias, como a cualquier otra.
 */
@Controller
@RequestMapping("/mis-incidencias")
public class MisIncidenciasController {

    private final IncidenciaService incidenciaService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    public MisIncidenciasController(IncidenciaService incidenciaService,
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

    @GetMapping
    public String listar(Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        model.addAttribute("incidencias", incidenciaService.listarPorUsuario(usuario.getId()));
        return "incidencia/mis-incidencias";
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("incidencia", new Incidencia());
        return "incidencia/reportar";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model,
                                          Authentication authentication, RedirectAttributes flash) {

        Usuario usuario = usuarioActual(authentication);

        return incidenciaService.buscarPorId(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuario.getId()))
                .<String>map(i -> {
                    if (!"ABIERTA".equals(i.getEstado())) {
                        flash.addFlashAttribute("error", "Esta incidencia ya no se puede editar");
                        return "redirect:/mis-incidencias";
                    }
                    model.addAttribute("incidencia", i);
                    return "incidencia/reportar";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "La incidencia no existe");
                    return "redirect:/mis-incidencias";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("incidencia") Incidencia incidencia,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes flash) {

        // El formulario reducido no incluye reportadoPor ni fechaReporte
        // (se asignan automaticamente), asi que sus errores de "obligatorio"
        // se ignoran aqui -- son esperados en este punto, no errores reales.
        boolean hayErroresReales = result.getFieldErrors().stream()
                .anyMatch(error -> !error.getField().equals("reportadoPor")
                        && !error.getField().equals("fechaReporte"));

        if (hayErroresReales) {
            return "incidencia/reportar";
        }

        Usuario usuario = usuarioActual(authentication);

        if (incidencia.getId() == null) {

            incidencia.setReportadoPor(usuario);
            incidencia.setFechaReporte(LocalDateTime.now());
            incidencia.setGeneraIndisponibilidad(false);
            incidenciaService.guardar(incidencia);

            flash.addFlashAttribute("exito",
                    "Incidencia reportada correctamente. El equipo tecnico la va a revisar.");

        } else {

            boolean actualizo = incidenciaService.actualizarPropia(incidencia.getId(), usuario.getId(),
                    incidencia.getEquipo(), incidencia.getCategoriaFalla(),
                    incidencia.getPrioridad(), incidencia.getDescripcion());

            flash.addFlashAttribute(actualizo ? "exito" : "error",
                    actualizo ? "Incidencia actualizada correctamente"
                              : "No se pudo actualizar (ya no esta abierta o no te pertenece)");
        }

        return "redirect:/mis-incidencias";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication authentication, RedirectAttributes flash) {

        Usuario usuario = usuarioActual(authentication);
        boolean eliminado = incidenciaService.eliminarPropia(id, usuario.getId());

        flash.addFlashAttribute(eliminado ? "exito" : "error",
                eliminado ? "Incidencia eliminada" : "No se pudo eliminar (ya no esta abierta o no te pertenece)");

        return "redirect:/mis-incidencias";
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, Authentication authentication, RedirectAttributes flash) {

        Usuario usuario = usuarioActual(authentication);
        boolean confirmado = incidenciaService.confirmarSolucion(id, usuario.getId());

        flash.addFlashAttribute(confirmado ? "exito" : "error",
                confirmado ? "Gracias por confirmar. La incidencia queda cerrada."
                           : "No se pudo confirmar la incidencia");

        return "redirect:/mis-incidencias";
    }

    @GetMapping("/{id}/rechazar")
    public String mostrarFormularioRechazo(@PathVariable Long id, Model model,
                                           Authentication authentication, RedirectAttributes flash) {

        Usuario usuario = usuarioActual(authentication);

        return incidenciaService.buscarPorId(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuario.getId()))
                .filter(i -> "CERRADA".equals(i.getEstado()) && "PENDIENTE".equals(i.getConfirmacionUsuario()))
                .<String>map(i -> {
                    model.addAttribute("incidencia", i);
                    return "incidencia/rechazar";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "Esta incidencia no esta pendiente de tu confirmacion");
                    return "redirect:/mis-incidencias";
                });
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id,
                           @RequestParam String comentario,
                           Authentication authentication,
                           RedirectAttributes flash) {

        if (comentario == null || comentario.isBlank()) {
            flash.addFlashAttribute("error", "Cuentanos que sigue fallando");
            return "redirect:/mis-incidencias/" + id + "/rechazar";
        }

        Usuario usuario = usuarioActual(authentication);
        boolean ok = incidenciaService.rechazarSolucion(id, usuario.getId(), comentario);

        flash.addFlashAttribute(ok ? "exito" : "error",
                ok ? "Avisamos al tecnico que el problema sigue."
                   : "No se pudo procesar tu respuesta");

        return "redirect:/mis-incidencias";
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioService.obtenerPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }
}