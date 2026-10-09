package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Creacion de la primera cuenta de administrador (requisito del tutor).
 * El formulario vive en la pestaña "Crear cuenta" del login; este
 * controlador solo recibe el envio. Solo funciona mientras no exista ningun
 * usuario con rol GESTOR; apenas se crea el primero, esta ruta deja de hacer
 * nada y redirige al login. Despues, las cuentas nuevas son solicitudes que
 * el administrador aprueba, o las crea el mismo desde /usuarios.
 */
@Controller
@RequestMapping("/configuracion-inicial")
public class ConfiguracionInicialController {

    private final UsuarioService usuarioService;

    public ConfiguracionInicialController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Se conserva la ruta antigua: ahora todo ocurre en el login. */
    @GetMapping
    public String mostrarFormulario() {
        return "redirect:/login";
    }

    @PostMapping("/crear")
    public String crear(@RequestParam String nombreCompleto,
                        @RequestParam String email,
                        @RequestParam String password,
                        @RequestParam String passwordConfirmacion,
                        @RequestParam(required = false) String aceptaTerminos,
                        HttpServletRequest request,
                        Model model,
                        RedirectAttributes flash) {

        // Revalidar: si alguien mas ya creo el administrador mientras este
        // formulario estaba abierto, no se permite crear un segundo por aqui.
        if (usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }

        String nombre = nombreCompleto == null ? "" : nombreCompleto.trim();
        String correo = email == null ? "" : email.trim().toLowerCase();
        String error = null;

        if (nombre.isEmpty() || correo.isEmpty() || password == null || password.isBlank()) {
            error = "Todos los campos son obligatorios";
        } else if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || correo.length() > 120) {
            error = "Ingresa un correo electrónico válido";
        } else if (password.length() < 8) {
            error = "La contraseña debe tener al menos 8 caracteres";
        } else if (!password.equals(passwordConfirmacion)) {
            error = "Las contraseñas no coinciden";
        } else if (usuarioService.existeEmail(correo)) {
            error = "Ese correo ya está registrado";
        } else if (aceptaTerminos == null) {
            error = "Debes aceptar los Términos y Condiciones para crear la cuenta";
        }

        if (error != null) {
            model.addAttribute("sinGestor", true);
            model.addAttribute("tabCrear", true);
            model.addAttribute("errorCrear", error);
            model.addAttribute("valorNombre", nombre);
            model.addAttribute("valorEmail", correo);
            return "login";
        }

        usuarioService.crearPrimerAdministrador(nombre, correo, password, request.getRemoteAddr());

        flash.addFlashAttribute("exito",
                "Cuenta de administrador creada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}