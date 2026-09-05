package co.edu.uan.gestionhardware.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Datos agregados que alimentan el panel principal: totales generales,
 * distribucion de equipos por estado y por area, y el detalle de
 * indicadores por equipo (RF-14, RF-15, RF-16) para la tabla del dashboard.
 */
public class ResumenDashboard {

    private final long totalEquipos;
    private final long incidenciasAbiertas;
    private final long mantenimientosPendientes;
    private final long equiposReclasificados;
    private final BigDecimal horasIndisponibilidadTotal;
    private final Map<String, Long> equiposPorEstado;
    private final Map<String, Long> equiposPorArea;
    private final List<IndicadorEquipo> indicadoresPorEquipo;
    private final List<Alerta> alertas;

    public ResumenDashboard(long totalEquipos, long incidenciasAbiertas,
                            long mantenimientosPendientes, long equiposReclasificados,
                            BigDecimal horasIndisponibilidadTotal,
                            Map<String, Long> equiposPorEstado,
                            Map<String, Long> equiposPorArea,
                            List<IndicadorEquipo> indicadoresPorEquipo,
                            List<Alerta> alertas) {
        this.totalEquipos = totalEquipos;
        this.incidenciasAbiertas = incidenciasAbiertas;
        this.mantenimientosPendientes = mantenimientosPendientes;
        this.equiposReclasificados = equiposReclasificados;
        this.horasIndisponibilidadTotal = horasIndisponibilidadTotal;
        this.equiposPorEstado = equiposPorEstado;
        this.equiposPorArea = equiposPorArea;
        this.indicadoresPorEquipo = indicadoresPorEquipo;
        this.alertas = alertas;
    }

    public long getTotalEquipos() { return totalEquipos; }
    public long getIncidenciasAbiertas() { return incidenciasAbiertas; }
    public long getMantenimientosPendientes() { return mantenimientosPendientes; }
    public long getEquiposReclasificados() { return equiposReclasificados; }
    public BigDecimal getHorasIndisponibilidadTotal() { return horasIndisponibilidadTotal; }
    public Map<String, Long> getEquiposPorEstado() { return equiposPorEstado; }
    public Map<String, Long> getEquiposPorArea() { return equiposPorArea; }
    public List<IndicadorEquipo> getIndicadoresPorEquipo() { return indicadoresPorEquipo; }
    public List<Alerta> getAlertas() { return alertas; }
}