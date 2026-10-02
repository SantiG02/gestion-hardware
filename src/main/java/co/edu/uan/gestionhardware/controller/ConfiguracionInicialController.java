package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Creacion de la primera cuenta de administrador (requisito del tutor).
 * Solo funciona mientras no exista ningun usuario con rol GESTOR en el
 * sistema; apenas se crea el primero, esta ruta deja de hacer nada y
 * redirige al login. Los administradores siguientes se crean desde
 * /usuarios, ya autenticado, como cualquier otro usuario.
 */
@Controller
@RequestMapping("/configuracion-inicial")
public class ConfiguracionInicialController {

    private final UsuarioService usuarioService;

    public ConfiguracionInicialController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrarFormulario() {
        if (usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }
        return "configuracion-inicial/crear";
    }

    @PostMapping("/crear")
    public String crear(@RequestParam String nombreCompleto,
                        @RequestParam String email,
                        @RequestParam String password,
                        @RequestParam String passwordConfirmacion,
                        Model model,
                        RedirectAttributes flash) {

        // Revalidar: si alguien mas ya creo el administrador mientras este
        // formulario estaba abierto, no se permite crear un segundo por aqui.
        if (usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }

        if (nombreCompleto == null || nombreCompleto.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            model.addAttribute("error", "Todos los campos son obligatorios");
            return "configuracion-inicial/crear";
        }

        if (password.length() < 8) {
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres");
            return "configuracion-inicial/crear";
        }

        if (!password.equals(passwordConfirmacion)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "configuracion-inicial/crear";
        }

        if (usuarioService.existeEmail(email)) {
            model.addAttribute("error", "Ese correo ya está registrado");
            return "configuracion-inicial/crear";
        }

        usuarioService.crearPrimerAdministrador(nombreCompleto, email, password);

        flash.addFlashAttribute("exito",
                "Cuenta de administrador creada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}