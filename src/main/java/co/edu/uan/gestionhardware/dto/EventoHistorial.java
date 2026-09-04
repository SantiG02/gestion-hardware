package co.edu.uan.gestionhardware.dto;

import java.time.LocalDateTime;

/**
 * Representa un evento del historial de un equipo.
 * No corresponde a ninguna tabla: es una vista unificada de
 * incidencias y mantenimientos para presentarlos en una sola
 * linea de tiempo.
 */
public class EventoHistorial {

    private final LocalDateTime fecha;
    private final String tipo;
    private final String subtipo;
    private final String titulo;
    private final String detalle;
    private final String responsable;
    private final String estado;
    private final Long referenciaId;

    public EventoHistorial(LocalDateTime fecha, String tipo, String subtipo,
                           String titulo, String detalle, String responsable,
                           String estado, Long referenciaId) {
        this.fecha = fecha;
        this.tipo = tipo;
        this.subtipo = subtipo;
        this.titulo = titulo;
        this.detalle = detalle;
        this.responsable = responsable;
        this.estado = estado;
        this.referenciaId = referenciaId;
    }

    public LocalDateTime getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public String getSubtipo() { return subtipo; }
    public String getTitulo() { return titulo; }
    public String getDetalle() { return detalle; }
    public String getResponsable() { return responsable; }
    public String getEstado() { return estado; }
    public Long getReferenciaId() { return referenciaId; }
}