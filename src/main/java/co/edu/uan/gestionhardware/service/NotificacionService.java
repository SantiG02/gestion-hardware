package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.model.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envia los correos de HardTrack en HTML: la confirmacion de cuenta cuando
 * el Gestor registra un usuario nuevo, y el resumen de alertas activas
 * (RF-17, RF-18).
 *
 * Los destinatarios de las alertas se calculan en cada envio a partir de
 * los usuarios activos con rol GESTOR o TECNICO (tabla usuario).
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

    @Value("${hardtrack.mail.remitente}")
    private String remitente;

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

        String contenido =
                "<p style=\"margin:0 0 16px; font-size:14px; line-height:1.6; color:#334155;\">"
                + "Se creó tu cuenta en <strong>HardTrack</strong> con el rol "
                + "<strong>" + usuario.getRol().getNombre() + "</strong>.</p>"
                + "<p style=\"margin:0; font-size:14px; line-height:1.6; color:#334155;\">"
                + "Puedes ingresar con tu correo (<strong>" + usuario.getEmail() + "</strong>) y la "
                + "contraseña que te asignó el Gestor Tecnológico.</p>";

        String cuerpo = plantillaBase("Tu cuenta fue creada",
                "Hola " + usuario.getNombreCompleto() + ",", contenido);

        enviarHtml(mailSender, new String[]{usuario.getEmail()}, "HardTrack - Tu cuenta fue creada", cuerpo);
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

        StringBuilder listaAlertas = new StringBuilder();

        for (Alerta alerta : alertas) {

            boolean alta = "ALTA".equalsIgnoreCase(alerta.getSeveridad());
            String colorBorde = alta ? "#dc2626" : "#d97706";
            String colorFondo = alta ? "#fef2f2" : "#fffbeb";

            listaAlertas.append("<div style=\"border-left:4px solid ").append(colorBorde)
                    .append("; background:").append(colorFondo)
                    .append("; border-radius:8px; padding:12px 16px; margin-bottom:10px;\">")
                    .append("<div style=\"font-size:13px; font-weight:700; color:#0f172a;\">")
                    .append(alerta.getTipo());

            if (alerta.getEquipoCodigo() != null) {
                listaAlertas.append(" &middot; ").append(alerta.getEquipoCodigo());
            }

            listaAlertas.append("</div>")
                    .append("<div style=\"font-size:13px; color:#475569; margin-top:4px;\">")
                    .append(alerta.getMensaje())
                    .append("</div></div>");
        }

        String cuerpo = plantillaBase(alertas.size() + " alerta(s) activa(s)",
                "Se detectaron " + alertas.size() + " alerta(s) en HardTrack:", listaAlertas.toString());

        enviarHtml(mailSender, destinatarios.toArray(String[]::new),
                "HardTrack - " + alertas.size() + " alerta(s) activa(s)", cuerpo);
    }

    /**
     * Envoltorio HTML compartido por los dos correos: encabezado azul con el
     * nombre de HardTrack, un titulo, una introduccion, y el contenido propio
     * de cada correo (ya armado en HTML) en medio.
     */
    private String plantillaBase(String titulo, String introduccion, String contenidoHtml) {
        return "<div style=\"font-family:'Segoe UI', Arial, sans-serif; max-width:520px; margin:0 auto;\">"
                + "<div style=\"background:#2563eb; padding:20px 24px; border-radius:12px 12px 0 0;\">"
                + "<div style=\"color:#ffffff; font-size:18px; font-weight:700;\">HardTrack</div>"
                + "<div style=\"color:#dbeafe; font-size:12.5px;\">Ciclo de vida del hardware</div>"
                + "</div>"
                + "<div style=\"border:1px solid #e2e8f0; border-top:none; border-radius:0 0 12px 12px; padding:24px;\">"
                + "<h2 style=\"margin:0 0 12px; font-size:16px; color:#0f172a;\">" + titulo + "</h2>"
                + "<p style=\"margin:0 0 16px; font-size:14px; color:#334155;\">" + introduccion + "</p>"
                + contenidoHtml
                + "<p style=\"margin:24px 0 0; font-size:12px; color:#94a3b8;\">"
                + "HardTrack &middot; Sistema de gestión de hardware</p>"
                + "</div>"
                + "</div>";
    }

    private void enviarHtml(JavaMailSender mailSender, String[] destinatarios, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatarios);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo armar o enviar el correo: " + e.getMessage(), e);
        }
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