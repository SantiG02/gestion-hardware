package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envia los correos de HardTrack usando la API HTTP de Brevo (no SMTP).
 *
 * Se cambio de SMTP a la API HTTP porque en varias redes (antivirus con
 * inspeccion de correo, firewalls de universidades y algunos proveedores
 * de internet) el trafico SMTP por los puertos 465/587 queda interceptado
 * y el certificado TLS de Brevo no coincide, aunque las credenciales esten
 * bien. El trafico HTTPS normal (puerto 443), en cambio, practicamente
 * nunca se bloquea asi -- es el mismo puerto que usa cualquier pagina web.
 * Esto hace que el envio de correo sea confiable sin importar en que
 * computador o red se corra la aplicacion (importante para la
 * sustentacion, donde el jurado la va a correr en sus propios equipos).
 */
@Service
public class NotificacionService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final List<String> ROLES_NOTIFICADOS = List.of("GESTOR", "TECNICO");

    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${hardtrack.brevo.api-key:}")
    private String apiKey;

    @Value("${hardtrack.mail.remitente}")
    private String remitente;

    public NotificacionService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void enviarConfirmacionCuenta(Usuario usuario) {

        String contenido =
                "<p style=\"margin:0 0 16px; font-size:14px; line-height:1.6; color:#334155;\">"
                + "Se creó tu cuenta en <strong>HardTrack</strong> con el rol "
                + "<strong>" + usuario.getRol().getNombre() + "</strong>.</p>"
                + "<p style=\"margin:0; font-size:14px; line-height:1.6; color:#334155;\">"
                + "Puedes ingresar con tu correo (<strong>" + usuario.getEmail() + "</strong>) y la "
                + "contraseña que te asignó el Gestor Tecnológico.</p>";

        String cuerpo = plantillaBase("Tu cuenta fue creada",
                "Hola " + usuario.getNombreCompleto() + ",", contenido);

        enviarHtml(new String[]{usuario.getEmail()}, "HardTrack - Tu cuenta fue creada", cuerpo);
    }

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

        enviarHtml(destinatarios.toArray(String[]::new),
                "HardTrack - " + alertas.size() + " alerta(s) activa(s)", cuerpo);
    }

    /**
     * Correo de recuperacion de contrasena (requisito del tutor). El enlace
     * ya viene armado con el token desde el controlador.
     */
    public void enviarRecuperacionContrasena(Usuario usuario, String enlace) {

        String contenido =
                "<p style=\"margin:0 0 16px; font-size:14px; line-height:1.6; color:#334155;\">"
                + "Recibimos una solicitud para restablecer tu contraseña en <strong>HardTrack</strong>. "
                + "Si fuiste tú, haz clic en el siguiente botón:</p>"
                + "<p style=\"margin:0 0 20px;\">"
                + "<a href=\"" + enlace + "\" style=\"display:inline-block; padding:12px 20px; "
                + "background:#1F2937; color:#ffffff; text-decoration:none; border-radius:8px; "
                + "font-weight:600; font-size:14px;\">Restablecer contraseña</a></p>"
                + "<p style=\"margin:0; font-size:13px; color:#94a3b8;\">"
                + "Este enlace vence en 1 hora. Si no lo pediste tú, puedes ignorar este correo.</p>";

        String cuerpo = plantillaBase("Restablecer contraseña",
                "Hola " + usuario.getNombreCompleto() + ",", contenido);

        enviarHtml(new String[]{usuario.getEmail()}, "HardTrack - Restablecer contraseña", cuerpo);
    }

    /**
     * Envoltorio HTML compartido por los tres correos: encabezado azul con el
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

    private void enviarHtml(String[] destinatarios, String asunto, String cuerpoHtml) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "El correo no esta configurado en este equipo (falta hardtrack.brevo.api-key en application-local.properties)");
        }

        try {
            Map<String, Object> remitenteJson = new LinkedHashMap<>();
            remitenteJson.put("email", remitente);
            remitenteJson.put("name", "HardTrack");

            List<Map<String, String>> destinatariosJson = List.of(destinatarios).stream()
                    .map(correo -> Map.of("email", correo))
                    .toList();

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("sender", remitenteJson);
            cuerpo.put("to", destinatariosJson);
            cuerpo.put("subject", asunto);
            cuerpo.put("htmlContent", cuerpoHtml);

            String json = objectMapper.writeValueAsString(cuerpo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Brevo rechazo el envio (codigo " + response.statusCode() + "): " + response.body());
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }
}