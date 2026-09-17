package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.dto.AreaResumen;
import co.edu.uan.gestionhardware.dto.IndicadorEquipo;
import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.dto.SegmentoDonut;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calcula los indicadores operativos por equipo (RF-14, RF-15) y clasifica
 * automaticamente cada equipo segun los umbrales definidos (RF-16). Tambien
 * arma el resumen agregado que consume el panel principal (RF-18): la
 * distribucion de estados para el donut, el desglose por area para las
 * barras apiladas, y el top de equipos con mas fallas.
 *
 * Los umbrales y el estado calculado (RF-03) ya no son constantes: se leen
 * en cada calculo desde ConfiguracionSistema, la fila unica de configuracion
 * administrada desde /configuracion.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private static final int DIAS_MANTENIMIENTO_PROXIMO = 7;
    private static final int TOP_FALLAS_LIMITE = 5;
    private static final double CX_DONUT = 60;
    private static final double CY_DONUT = 60;
    private static final double RADIO_DONUT = 54.0;

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

        // area -> [estable, seguimiento, renovacion]
        Map<String, long[]> conteoPorArea = new LinkedHashMap<>();
        Map<String, Long> conteoPorEstado = new LinkedHashMap<>();
        conteoPorEstado.put(ESTADO_ESTABLE, 0L);
        conteoPorEstado.put(ESTADO_SEGUIMIENTO, 0L);
        conteoPorEstado.put(ESTADO_RENOVACION, 0L);

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

            String estado = equipo.getEstadoEquipo().getNombre();
            conteoPorEstado.merge(estado, 1L, Long::sum);

            String area = equipo.getArea().getNombre();
            long[] conteo = conteoPorArea.computeIfAbsent(area, k -> new long[3]);
            if (ESTADO_ESTABLE.equalsIgnoreCase(estado)) conteo[0]++;
            else if (ESTADO_SEGUIMIENTO.equalsIgnoreCase(estado)) conteo[1]++;
            else conteo[2]++;
        }

        mantenimientoRepository.findProximosAVencer(LocalDate.now().plusDays(DIAS_MANTENIMIENTO_PROXIMO))
                .forEach(m -> alertas.add(new Alerta(
                        "Mantenimiento próximo a vencer",
                        "MEDIA",
                        m.getEquipo().getCodigoInterno(),
                        "Mantenimiento preventivo programado para el " + m.getFechaProgramada())));

        List<SegmentoDonut> segmentosEstado = construirSegmentosDonut(conteoPorEstado);
        List<AreaResumen> areasResumen = construirResumenPorArea(conteoPorArea);

        List<IndicadorEquipo> topFallas = indicadores.stream()
                .filter(i -> i.getFallasMes() > 0)
                .sorted(Comparator.comparingLong(IndicadorEquipo::getFallasMes).reversed())
                .limit(TOP_FALLAS_LIMITE)
                .toList();

        return new ResumenDashboard(
                equipos.size(),
                incidenciaRepository.countByEstadoNot("CERRADA"),
                mantenimientoRepository.countByEstadoNot("FINALIZADO"),
                equiposReclasificados,
                horasIndisponibilidadTotal,
                segmentosEstado,
                areasResumen,
                topFallas,
                indicadores,
                alertas);
    }

    private List<SegmentoDonut> construirSegmentosDonut(Map<String, Long> conteoPorEstado) {

        long total = conteoPorEstado.values().stream().mapToLong(Long::longValue).sum();

        String[] nombres = {ESTADO_ESTABLE, ESTADO_SEGUIMIENTO, ESTADO_RENOVACION};
        String[] colores = {"var(--estable)", "var(--seguimiento)", "var(--renovacion)"};

        List<SegmentoDonut> segmentos = new ArrayList<>();
        double anguloInicio = -90; // arranca arriba (12 en punto)

        for (int i = 0; i < nombres.length; i++) {
            long cantidad = conteoPorEstado.getOrDefault(nombres[i], 0L);
            double porcentaje = total == 0 ? 0 : (cantidad * 100.0) / total;
            double anguloBarrido = total == 0 ? 0 : (cantidad * 360.0) / total;

            String pathArco = construirArco(anguloInicio, anguloInicio + anguloBarrido, cantidad == total && total > 0);
            segmentos.add(new SegmentoDonut(nombres[i], cantidad, colores[i], porcentaje, pathArco));

            anguloInicio += anguloBarrido;
        }

        return segmentos;
    }

    /**
     * Arma el atributo "d" de un <path> SVG que dibuja un arco real entre dos
     * angulos (en grados, 0 = derecha, sentido horario). Si el segmento ocupa
     * el circulo completo (un solo estado con el 100%), un unico arco de 360
     * grados no se puede representar con el comando A, asi que se dibuja como
     * dos semicirculos.
     */
    private String construirArco(double anguloInicioGrados, double anguloFinGrados, boolean circuloCompleto) {

        if (circuloCompleto) {
            double xDerecha = CX_DONUT + RADIO_DONUT;
            double xIzquierda = CX_DONUT - RADIO_DONUT;
            return String.format(Locale.US,
                    "M %.3f %.3f A %.3f %.3f 0 1 1 %.3f %.3f A %.3f %.3f 0 1 1 %.3f %.3f",
                    xDerecha, CY_DONUT, RADIO_DONUT, RADIO_DONUT, xIzquierda, CY_DONUT,
                    RADIO_DONUT, RADIO_DONUT, xDerecha, CY_DONUT);
        }

        double inicioRad = Math.toRadians(anguloInicioGrados);
        double finRad = Math.toRadians(anguloFinGrados);

        double xInicio = CX_DONUT + RADIO_DONUT * Math.cos(inicioRad);
        double yInicio = CY_DONUT + RADIO_DONUT * Math.sin(inicioRad);
        double xFin = CX_DONUT + RADIO_DONUT * Math.cos(finRad);
        double yFin = CY_DONUT + RADIO_DONUT * Math.sin(finRad);

        int arcoLargo = (anguloFinGrados - anguloInicioGrados) > 180 ? 1 : 0;

        return String.format(Locale.US, "M %.3f %.3f A %.3f %.3f 0 %d 1 %.3f %.3f",
                xInicio, yInicio, RADIO_DONUT, RADIO_DONUT, arcoLargo, xFin, yFin);
    }

    private List<AreaResumen> construirResumenPorArea(Map<String, long[]> conteoPorArea) {

        List<AreaResumen> resumen = new ArrayList<>();

        for (Map.Entry<String, long[]> entrada : conteoPorArea.entrySet()) {

            long[] conteo = entrada.getValue();
            long total = conteo[0] + conteo[1] + conteo[2];

            double pctEstable = total == 0 ? 0 : (conteo[0] * 100.0) / total;
            double pctSeguimiento = total == 0 ? 0 : (conteo[1] * 100.0) / total;
            double pctRenovacion = total == 0 ? 0 : (conteo[2] * 100.0) / total;

            resumen.add(new AreaResumen(entrada.getKey(), total, conteo[0], conteo[1], conteo[2],
                    pctEstable, pctSeguimiento, pctRenovacion));
        }

        resumen.sort(Comparator.comparingLong(AreaResumen::getTotalEquipos).reversed());
        return resumen;
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