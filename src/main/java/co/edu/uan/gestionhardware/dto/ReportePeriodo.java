package co.edu.uan.gestionhardware.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reporte periodico completo (RF-19): fallas, indisponibilidad, mantenimientos
 * y clasificacion final de activos, para el rango de fechas consultado.
 */
public class ReportePeriodo {

    private final String etiquetaPeriodo;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final long totalFallas;
    private final BigDecimal totalHorasIndisponibilidad;
    private final long totalMantenimientos;
    private final Map<String, Long> equiposPorEstado;
    private final List<ReporteEquipo> equipos;

    public ReportePeriodo(String etiquetaPeriodo, LocalDate desde, LocalDate hasta,
                          long totalFallas, BigDecimal totalHorasIndisponibilidad,
                          long totalMantenimientos, Map<String, Long> equiposPorEstado,
                          List<ReporteEquipo> equipos) {
        this.etiquetaPeriodo = etiquetaPeriodo;
        this.desde = desde;
        this.hasta = hasta;
        this.totalFallas = totalFallas;
        this.totalHorasIndisponibilidad = totalHorasIndisponibilidad;
        this.totalMantenimientos = totalMantenimientos;
        this.equiposPorEstado = equiposPorEstado;
        this.equipos = equipos;
    }

    public String getEtiquetaPeriodo() { return etiquetaPeriodo; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public long getTotalFallas() { return totalFallas; }
    public BigDecimal getTotalHorasIndisponibilidad() { return totalHorasIndisponibilidad; }
    public long getTotalMantenimientos() { return totalMantenimientos; }
    public Map<String, Long> getEquiposPorEstado() { return equiposPorEstado; }
    public List<ReporteEquipo> getEquipos() { return equipos; }
}