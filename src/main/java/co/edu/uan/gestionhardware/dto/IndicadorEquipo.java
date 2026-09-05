package co.edu.uan.gestionhardware.dto;

import co.edu.uan.gestionhardware.model.Equipo;

import java.math.BigDecimal;

/**
 * Agrupa los indicadores operativos calculados para un equipo (RF-14, RF-15)
 * junto con el estado que le corresponde segun los umbrales definidos (RF-16).
 * No corresponde a ninguna tabla: se construye en memoria a partir del
 * inventario y del historial de incidencias de cada equipo.
 */
public class IndicadorEquipo {

    private final Equipo equipo;
    private final long fallasSemana;
    private final long fallasMes;
    private final long reportesAcumulados;
    private final BigDecimal horasIndisponibilidad;
    private final int antiguedadAnios;
    private final boolean actualizacionAlDia;
    private final String estadoSugerido;
    private final boolean requiereReclasificacion;

    public IndicadorEquipo(Equipo equipo, long fallasSemana, long fallasMes,
                           long reportesAcumulados, BigDecimal horasIndisponibilidad,
                           int antiguedadAnios, boolean actualizacionAlDia,
                           String estadoSugerido, boolean requiereReclasificacion) {
        this.equipo = equipo;
        this.fallasSemana = fallasSemana;
        this.fallasMes = fallasMes;
        this.reportesAcumulados = reportesAcumulados;
        this.horasIndisponibilidad = horasIndisponibilidad;
        this.antiguedadAnios = antiguedadAnios;
        this.actualizacionAlDia = actualizacionAlDia;
        this.estadoSugerido = estadoSugerido;
        this.requiereReclasificacion = requiereReclasificacion;
    }

    public Equipo getEquipo() { return equipo; }
    public long getFallasSemana() { return fallasSemana; }
    public long getFallasMes() { return fallasMes; }
    public long getReportesAcumulados() { return reportesAcumulados; }
    public BigDecimal getHorasIndisponibilidad() { return horasIndisponibilidad; }
    public int getAntiguedadAnios() { return antiguedadAnios; }
    public boolean isActualizacionAlDia() { return actualizacionAlDia; }
    public String getEstadoSugerido() { return estadoSugerido; }
    public boolean isRequiereReclasificacion() { return requiereReclasificacion; }
}