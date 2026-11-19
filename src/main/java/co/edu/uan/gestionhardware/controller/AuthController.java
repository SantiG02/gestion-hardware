package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.config.RedireccionPorRolSuccessHandler;
import co.edu.uan.gestionhardware.service.UsuarioService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model, Authentication autenticacion) {
        if (haySesionAbierta(autenticacion)) {
            return "redirect:" + RedireccionPorRolSuccessHandler.destinoPorRol(autenticacion);
        }
        boolean sinGestor = !usuarioService.existeAlgunGestor();
        model.addAttribute("sinGestor", sinGestor);
        model.addAttribute("tabCrear", sinGestor);
        return "login";
    }

    // Spring Security reenvia aqui con el metodo original de la peticion (por
    // ejemplo un POST rechazado por CSRF); con solo GET respondia 405.
    @RequestMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }

    /**
     * Si llega un POST a /login con la sesion ya abierta (por ejemplo, al enviar
     * de nuevo el formulario), se manda al usuario a su pagina de inicio en
     * lugar de mostrar el error 405.
     */
    @PostMapping("/login")
    public String loginConSesionAbierta(Authentication autenticacion) {
        if (haySesionAbierta(autenticacion)) {
            return "redirect:" + RedireccionPorRolSuccessHandler.destinoPorRol(autenticacion);
        }
        return "redirect:/login";
    }

    private boolean haySesionAbierta(Authentication autenticacion) {
        return autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken);
    }
}
