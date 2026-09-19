package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuracion parametrizable del sistema (RF-03, RF-04). Es una tabla de
 * una sola fila: siempre se trabaja sobre el registro con id = 1.
 *
 * Los destinatarios de las notificaciones ya no se configuran aqui como
 * texto libre: se calculan dinamicamente a partir de los usuarios activos
 * con rol GESTOR o TECNICO (ver NotificacionService).
 *
 * El envio automatico de alertas (ver EnvioAutomaticoAlertas) es por evento,
 * no por la frecuencia configurada abajo: se dispara cuando aparece una
 * alerta que no estaba activa en la revision anterior. El campo
 * frecuenciaNotificacion queda guardado mientras se decide si se retira del
 * formulario o se le da otro uso.
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

    /**
     * Cuando fue la ultima vez que el envio automatico realmente mando un
     * correo (no cada revision, solo cuando hubo algo nuevo). Es solo
     * informativa; no la edita el usuario.
     */
    @Column(name = "ultimo_envio_automatico")
    private LocalDateTime ultimoEnvioAutomatico;

    /**
     * Firma de las alertas activas en la ultima revision (una linea por
     * alerta, formato "tipo::codigoEquipo"), usada para detectar cuales son
     * nuevas en la siguiente revision. Se actualiza en cada revision, haya
     * o no correo de por medio. No la edita el usuario.
     */
    @Lob
    @Column(name = "firma_ultimas_alertas")
    private String firmaUltimasAlertas;

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

    public LocalDateTime getUltimoEnvioAutomatico() { return ultimoEnvioAutomatico; }
    public void setUltimoEnvioAutomatico(LocalDateTime ultimoEnvioAutomatico) { this.ultimoEnvioAutomatico = ultimoEnvioAutomatico; }

    public String getFirmaUltimasAlertas() { return firmaUltimasAlertas; }
    public void setFirmaUltimasAlertas(String firmaUltimasAlertas) { this.firmaUltimasAlertas = firmaUltimasAlertas; }
}