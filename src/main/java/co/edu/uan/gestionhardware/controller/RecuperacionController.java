package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.NotificacionService;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Recuperacion de cuenta por correo (requisito del tutor). El mensaje de
 * confirmacion es siempre el mismo exista o no el correo en el sistema,
 * para no revelar que cuentas estan registradas.
 */
@Controller
public class RecuperacionController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public RecuperacionController(UsuarioService usuarioService,
                                  NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/recuperar")
    public String mostrarSolicitud() {
        return "recuperacion/solicitar";
    }

    @PostMapping("/recuperar")
    public String solicitar(@RequestParam String email, HttpServletRequest request, Model model) {

        usuarioService.solicitarRecuperacion(email).ifPresent(usuario -> {
            String enlace = urlBase(request) + "/restablecer?token=" + usuario.getResetToken();
            try {
                notificacionService.enviarRecuperacionContrasena(usuario, enlace);
            } catch (Exception e) {
                // Si el correo no esta configurado en este equipo, no se
                // interrumpe el flujo: igual se muestra el mensaje generico.
            }
        });

        model.addAttribute("enviado", true);
        return "recuperacion/solicitar";
    }

    @GetMapping("/restablecer")
    public String mostrarRestablecer(@RequestParam String token, Model model) {

        if (usuarioService.tokenValido(token).isEmpty()) {
            model.addAttribute("tokenInvalido", true);
            return "recuperacion/restablecer";
        }

        model.addAttribute("token", token);
        return "recuperacion/restablecer";
    }

    @PostMapping("/restablecer")
    public String restablecer(@RequestParam String token,
                              @RequestParam String password,
                              @RequestParam String passwordConfirmacion,
                              Model model,
                              RedirectAttributes flash) {

        if (usuarioService.tokenValido(token).isEmpty()) {
            model.addAttribute("tokenInvalido", true);
            return "recuperacion/restablecer";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres");
            return "recuperacion/restablecer";
        }

        if (!password.equals(passwordConfirmacion)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "recuperacion/restablecer";
        }

        usuarioService.restablecerPassword(token, password);

        flash.addFlashAttribute("exito", "Contraseña actualizada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    private String urlBase(HttpServletRequest request) {
        int puerto = request.getServerPort();
        String sufijoPuerto = (puerto == 80 || puerto == 443) ? "" : ":" + puerto;
        return request.getScheme() + "://" + request.getServerName() + sufijoPuerto;
    }
}