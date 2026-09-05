package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.IndicadorEquipo;
import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import co.edu.uan.gestionhardware.repository.EstadoEquipoRepository;
import co.edu.uan.gestionhardware.repository.IncidenciaRepository;
import co.edu.uan.gestionhardware.repository.MantenimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula los indicadores operativos por equipo (RF-14, RF-15) y clasifica
 * automaticamente cada equipo segun los umbrales definidos (RF-16). Tambien
 * arma el resumen agregado que consume el panel principal (RF-18).
 *
 * Los umbrales quedan como constantes porque el modulo de parametrizacion
 * (RF-03) todavia no esta implementado en el sistema. Cuando exista esa
 * pantalla de configuracion, estos valores deben salir de ahi en vez de
 * estar fijos en el codigo.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private static final long UMBRAL_FALLAS_MES = 3;
    private static final BigDecimal UMBRAL_HORAS_INDISPONIBILIDAD = BigDecimal.valueOf(120);
    private static final int UMBRAL_ANTIGUEDAD_ANIOS = 5;
    private static final int UMBRAL_MESES_ACTUALIZACION = 12;

    private static final String ESTADO_ESTABLE = "Estable";
    private static final String ESTADO_SEGUIMIENTO = "En seguimiento";
    private static final String ESTADO_RENOVACION = "Candidato a renovacion";

    private final EquipoRepository equipoRepository;
    private final EstadoEquipoRepository estadoEquipoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    public IndicadorService(EquipoRepository equipoRepository,
                            EstadoEquipoRepository estadoEquipoRepository,
                            IncidenciaRepository incidenciaRepository,
                            MantenimientoRepository mantenimientoRepository) {
        this.equipoRepository = equipoRepository;
        this.estadoEquipoRepository = estadoEquipoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    /**
     * Calcula los indicadores de todos los equipos activos y, de paso,
     * reclasifica en base de datos los que no coinciden con el estado
     * que les corresponde segun los umbrales (RF-16).
     */
    @Transactional
    public ResumenDashboard construirResumen() {

        List<Equipo> equipos = equipoRepository.findByActivoTrue();
        List<IndicadorEquipo> indicadores = new ArrayList<>();

        long equiposReclasificados = 0;
        BigDecimal horasIndisponibilidadTotal = BigDecimal.ZERO;

        Map<String, Long> equiposPorEstado = new LinkedHashMap<>();
        Map<String, Long> equiposPorArea = new LinkedHashMap<>();

        for (Equipo equipo : equipos) {

            IndicadorEquipo indicador = calcularIndicadores(equipo);

            if (indicador.isRequiereReclasificacion()) {
                equiposReclasificados++;
                reclasificar(equipo, indicador.getEstadoSugerido());
            }

            indicadores.add(indicador);
            horasIndisponibilidadTotal = horasIndisponibilidadTotal.add(indicador.getHorasIndisponibilidad());

            equiposPorEstado.merge(equipo.getEstadoEquipo().getNombre(), 1L, Long::sum);
            equiposPorArea.merge(equipo.getArea().getNombre(), 1L, Long::sum);
        }

        return new ResumenDashboard(
                equipos.size(),
                incidenciaRepository.countByEstadoNot("CERRADA"),
                mantenimientoRepository.countByEstadoNot("FINALIZADO"),
                equiposReclasificados,
                horasIndisponibilidadTotal,
                equiposPorEstado,
                equiposPorArea,
                indicadores);
    }

    private IndicadorEquipo calcularIndicadores(Equipo equipo) {

        LocalDateTime ahora = LocalDateTime.now();

        // RF-14: fallas por periodo y reportes tecnicos acumulados
        long fallasSemana = incidenciaRepository.countByEquipoIdAndFechaReporteBetween(
                equipo.getId(), ahora.minusDays(7), ahora);

        long fallasMes = incidenciaRepository.countByEquipoIdAndFechaReporteBetween(
                equipo.getId(), ahora.minusDays(30), ahora);

        long reportesAcumulados = incidenciaRepository.countByEquipoId(equipo.getId());

        // RF-15: horas de indisponibilidad, antiguedad y cumplimiento de actualizaciones
        BigDecimal horasIndisponibilidad = incidenciaRepository
                .sumHorasIndisponibilidadPorEquipo(equipo.getId());

        int antiguedadAnios = Period.between(equipo.getFechaCompra(), LocalDate.now()).getYears();

        boolean actualizacionAlDia = equipo.getFechaUltimaActualizacionSo() != null
                && Period.between(equipo.getFechaUltimaActualizacionSo(), LocalDate.now())
                        .toTotalMonths() < UMBRAL_MESES_ACTUALIZACION;

        // RF-16: clasificacion automatica segun umbrales
        String estadoSugerido = clasificar(fallasMes, horasIndisponibilidad, antiguedadAnios);
        boolean requiereReclasificacion = !estadoSugerido.equalsIgnoreCase(equipo.getEstadoEquipo().getNombre());

        return new IndicadorEquipo(equipo, fallasSemana, fallasMes, reportesAcumulados,
                horasIndisponibilidad, antiguedadAnios, actualizacionAlDia,
                estadoSugerido, requiereReclasificacion);
    }

    /**
     * Aplica los umbrales de RF-16. El orden importa: primero se evaluan las
     * condiciones mas graves (candidato a renovacion), luego seguimiento, y
     * si ninguna aplica el equipo se considera estable.
     */
    private String clasificar(long fallasMes, BigDecimal horasIndisponibilidad, int antiguedadAnios) {

        boolean superaIndisponibilidad = horasIndisponibilidad.compareTo(UMBRAL_HORAS_INDISPONIBILIDAD) >= 0;
        boolean superaAntiguedad = antiguedadAnios >= UMBRAL_ANTIGUEDAD_ANIOS;

        if (superaIndisponibilidad || superaAntiguedad) {
            return ESTADO_RENOVACION;
        }

        if (fallasMes >= UMBRAL_FALLAS_MES) {
            return ESTADO_SEGUIMIENTO;
        }

        return ESTADO_ESTABLE;
    }

    private void reclasificar(Equipo equipo, String nombreEstado) {
        estadoEquipoRepository.findByNombre(nombreEstado).ifPresent(estado -> {
            equipo.setEstadoEquipo(estado);
            equipoRepository.save(equipo);
        });
    }
}