package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.service.LimitadorSolicitudes;
import co.edu.uan.gestionhardware.service.NotificacionService;
import co.edu.uan.gestionhardware.service.RegistroPendienteService;
import co.edu.uan.gestionhardware.service.RegistroPendienteService.Registro;
import co.edu.uan.gestionhardware.service.RegistroPendienteService.VerificacionException;
import co.edu.uan.gestionhardware.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

/**
 * Creacion de cuenta desde la pestaña "Crear cuenta" del login. Cualquier
 * persona puede crearla una vez existe un administrador: se crea siempre con
 * rol USUARIO (Usuario Final) y solo el Gestor puede cambiarle el rol despues
 * desde /usuarios.
 *
 * Si el correo del sistema esta configurado, el correo se confirma primero con
 * un codigo de 6 digitos y la cuenta solo se crea al acertarlo. Si no esta
 * configurado (por ejemplo en el equipo de un jurado sin la clave de Brevo),
 * la cuenta se crea directamente para no dejar el sistema inutilizable.
 */
@Controller
public class SolicitudCuentaController {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final RegistroPendienteService registros;
    private final LimitadorSolicitudes limitador;

    public SolicitudCuentaController(UsuarioService usuarioService,
                                     NotificacionService notificacionService,
                                     RegistroPendienteService registros,
                                     LimitadorSolicitudes limitador) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
        this.registros = registros;
        this.limitador = limitador;
    }

    // ------------------------------------------------------------------
    // Paso 1: datos de la cuenta
    // ------------------------------------------------------------------
    @PostMapping("/solicitar-cuenta")
    public String solicitar(@RequestParam String nombreCompleto,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam String passwordConfirmacion,
                            @RequestParam(required = false) String aceptaTerminos,
                            @RequestParam(required = false) String sitioWeb,
                            HttpServletRequest request,
                            Model model,
                            RedirectAttributes flash) {

        // Sin administrador, el formulario crea al primero (otro controlador).
        if (!usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }

        // Campo señuelo oculto: solo lo llena un bot.
        if (sitioWeb != null && !sitioWeb.isBlank()) {
            flash.addFlashAttribute("exito", mensajeCuentaCreada());
            return "redirect:/login";
        }

        String nombre = nombreCompleto == null ? "" : nombreCompleto.trim();
        String correo = email == null ? "" : email.trim().toLowerCase();
        String ip = request.getRemoteAddr();

        String error = validar(nombre, correo, password, passwordConfirmacion);

        if (error == null && aceptaTerminos == null) {
            error = "Debes aceptar los Términos y Condiciones para crear la cuenta";
        }

        if (error == null && !limitador.permitir(ip)) {
            error = "Has intentado crear demasiadas cuentas desde este equipo. Inténtalo de nuevo más tarde.";
        }

        if (error == null && usuarioService.existeEmail(correo)) {
            error = "Ya existe una cuenta con este correo. Inicia sesión o usa «¿Olvidó su contraseña?».";
        }

        if (error != null) {
            return formularioConError(model, error, nombre, correo);
        }

        // Sin correo configurado no se puede enviar el codigo: se crea directo.
        if (!notificacionService.correoConfigurado()) {
            usuarioService.registrarSolicitud(nombre, correo, password, ip);
            flash.addFlashAttribute("exito", mensajeCuentaCreada());
            return "redirect:/login";
        }

        String codigo;
        try {
            codigo = registros.iniciar(nombre, correo, usuarioService.codificarPassword(password), ip);
        } catch (IllegalStateException e) {
            return formularioConError(model, e.getMessage(), nombre, correo);
        }

        try {
            notificacionService.enviarCodigoVerificacion(nombre, correo, codigo);
        } catch (Exception e) {
            registros.descartar(correo);
            return formularioConError(model,
                    "No se pudo enviar el código a ese correo. Revisa que esté bien escrito e inténtalo de nuevo.",
                    nombre, correo);
        }

        return vistaVerificacion(model, correo, null,
                "Te enviamos un código de 6 dígitos a " + correo + ". Vence en 15 minutos.");
    }

    // ------------------------------------------------------------------
    // Paso 2: codigo recibido por correo
    // ------------------------------------------------------------------
    @PostMapping("/verificar-cuenta")
    public String verificar(@RequestParam String email,
                            @RequestParam String codigo,
                            Model model,
                            RedirectAttributes flash) {

        if (!usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }

        String correo = email == null ? "" : email.trim().toLowerCase();

        try {
            Registro registro = registros.verificar(correo, codigo);

            boolean creada = usuarioService.crearCuentaVerificada(
                    registro.getNombre(), registro.getEmail(), registro.getPasswordHash(),
                    registro.getIp(), registro.getAceptadoEn()).isPresent();

            if (!creada) {
                flash.addFlashAttribute("exito", "Ese correo ya tiene una cuenta. Inicia sesión.");
                return "redirect:/login";
            }

            flash.addFlashAttribute("exito", "Correo confirmado. " + mensajeCuentaCreada());
            return "redirect:/login";

        } catch (VerificacionException e) {
            if (e.isReiniciar()) {
                return formularioConError(model, e.getMessage(), "", correo);
            }
            return vistaVerificacion(model, correo, e.getMessage(), null);
        }
    }

    @PostMapping("/reenviar-codigo")
    public String reenviar(@RequestParam String email,
                           HttpServletRequest request,
                           Model model) {

        if (!usuarioService.existeAlgunGestor()) {
            return "redirect:/login";
        }

        String correo = email == null ? "" : email.trim().toLowerCase();

        if (!limitador.permitir(request.getRemoteAddr())) {
            return vistaVerificacion(model, correo,
                    "Has pedido demasiados códigos desde este equipo. Inténtalo más tarde.", null);
        }

        try {
            String codigo = registros.reenviar(correo);
            // El nombre no es necesario para reenviar: se saluda de forma general.
            notificacionService.enviarCodigoVerificacion("", correo, codigo);
            return vistaVerificacion(model, correo, null, "Enviamos un código nuevo a " + correo + ".");

        } catch (VerificacionException e) {
            if (e.isReiniciar()) {
                return formularioConError(model, e.getMessage(), "", correo);
            }
            return vistaVerificacion(model, correo, e.getMessage(), null);

        } catch (Exception e) {
            return vistaVerificacion(model, correo,
                    "No se pudo enviar el código. Inténtalo de nuevo en un momento.", null);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    private String formularioConError(Model model, String error, String nombre, String correo) {
        model.addAttribute("sinGestor", false);
        model.addAttribute("tabCrear", true);
        model.addAttribute("errorCrear", error);
        model.addAttribute("valorNombre", nombre);
        model.addAttribute("valorEmail", correo);
        return "login";
    }

    private String vistaVerificacion(Model model, String correo, String error, String info) {
        model.addAttribute("sinGestor", false);
        model.addAttribute("tabCrear", true);
        model.addAttribute("verificar", true);
        model.addAttribute("valorEmail", correo);
        model.addAttribute("errorCrear", error);
        model.addAttribute("infoCrear", info);
        return "login";
    }

    private String validar(String nombre, String correo, String password, String confirmacion) {

        if (nombre.isEmpty() || correo.isEmpty() || password == null || password.isEmpty()) {
            return "Todos los campos son obligatorios";
        }
        if (nombre.length() > 150) {
            return "El nombre es demasiado largo";
        }
        if (correo.length() > 120 || !CORREO.matcher(correo).matches()) {
            return "Ingresa un correo electrónico válido";
        }
        if (password.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres";
        }
        if (!password.equals(confirmacion)) {
            return "Las contraseñas no coinciden";
        }
        return null;
    }

    private String mensajeCuentaCreada() {
        return "Cuenta creada correctamente. Ya puedes iniciar sesión con tu correo y contraseña.";
    }
}