package co.edu.uan.gestionhardware.dto;

import java.math.BigDecimal;

/**
 * Fila del reporte periodico (RF-19): resumen de un equipo dentro del
 * periodo consultado (fallas, horas de indisponibilidad, mantenimientos),
 * mas su clasificacion final (estado actual al momento de generar el reporte).
 */
public class ReporteEquipo {

    private final String codigoInterno;
    private final String area;
    private final long fallas;
    private final BigDecimal horasIndisponibilidad;
    private final long mantenimientos;
    private final String estadoActual;

    public ReporteEquipo(String codigoInterno, String area, long fallas,
                         BigDecimal horasIndisponibilidad, long mantenimientos, String estadoActual) {
        this.codigoInterno = codigoInterno;
        this.area = area;
        this.fallas = fallas;
        this.horasIndisponibilidad = horasIndisponibilidad;
        this.mantenimientos = mantenimientos;
        this.estadoActual = estadoActual;
    }

    public String getCodigoInterno() { return codigoInterno; }
    public String getArea() { return area; }
    public long getFallas() { return fallas; }
    public BigDecimal getHorasIndisponibilidad() { return horasIndisponibilidad; }
    public long getMantenimientos() { return mantenimientos; }
    public String getEstadoActual() { return estadoActual; }
}