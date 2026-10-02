package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.EquipoService;
import co.edu.uan.gestionhardware.service.IncidenciaService;
import co.edu.uan.gestionhardware.service.MantenimientoService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Panel de inicio del Tecnico de Soporte: solo los indicadores relevantes
 * para su trabajo diario -- sus propios mantenimientos pendientes, las
 * incidencias abiertas de la empresa, los mantenimientos por vencer, y el
 * total de equipos activos. Sin el resto de lo que ve el Gestor
 * Tecnologico en /dashboard (no le compete reclasificar equipos, mandar
 * alertas por correo, ni ver reportes).
 */
@Controller
@RequestMapping("/panel-tecnico")
public class PanelTecnicoController {

    private final MantenimientoService mantenimientoService;
    private final IncidenciaService incidenciaService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    public PanelTecnicoController(MantenimientoService mantenimientoService,
                                  IncidenciaService incidenciaService,
                                  EquipoService equipoService,
                                  UsuarioService usuarioService) {
        this.mantenimientoService = mantenimientoService;
        this.incidenciaService = incidenciaService;
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrar(Model model, Authentication authentication) {

        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        model.addAttribute("misMantenimientosPendientes",
                mantenimientoService.contarPendientesPorTecnico(usuario.getId()));
        model.addAttribute("incidenciasAbiertas", incidenciaService.listarAbiertas().size());
        model.addAttribute("mantenimientosProximos", mantenimientoService.listarProximosAVencer(7).size());
        model.addAttribute("totalEquipos", equipoService.listarActivos().size());

        return "panel-tecnico/panel";
    }
}