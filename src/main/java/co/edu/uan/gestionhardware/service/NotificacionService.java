package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envia por correo electronico el resumen de alertas activas (RF-18) a los
 * destinatarios configurados en ConfiguracionSistema (RF-04).
 *
 * El bean JavaMailSender solo existe si spring.mail.host esta configurado
 * (application-local.properties, que no se comparte por git). Por eso se
 * recibe como ObjectProvider en vez de inyectarlo directo: si algun
 * integrante del equipo no tiene el correo configurado en su maquina, la
 * aplicacion arranca igual, y el envio simplemente falla con un mensaje
 * claro en vez de tumbar el arranque de toda la app.
 *
 * El envio es manual, disparado desde el boton "Enviar por correo" del
 * dashboard. Todavia no hay un job programado que respete automaticamente
 * la frecuencia configurada (DIARIA/SEMANAL/MENSUAL); eso queda como una
 * extension futura con @Scheduled.
 */
@Service
public class NotificacionService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public NotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void enviarAlertas(List<Alerta> alertas, ConfiguracionSistema configuracion) {

        if (alertas.isEmpty()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new IllegalStateException(
                    "El correo no esta configurado en este equipo (falta spring.mail.* en application-local.properties)");
        }

        String[] destinatarios = configuracion.getDestinatariosNotificacion().split(",");
        for (int i = 0; i < destinatarios.length; i++) {
            destinatarios[i] = destinatarios[i].trim();
        }

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
        mensaje.setTo(destinatarios);
        mensaje.setSubject("HardTrack - " + alertas.size() + " alerta(s) activa(s)");
        mensaje.setText(cuerpo.toString());

        mailSender.send(mensaje);
    }
}