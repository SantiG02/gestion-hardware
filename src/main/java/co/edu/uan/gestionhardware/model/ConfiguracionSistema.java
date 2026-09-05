package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Configuracion parametrizable del sistema (RF-03, RF-04). Es una tabla de
 * una sola fila: siempre se trabaja sobre el registro con id = 1.
 *
 * Los destinatarios de las notificaciones ya no se configuran aqui como
 * texto libre: se calculan dinamicamente a partir de los usuarios activos
 * con rol GESTOR o TECNICO (ver NotificacionService).
 */
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    @Id
    private Long id;

    @NotNull(message = "Debe indicar el umbral de fallas mensuales")
    @Min(value = 1, message = "Debe ser mayor a cero")
    @Column(name = "umbral_fallas_mes", nullable = false)
    private Integer umbralFallasMes;

    @NotNull(message = "Debe indicar el umbral de horas de indisponibilidad")
    @DecimalMin(value = "0.0", message = "No puede ser negativo")
    @Column(name = "umbral_horas_indisponibilidad", nullable = false, precision = 8, scale = 2)
    private BigDecimal umbralHorasIndisponibilidad;

    @NotNull(message = "Debe indicar el umbral de antiguedad")
    @Min(value = 1, message = "Debe ser mayor a cero")
    @Column(name = "umbral_antiguedad_anios", nullable = false)
    private Integer umbralAntiguedadAnios;

    @NotNull(message = "Debe indicar el plazo de actualizacion")
    @Min(value = 1, message = "Debe ser mayor a cero")
    @Column(name = "umbral_meses_actualizacion", nullable = false)
    private Integer umbralMesesActualizacion;

    @NotBlank(message = "Debe seleccionar la frecuencia de notificacion")
    @Column(name = "frecuencia_notificacion", nullable = false, length = 20)
    private String frecuenciaNotificacion;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUmbralFallasMes() { return umbralFallasMes; }
    public void setUmbralFallasMes(Integer umbralFallasMes) { this.umbralFallasMes = umbralFallasMes; }

    public BigDecimal getUmbralHorasIndisponibilidad() { return umbralHorasIndisponibilidad; }
    public void setUmbralHorasIndisponibilidad(BigDecimal v) { this.umbralHorasIndisponibilidad = v; }

    public Integer getUmbralAntiguedadAnios() { return umbralAntiguedadAnios; }
    public void setUmbralAntiguedadAnios(Integer umbralAntiguedadAnios) { this.umbralAntiguedadAnios = umbralAntiguedadAnios; }

    public Integer getUmbralMesesActualizacion() { return umbralMesesActualizacion; }
    public void setUmbralMesesActualizacion(Integer v) { this.umbralMesesActualizacion = v; }

    public String getFrecuenciaNotificacion() { return frecuenciaNotificacion; }
    public void setFrecuenciaNotificacion(String frecuenciaNotificacion) { this.frecuenciaNotificacion = frecuenciaNotificacion; }
}