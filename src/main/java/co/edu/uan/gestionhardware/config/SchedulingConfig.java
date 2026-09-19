package co.edu.uan.gestionhardware.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas de Spring (@Scheduled), usadas por
 * EnvioAutomaticoAlertas para enviar las alertas por correo segun la
 * frecuencia configurada en RF-04, sin depender del boton manual.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}