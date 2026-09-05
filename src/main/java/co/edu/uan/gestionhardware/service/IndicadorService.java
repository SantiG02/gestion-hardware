package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.dto.IndicadorEquipo;
import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
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
 * Los umbrales y el estado calculado (RF-03) ya no son constantes: se leen
 * en cada calculo desde ConfiguracionSistema, la fila unica de configuracion
 * administrada desde /configuracion.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

    // No forma parte de RF-03 (esos umbrales son solo fallas, indisponibilidad,
    // antiguedad y actualizaciones), asi que queda fijo aqui.
    private static final int DIAS_MANTENIMIENTO_PROXIMO = 7;

    private static final String ESTADO_ESTABLE = "Estable";
    private static final String ESTADO_SEGUIMIENTO = "En seguimiento";
    private static final String ESTADO_RENOVACION = "Candidato a renovacion";

    private final EquipoRepository equipoRepository;
    private final EstadoEquipoRepository estadoEquipoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final ConfiguracionService configuracionService;

    public IndicadorService(EquipoRepository equipoRepository,
                            EstadoEquipoRepository estadoEquipoRepository,
                            IncidenciaRepository incidenciaRepository,
                            MantenimientoRepository mantenimientoRepository,
                            ConfiguracionService configuracionService) {
        this.equipoRepository = equipoRepository;
        this.estadoEquipoRepository = estadoEquipoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.configuracionService = configuracionService;
    }

    /**
     * Calcula los indicadores de todos los equipos activos y, de paso,
     * reclasifica en base de datos los que no coinciden con el estado
     * que les corresponde segun los umbrales (RF-16).
     */
    @Transactional
    public ResumenDashboard construirResumen() {

        ConfiguracionSistema configuracion = configuracionService.obtener();

        List<Equipo> equipos = equipoRepository.findByActivoTrue();
        List<IndicadorEquipo> indicadores = new ArrayList<>();

        long equiposReclasificados = 0;
        BigDecimal horasIndisponibilidadTotal = BigDecimal.ZERO;

        Map<String, Long> equiposPorEstado = new LinkedHashMap<>();
        Map<String, Long> equiposPorArea = new LinkedHashMap<>();
        List<Alerta> alertas = new ArrayList<>();

        for (Equipo equipo : equipos) {

            IndicadorEquipo indicador = calcularIndicadores(equipo, configuracion);

            if (indicador.isRequiereReclasificacion()) {
                equiposReclasificados++;
                reclasificar(equipo, indicador.getEstadoSugerido());
            }

            indicadores.add(indicador);
            horasIndisponibilidadTotal = horasIndisponibilidadTotal.add(indicador.getHorasIndisponibilidad());
            alertas.addAll(generarAlertasEquipo(indicador, configuracion));

            equiposPorEstado.merge(equipo.getEstadoEquipo().getNombre(), 1L, Long::sum);
            equiposPorArea.merge(equipo.getArea().getNombre(), 1L, Long::sum);
        }

        mantenimientoRepository.findProximosAVencer(LocalDate.now().plusDays(DIAS_MANTENIMIENTO_PROXIMO))
                .forEach(m -> alertas.add(new Alerta(
                        "Mantenimiento próximo a vencer",
                        "MEDIA",
                        m.getEquipo().getCodigoInterno(),
                        "Mantenimiento preventivo programado para el " + m.getFechaProgramada())));

        return new ResumenDashboard(
                equipos.size(),
                incidenciaRepository.countByEstadoNot("CERRADA"),
                mantenimientoRepository.countByEstadoNot("FINALIZADO"),
                equiposReclasificados,
                horasIndisponibilidadTotal,
                equiposPorEstado,
                equiposPorArea,
                indicadores,
                alertas);
    }

    private IndicadorEquipo calcularIndicadores(Equipo equipo, ConfiguracionSistema configuracion) {

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
                        .toTotalMonths() < configuracion.getUmbralMesesActualizacion();

        // RF-16: clasificacion automatica segun umbrales
        String estadoSugerido = clasificar(fallasMes, horasIndisponibilidad, antiguedadAnios, configuracion);
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
    private String clasificar(long fallasMes, BigDecimal horasIndisponibilidad,
                              int antiguedadAnios, ConfiguracionSistema configuracion) {

        boolean superaIndisponibilidad = horasIndisponibilidad
                .compareTo(configuracion.getUmbralHorasIndisponibilidad()) >= 0;
        boolean superaAntiguedad = antiguedadAnios >= configuracion.getUmbralAntiguedadAnios();

        if (superaIndisponibilidad || superaAntiguedad) {
            return ESTADO_RENOVACION;
        }

        if (fallasMes >= configuracion.getUmbralFallasMes()) {
            return ESTADO_SEGUIMIENTO;
        }

        return ESTADO_ESTABLE;
    }

    /**
     * Genera las alertas de RF-17 que aplican a un equipo: fallas recurrentes,
     * indisponibilidad prolongada y antiguedad critica. La de mantenimiento
     * proximo a vencer se arma aparte en construirResumen, porque no depende
     * de un equipo puntual sino de la tabla de mantenimientos programados.
     */
    private List<Alerta> generarAlertasEquipo(IndicadorEquipo indicador, ConfiguracionSistema configuracion) {

        List<Alerta> alertas = new ArrayList<>();
        String codigo = indicador.getEquipo().getCodigoInterno();

        if (indicador.getFallasMes() >= configuracion.getUmbralFallasMes()) {
            alertas.add(new Alerta("Fallas recurrentes", "MEDIA", codigo,
                    indicador.getFallasMes() + " fallas reportadas en los últimos 30 días"));
        }

        if (indicador.getHorasIndisponibilidad().compareTo(configuracion.getUmbralHorasIndisponibilidad()) >= 0) {
            alertas.add(new Alerta("Indisponibilidad prolongada", "ALTA", codigo,
                    indicador.getHorasIndisponibilidad() + " horas acumuladas fuera de servicio"));
        }

        if (indicador.getAntiguedadAnios() >= configuracion.getUmbralAntiguedadAnios()) {
            alertas.add(new Alerta("Antigüedad crítica", "ALTA", codigo,
                    indicador.getAntiguedadAnios() + " años de antigüedad"));
        }

        return alertas;
    }

    private void reclasificar(Equipo equipo, String nombreEstado) {
        estadoEquipoRepository.findByNombre(nombreEstado).ifPresent(estado -> {
            equipo.setEstadoEquipo(estado);
            equipoRepository.save(equipo);
        });
    }
}