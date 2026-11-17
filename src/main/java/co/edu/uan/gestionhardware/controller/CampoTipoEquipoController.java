package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.CampoTipoEquipo;
import co.edu.uan.gestionhardware.model.TipoDatoCampo;
import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.service.CampoTipoEquipoService;
import co.edu.uan.gestionhardware.service.TipoEquipoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Definicion de los campos propios de cada tipo de equipo (solo GESTOR,
 * por la regla /tipos-equipo/** de SecurityConfig).
 */
@Controller
@RequestMapping("/tipos-equipo/{tipoId}/campos")
public class CampoTipoEquipoController {

    private final TipoEquipoService tipoService;
    private final CampoTipoEquipoService campoService;

    public CampoTipoEquipoController(TipoEquipoService tipoService,
                                     CampoTipoEquipoService campoService) {
        this.tipoService = tipoService;
        this.campoService = campoService;
    }

    @ModelAttribute("tiposDato")
    public TipoDatoCampo[] cargarTiposDato() {
        return TipoDatoCampo.values();
    }

    @GetMapping
    public String listar(@PathVariable Long tipoId, Model model, RedirectAttributes flash) {
        return tipoService.buscarPorId(tipoId)
                .map(tipo -> {
                    model.addAttribute("tipo", tipo);
                    model.addAttribute("campos", campoService.listarPorTipo(tipoId));
                    return "tipo-equipo/campos";
                })
                .orElseGet(() -> redirigirTipoInexistente(flash));
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(@PathVariable Long tipoId,
                                         Model model,
                                         RedirectAttributes flash) {
        return tipoService.buscarPorId(tipoId)
                .map(tipo -> {
                    model.addAttribute("tipo", tipo);
                    model.addAttribute("campo", new CampoTipoEquipo());
                    return "tipo-equipo/campo-formulario";
                })
                .orElseGet(() -> redirigirTipoInexistente(flash));
    }

    @GetMapping("/{campoId}/editar")
    public String mostrarFormularioEditar(@PathVariable Long tipoId,
                                          @PathVariable Long campoId,
                                          Model model,
                                          RedirectAttributes flash) {
        TipoEquipo tipo = tipoService.buscarPorId(tipoId).orElse(null);
        if (tipo == null) {
            return redirigirTipoInexistente(flash);
        }
        return campoService.buscarPorId(campoId)
                .filter(c -> perteneceAlTipo(c, tipoId))
                .map(campo -> {
                    model.addAttribute("tipo", tipo);
                    model.addAttribute("campo", campo);
                    return "tipo-equipo/campo-formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El campo no existe");
                    return "redirect:/tipos-equipo/" + tipoId + "/campos";
                });
    }

    @PostMapping("/guardar")
    public String guardar(@PathVariable Long tipoId,
                          @Valid @ModelAttribute("campo") CampoTipoEquipo campo,
                          BindingResult result,
                          Model model,
                          RedirectAttributes flash) {

        TipoEquipo tipo = tipoService.buscarPorId(tipoId).orElse(null);
        if (tipo == null) {
            return redirigirTipoInexistente(flash);
        }
        model.addAttribute("tipo", tipo);

        boolean esNuevo = (campo.getId() == null);

        if (campo.getTipoDato() == null) {
            campo.setTipoDato(TipoDatoCampo.TEXTO);
        }

        String nombre = campo.getNombre() == null ? "" : campo.getNombre().trim();

        if (!result.hasFieldErrors("nombre")
                && campoService.existeNombreEnOtro(tipoId, nombre, campo.getId())) {
            result.rejectValue("nombre", "nombre.duplicado",
                    "Ya existe un campo con ese nombre en este tipo");
        }

        String opcionesNormalizadas = null;
        if (campo.getTipoDato() == TipoDatoCampo.LISTA) {
            opcionesNormalizadas = normalizarOpciones(campo.getOpciones());
            if (opcionesNormalizadas.isEmpty()) {
                result.rejectValue("opciones", "opciones.vacias",
                        "Escriba al menos una opcion, separadas por coma");
            } else if (opcionesNormalizadas.length() > 500) {
                result.rejectValue("opciones", "opciones.largas", "Maximo 500 caracteres");
            }
        }

        if (result.hasErrors()) {
            return "tipo-equipo/campo-formulario";
        }

        CampoTipoEquipo destino = campo;
        if (!esNuevo) {
            destino = campoService.buscarPorId(campo.getId())
                    .filter(c -> perteneceAlTipo(c, tipoId))
                    .orElse(null);
            if (destino == null) {
                flash.addFlashAttribute("error", "El campo no existe");
                return "redirect:/tipos-equipo/" + tipoId + "/campos";
            }
        }

        destino.setTipoEquipo(tipo);
        destino.setNombre(nombre);
        destino.setTipoDato(campo.getTipoDato());
        destino.setOpciones(opcionesNormalizadas);
        destino.setObligatorio(Boolean.TRUE.equals(campo.getObligatorio()));
        campoService.guardar(destino);

        flash.addFlashAttribute("exito",
                esNuevo ? "Campo registrado correctamente"
                        : "Campo actualizado correctamente");
        return "redirect:/tipos-equipo/" + tipoId + "/campos";
    }

    @PostMapping("/{campoId}/desactivar")
    public String desactivar(@PathVariable Long tipoId,
                             @PathVariable Long campoId,
                             RedirectAttributes flash) {
        campoService.buscarPorId(campoId)
                .filter(c -> perteneceAlTipo(c, tipoId))
                .ifPresent(c -> campoService.desactivar(campoId));
        flash.addFlashAttribute("exito", "Campo inactivado correctamente");
        return "redirect:/tipos-equipo/" + tipoId + "/campos";
    }

    @PostMapping("/{campoId}/activar")
    public String activar(@PathVariable Long tipoId,
                          @PathVariable Long campoId,
                          RedirectAttributes flash) {
        campoService.buscarPorId(campoId)
                .filter(c -> perteneceAlTipo(c, tipoId))
                .ifPresent(c -> campoService.activar(campoId));
        flash.addFlashAttribute("exito", "Campo activado correctamente");
        return "redirect:/tipos-equipo/" + tipoId + "/campos";
    }

    // ---- utilidades

    // Se compara por id del tipo (la relacion es LAZY y no hay sesion abierta aqui,
    // pero getId() de un proxy no la necesita).
    private boolean perteneceAlTipo(CampoTipoEquipo campo, Long tipoId) {
        return campo.getTipoEquipo() != null && tipoId.equals(campo.getTipoEquipo().getId());
    }

    private String normalizarOpciones(String texto) {
        if (texto == null) {
            return "";
        }
        Set<String> opciones = Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return String.join(",", opciones);
    }

    private String redirigirTipoInexistente(RedirectAttributes flash) {
        flash.addFlashAttribute("error", "El tipo de equipo no existe");
        return "redirect:/tipos-equipo";
    }
}
