package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.EventoHistorial;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Mantenimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HistorialService {

    private final IncidenciaService incidenciaService;
    private final MantenimientoService mantenimientoService;

    public HistorialService(IncidenciaService incidenciaService,
                            MantenimientoService mantenimientoService) {
        this.incidenciaService = incidenciaService;
        this.mantenimientoService = mantenimientoService;
    }

    public List<EventoHistorial> obtenerHistorial(Long equipoId) {

        List<EventoHistorial> eventos = new ArrayList<>();

        for (Incidencia i : incidenciaService.listarPorEquipo(equipoId)) {
            eventos.add(convertir(i));
        }

        for (Mantenimiento m : mantenimientoService.listarPorEquipo(equipoId)) {
            eventos.add(convertir(m));
        }

        eventos.sort(Comparator.comparing(EventoHistorial::getFecha).reversed());

        return eventos;
    }

    private EventoHistorial convertir(Incidencia i) {

        String titulo = "Falla reportada: " + i.getCategoriaFalla().getNombre();

        String detalle = i.getDescripcion();
        if (i.getSolucion() != null && !i.getSolucion().isBlank()) {
            detalle = detalle + " — Solucion: " + i.getSolucion();
        }
        if (i.getHorasIndisponibilidad() != null) {
            detalle = detalle + " (" + i.getHorasIndisponibilidad() + " h fuera de servicio)";
        }

        return new EventoHistorial(
                i.getFechaReporte(),
                "INCIDENCIA",
                i.getPrioridad(),
                titulo,
                detalle,
                i.getReportadoPor().getNombreCompleto(),
                i.getEstado(),
                i.getId());
    }

    private EventoHistorial convertir(Mantenimiento m) {

        LocalDateTime fecha = (m.getFechaEjecucion() != null)
                ? m.getFechaEjecucion().atStartOfDay()
                : (m.getFechaProgramada() != null
                        ? m.getFechaProgramada().atStartOfDay()
                        : LocalDateTime.now());

        String titulo = "PREVENTIVO".equals(m.getTipo())
                ? "Mantenimiento preventivo"
                : "Mantenimiento correctivo";

        String detalle = (m.getActividadesRealizadas() != null
                && !m.getActividadesRealizadas().isBlank())
                ? m.getActividadesRealizadas()
                : "Sin actividades registradas";

        if (m.getResultado() != null && !m.getResultado().isBlank()) {
            detalle = detalle + " — Resultado: " + m.getResultado();
        }

        return new EventoHistorial(
                fecha,
                "MANTENIMIENTO",
                m.getTipo(),
                titulo,
                detalle,
                m.getTecnico().getNombreCompleto(),
                m.getEstado(),
                m.getId());
    }
}