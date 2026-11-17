package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.model.CampoTipoEquipo;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.EstadoEquipo;
import co.edu.uan.gestionhardware.model.Sede;
import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.CampoTipoEquipoService;
import co.edu.uan.gestionhardware.service.EquipoService;
import co.edu.uan.gestionhardware.service.HistorialService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import co.edu.uan.gestionhardware.service.ValorCampoEquipoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService equipoService;
    private final UsuarioService usuarioService;
    private final HistorialService historialService;
    private final CampoTipoEquipoService campoService;
    private final ValorCampoEquipoService valorService;

    public EquipoController(EquipoService equipoService,
                            UsuarioService usuarioService,
                            HistorialService historialService,
                            CampoTipoEquipoService campoService,
                            ValorCampoEquipoService valorService) {
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
        this.historialService = historialService;
        this.campoService = campoService;
        this.valorService = valorService;
    }

    // Se ejecutan antes de cualquier metodo de este controlador.
    // Dejan las listas disponibles en todas las vistas sin repetir codigo.

    @ModelAttribute("sedes")
    public List<Sede> cargarSedes() {
        return equipoService.listarSedes();
    }

    @ModelAttribute("areas")
    public List<Area> cargarAreas() {
        return equipoService.listarAreas();
    }

    @ModelAttribute("tipos")
    public List<TipoEquipo> cargarTipos() {
        return equipoService.listarTipos();
    }

    // Campos propios de cada tipo, agrupados por id de tipo (formulario dinamico).
    @ModelAttribute("camposPorTipo")
    public Map<Long, List<CampoTipoEquipo>> cargarCamposPorTipo() {
        return campoService.listarActivosAgrupadosPorTipo();
    }

    @ModelAttribute("estados")
    public List<EstadoEquipo> cargarEstados() {
        return equipoService.listarEstados();
    }

    @ModelAttribute("usuarios")
    public List<Usuario> cargarUsuarios() {
        return usuarioService.listarActivos();
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String texto,
                         @RequestParam(required = false) Long sedeId,
                         @RequestParam(required = false) Long areaId,
                         @RequestParam(required = false) Long estadoId,
                         @RequestParam(required = false) Long responsableId,
                         Model model) {
        model.addAttribute("equipos",
                equipoService.filtrar(texto, sedeId, areaId, estadoId, responsableId));
        return "equipo/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("equipo", new Equipo());
        model.addAttribute("valores", new HashMap<Long, String>());
        return "equipo/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes flash) {
        return equipoService.buscarPorId(id)
                .map(equipo -> {
                    model.addAttribute("equipo", equipo);
                    model.addAttribute("valores", valorService.valoresPorCampo(id));
                    return "equipo/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El equipo no existe");
                    return "redirect:/equipos";
                });
    }

    @GetMapping("/{id}/historial")
    public String historial(@PathVariable Long id,
                            Model model,
                            RedirectAttributes flash) {
        return equipoService.buscarPorId(id)
                .map(equipo -> {
                    model.addAttribute("equipo", equipo);
                    model.addAttribute("eventos", historialService.obtenerHistorial(id));
                    model.addAttribute("valoresCampos", valorService.listarPorEquipo(id));
                    return "equipo/historial";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El equipo no existe");
                    return "redirect:/equipos";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("equipo") Equipo equipo,
                          BindingResult result,
                          @RequestParam Map<String, String> params,
                          Model model,
                          RedirectAttributes flash) {

        // Valores de los campos propios del tipo (parametros campo_<id>).
        Map<Long, String> valores = extraerValores(params);

        // El código interno y el serial no pueden repetirse en otro equipo.
        if (equipo.getCodigoInterno() != null) {
            equipoService.buscarPorCodigoInterno(equipo.getCodigoInterno())
                    .filter(otro -> !otro.getId().equals(equipo.getId()))
                    .ifPresent(otro -> result.rejectValue("codigoInterno", "duplicado",
                            "Ya existe un equipo con este código interno"));
        }
        if (equipo.getSerial() != null) {
            equipoService.buscarPorSerial(equipo.getSerial())
                    .filter(otro -> !otro.getId().equals(equipo.getId()))
                    .ifPresent(otro -> result.rejectValue("serial", "duplicado",
                            "Ya existe un equipo con este serial"));
        }

        Map<Long, String> erroresCampos =
                valorService.validar(equipo.getTipoEquipo(), valores);

        if (result.hasErrors() || !erroresCampos.isEmpty()) {
            model.addAttribute("valores", valores);
            model.addAttribute("erroresCampos", erroresCampos);
            return "equipo/formulario";
        }

        boolean esNuevo = (equipo.getId() == null);
        equipoService.guardarConValores(equipo, valores);

        flash.addFlashAttribute("exito",
                esNuevo ? "Equipo registrado correctamente"
                        : "Equipo actualizado correctamente");

        return "redirect:/equipos";
    }

    private Map<Long, String> extraerValores(Map<String, String> params) {
        Map<Long, String> valores = new HashMap<>();
        for (Map.Entry<String, String> entrada : params.entrySet()) {
            if (entrada.getKey().startsWith("campo_")) {
                try {
                    valores.put(Long.valueOf(entrada.getKey().substring(6)), entrada.getValue());
                } catch (NumberFormatException ignorado) {
                    // Parametro mal formado: se ignora.
                }
            }
        }
        return valores;
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        equipoService.desactivar(id);
        flash.addFlashAttribute("exito", "Equipo dado de baja del inventario");
        return "redirect:/equipos";
    }
}