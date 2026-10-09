package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        boolean sinGestor = !usuarioService.existeAlgunGestor();
        model.addAttribute("sinGestor", sinGestor);
        model.addAttribute("tabCrear", sinGestor);
        return "login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}