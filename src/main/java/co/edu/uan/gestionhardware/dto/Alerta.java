package co.edu.uan.gestionhardware.dto;

/**
 * Representa una alerta operativa (RF-17): fallas recurrentes, indisponibilidad
 * prolongada, antiguedad critica o mantenimiento preventivo proximo a vencer.
 * No corresponde a ninguna tabla; se genera en memoria a partir de los
 * indicadores calculados y de los mantenimientos programados.
 */
public class Alerta {

    private final String tipo;
    private final String severidad;
    private final String equipoCodigo;
    private final String mensaje;

    public Alerta(String tipo, String severidad, String equipoCodigo, String mensaje) {
        this.tipo = tipo;
        this.severidad = severidad;
        this.equipoCodigo = equipoCodigo;
        this.mensaje = mensaje;
    }

    public String getTipo() { return tipo; }
    public String getSeveridad() { return severidad; }
    public String getEquipoCodigo() { return equipoCodigo; }
    public String getMensaje() { return mensaje; }
}