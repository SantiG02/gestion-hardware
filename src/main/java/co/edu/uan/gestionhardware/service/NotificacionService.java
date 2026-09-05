package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.model.Usuario;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envia los correos de HardTrack: la confirmacion de cuenta cuando el Gestor
 * registra un usuario nuevo, y el resumen de alertas activas (RF-17, RF-18).
 *
 * Los destinatarios de las alertas ya no son un texto configurado a mano:
 * se calculan en cada envio a partir de los usuarios activos con rol GESTOR
 * o TECNICO (tabla usuario), asi que basta con que alguien este registrado
 * y activo en el sistema para empezar a recibir las notificaciones.
 *
 * El bean JavaMailSender solo existe si spring.mail.host esta configurado
 * (application-local.properties, que no se comparte por git). Por eso se
 * recibe como ObjectProvider: si el correo no esta configurado en esta
 * maquina, la aplicacion arranca igual y el envio falla con un mensaje
 * claro en vez de tumbar el arranque de toda la app.
 */
@Service
public class NotificacionService {

    private static final List<String> ROLES_NOTIFICADOS = List.of("GESTOR", "TECNICO");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final UsuarioService usuarioService;

    public NotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               UsuarioService usuarioService) {
        this.mailSenderProvider = mailSenderProvider;
        this.usuarioService = usuarioService;
    }

    /**
     * Envia el correo de bienvenida cuando el Gestor Tecnologico crea una
     * cuenta nueva. No incluye la contrasena: el Gestor se la comunica al
     * usuario por su cuenta.
     */
    public void enviarConfirmacionCuenta(Usuario usuario) {

        JavaMailSender mailSender = obtenerMailSenderObligatorio();

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("HardTrack - Tu cuenta fue creada");
        mensaje.setText(
                "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                "Se creo tu cuenta en HardTrack con el rol " + usuario.getRol().getNombre() + ".\n" +
                "Puedes ingresar con tu correo (" + usuario.getEmail() + ") y la contrasena " +
                "que te asigno el Gestor Tecnologico.\n\n" +
                "HardTrack - Sistema de gestion de hardware");

        mailSender.send(mensaje);
    }

    /**
     * Envia el resumen de alertas activas (RF-17) a todos los Gestores
     * Tecnologicos y Tecnicos de Soporte activos (RF-18).
     */
    public void enviarAlertas(List<Alerta> alertas) {

        if (alertas.isEmpty()) {
            return;
        }

        List<String> destinatarios = usuarioService.listarActivos().stream()
                .filter(u -> ROLES_NOTIFICADOS.contains(u.getRol().getNombre()))
                .map(Usuario::getEmail)
                .toList();

        if (destinatarios.isEmpty()) {
            throw new IllegalStateException(
                    "No hay usuarios Gestor o Tecnico activos a quienes notificar");
        }

        JavaMailSender mailSender = obtenerMailSenderObligatorio();

        StringBuilder cuerpo = new StringBuilder(
                "Se detectaron " + alertas.size() + " alerta(s) en HardTrack:\n\n");

        for (Alerta alerta : alertas) {
            cuerpo.append("- [").append(alerta.getTipo()).append("] ");
            if (alerta.getEquipoCodigo() != null) {
                cuerpo.append(alerta.getEquipoCodigo()).append(": ");
            }
            cuerpo.append(alerta.getMensaje()).append("\n");
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatarios.toArray(String[]::new));
        mensaje.setSubject("HardTrack - " + alertas.size() + " alerta(s) activa(s)");
        mensaje.setText(cuerpo.toString());

        mailSender.send(mensaje);
    }

    private JavaMailSender obtenerMailSenderObligatorio() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException(
                    "El correo no esta configurado en este equipo (falta spring.mail.* en application-local.properties)");
        }
        return mailSender;
    }
}