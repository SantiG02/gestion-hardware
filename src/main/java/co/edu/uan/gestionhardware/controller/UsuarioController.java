package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.model.Rol;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.AreaService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;


@Controller
@RequestMapping("/usuarios")

public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AreaService areaService;

    public UsuarioController(UsuarioService usuarioService, AreaService areaService){
        this.usuarioService = usuarioService;
        this.areaService = areaService;
    }

    @ModelAttribute("roles")
    public List<Rol> cargarRoles() {
        return usuarioService.listaRols();
    }

    @ModelAttribute("areas")
    public List<Area> cargarAreas() {
        return areaService.listarTodas();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuario/lista";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        usuarioService.desactivar(id);
        flash.addFlashAttribute("exito", "Usuario inactivado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes flash) {
        usuarioService.activar(id);
        flash.addFlashAttribute("exito", "Usuario activado correctamente");
        return "redirect:/usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuario/formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                      Model model,
                                      RedirectAttributes flash) {
        return usuarioService.buscarPorId(id)
            .map(usuario -> {
                model.addAttribute("usuario", usuario);
                return "usuario/formulario";
            })
            .orElseGet(() -> {
                flash.addFlashAttribute("error", "El usuario no existe");
                return "redirect:/usuarios";
            });
        }
    
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult result,
    @RequestParam(required = false) String passwordPlano,RedirectAttributes flash) {
        
        if (usuario.getId() == null && (passwordPlano == null || passwordPlano.isBlank())) {
            result.reject("password.requerida",
            "La contraseña es obligatoria para un usuario nuevo");
        }

        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()
            && usuarioService.existeEmail(usuario.getEmail())) {

            boolean esOtroUsuario = usuario.getId() == null
                || usuarioService.buscarPorId(usuario.getId())
                .map(original -> !original.getEmail().equalsIgnoreCase(usuario.getEmail()))
                .orElse(true);

            if (esOtroUsuario) {
                result.rejectValue("email", "email.duplicado",
                "Ya existe un usuario registrado con este correo");
            }
        }
                    
        if (result.hasErrors()) {
            return "usuario/formulario";
        }
        boolean esNuevo = (usuario.getId() == null);
        usuarioService.guardar(usuario,passwordPlano);
        flash.addFlashAttribute("exito",
            esNuevo ? "Usuario registrado correctamente"
                    : "Usuario actualizado correctamente");
        return "redirect:/usuarios";
}




    
}
