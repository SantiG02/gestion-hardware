package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
import co.edu.uan.gestionhardware.service.ConfiguracionService;
import co.edu.uan.gestionhardware.service.IndicadorService;
import co.edu.uan.gestionhardware.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Envia por correo, de forma automatica, solo las alertas que son nuevas
 * desde la ultima revision (RF-17, RF-18) -- no por un horario fijo, sino
 * por evento: cuando un equipo cruza un umbral que antes no habia cruzado,
 * o aparece un mantenimiento proximo a vencer que antes no estaba en la
 * lista. Si nada cambio desde la ultima revision, no se manda nada.
 *
 * El boton "Enviar alertas por correo" del dashboard sigue funcionando
 * igual, independiente de este job: ese siempre manda TODAS las alertas
 * activas en ese momento, no solo las nuevas.
 *
 * Revisa cada 15 minutos. No es instantaneo (no se dispara en el mismo
 * segundo en que se registra una incidencia), pero evita tener que
 * enganchar el envio de correo dentro de cada controlador que puede crear
 * una alerta (incidencias, mantenimientos, reclasificacion de equipos).
 */
@Component
public class EnvioAutomaticoAlertas {

    private static final Logger log = LoggerFactory.getLogger(EnvioAutomaticoAlertas.class);

    private final ConfiguracionService configuracionService;
    private final IndicadorService indicadorService;
    private final NotificacionService notificacionService;

    public EnvioAutomaticoAlertas(ConfiguracionService configuracionService,
                                  IndicadorService indicadorService,
                                  NotificacionService notificacionService) {
        this.configuracionService = configuracionService;
        this.indicadorService = indicadorService;
        this.notificacionService = notificacionService;
    }

    @Scheduled(cron = "0 0 * * * *") // cada hora, en el minuto 0
    public void revisarYEnviar() {

        ConfiguracionSistema configuracion = configuracionService.obtener();
        ResumenDashboard resumen = indicadorService.construirResumen();

        Set<String> firmaAnterior = parsearFirma(configuracion.getFirmaUltimasAlertas());

        List<Alerta> alertasNuevas = new ArrayList<>();
        Set<String> firmaActual = new HashSet<>();

        for (Alerta alerta : resumen.getAlertas()) {
            String clave = clave(alerta);
            firmaActual.add(clave);
            if (!firmaAnterior.contains(clave)) {
                alertasNuevas.add(alerta);
            }
        }

        LocalDateTime momentoEnvio = null;

        if (!alertasNuevas.isEmpty()) {
            try {
                notificacionService.enviarAlertas(alertasNuevas);
                momentoEnvio = LocalDateTime.now();
                log.info("Envio automatico de alertas: {} alerta(s) nueva(s) enviada(s)", alertasNuevas.size());
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo de alertas nuevas: {}", e.getMessage());
            }
        }

        // La firma se actualiza siempre, haya o no correo, para que la
        // proxima revision compare contra el estado real mas reciente.
                // La firma se actualiza siempre, haya o no correo, para que la
        // proxima revision compare contra el estado real mas reciente.
        try {
            configuracionService.actualizarEstadoAlertas(construirFirma(firmaActual), momentoEnvio);
        } catch (Exception e) {
            log.warn("No se pudo guardar la firma de alertas de esta revision: {}", e.getMessage());
        }
    }

    private String clave(Alerta alerta) {
        return alerta.getTipo() + "::" + alerta.getEquipoCodigo();
    }

    private Set<String> parsearFirma(String firma) {
        Set<String> resultado = new HashSet<>();
        if (firma != null && !firma.isBlank()) {
            for (String linea : firma.split("\n")) {
                if (!linea.isBlank()) {
                    resultado.add(linea.trim());
                }
            }
        }
        return resultado;
    }

    private String construirFirma(Set<String> claves) {
        return String.join("\n", claves);
    }
}