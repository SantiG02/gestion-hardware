package co.edu.uan.gestionhardware.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos agregados que alimentan el panel principal: totales generales,
 * la distribucion de equipos por estado (para el donut) y por area (para
 * las barras apiladas), el top de equipos con mas fallas, las alertas
 * activas y el detalle de indicadores por equipo (RF-14, RF-15, RF-16).
 */
public class ResumenDashboard {

    private final long totalEquipos;
    private final long incidenciasAbiertas;
    private final long mantenimientosPendientes;
    private final long equiposReclasificados;
    private final BigDecimal horasIndisponibilidadTotal;
    private final List<SegmentoDonut> segmentosEstado;
    private final List<AreaResumen> areasResumen;
    private final List<IndicadorEquipo> topFallas;
    private final List<IndicadorEquipo> indicadoresPorEquipo;
    private final List<Alerta> alertas;

    public ResumenDashboard(long totalEquipos, long incidenciasAbiertas,
                            long mantenimientosPendientes, long equiposReclasificados,
                            BigDecimal horasIndisponibilidadTotal,
                            List<SegmentoDonut> segmentosEstado,
                            List<AreaResumen> areasResumen,
                            List<IndicadorEquipo> topFallas,
                            List<IndicadorEquipo> indicadoresPorEquipo,
                            List<Alerta> alertas) {
        this.totalEquipos = totalEquipos;
        this.incidenciasAbiertas = incidenciasAbiertas;
        this.mantenimientosPendientes = mantenimientosPendientes;
        this.equiposReclasificados = equiposReclasificados;
        this.horasIndisponibilidadTotal = horasIndisponibilidadTotal;
        this.segmentosEstado = segmentosEstado;
        this.areasResumen = areasResumen;
        this.topFallas = topFallas;
        this.indicadoresPorEquipo = indicadoresPorEquipo;
        this.alertas = alertas;
    }

    public long getTotalEquipos() { return totalEquipos; }
    public long getIncidenciasAbiertas() { return incidenciasAbiertas; }
    public long getMantenimientosPendientes() { return mantenimientosPendientes; }
    public long getEquiposReclasificados() { return equiposReclasificados; }
    public BigDecimal getHorasIndisponibilidadTotal() { return horasIndisponibilidadTotal; }
    public List<SegmentoDonut> getSegmentosEstado() { return segmentosEstado; }
    public List<AreaResumen> getAreasResumen() { return areasResumen; }
    public List<IndicadorEquipo> getTopFallas() { return topFallas; }
    public List<IndicadorEquipo> getIndicadoresPorEquipo() { return indicadoresPorEquipo; }
    public List<Alerta> getAlertas() { return alertas; }

    public long getCandidatosARenovacion() {
        return segmentosEstado.stream()
                .filter(s -> s.getNombre().equalsIgnoreCase("Candidato a renovacion"))
                .mapToLong(SegmentoDonut::getCantidad)
                .findFirst().orElse(0);
    }
}