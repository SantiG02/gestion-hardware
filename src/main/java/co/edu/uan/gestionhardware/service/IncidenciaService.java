package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.CategoriaFalla;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.repository.CategoriaFallaRepository;
import co.edu.uan.gestionhardware.repository.IncidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final CategoriaFallaRepository categoriaFallaRepository;

    public IncidenciaService(IncidenciaRepository incidenciaRepository,
                             CategoriaFallaRepository categoriaFallaRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.categoriaFallaRepository = categoriaFallaRepository;
    }

    public List<Incidencia> listarTodas() {
        return incidenciaRepository.findTodasConRelaciones();
    }

        public List<Incidencia> filtrar(String texto, String estado, String prioridad, Long categoriaId) {
        String textoNormalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
        String estadoNormalizado = (estado == null || estado.isBlank()) ? null : estado;
        String prioridadNormalizada = (prioridad == null || prioridad.isBlank()) ? null : prioridad;
        return incidenciaRepository.filtrar(textoNormalizado, estadoNormalizado, prioridadNormalizada, categoriaId);
    }

    public List<Incidencia> listarAbiertas() {
        return incidenciaRepository.findAbiertasConRelaciones();
    }

    public List<Incidencia> listarPorEquipo(Long equipoId) {
        return incidenciaRepository.findPorEquipo(equipoId);
    }

    public List<Incidencia> listarPorUsuario(Long usuarioId) {
        return incidenciaRepository.findPorUsuarioReportador(usuarioId);
    }

    public Optional<Incidencia> buscarPorId(Long id) {
        return incidenciaRepository.findConRelaciones(id);
    }

    public List<CategoriaFalla> listarCategorias() {
        return categoriaFallaRepository.findAllByOrderByNombreAsc();
    }

    public boolean tieneIncidenciaAbierta(Long equipoId) {
        return incidenciaRepository.existsByEquipoIdAndEstadoNot(equipoId, "CERRADA");
    }

    @Transactional
    public Incidencia guardar(Incidencia incidencia) {

        if (incidencia.getEstado() == null || incidencia.getEstado().isBlank()) {
            incidencia.setEstado("ABIERTA");
        }

        if (Boolean.FALSE.equals(incidencia.getGeneraIndisponibilidad())) {
            incidencia.setFechaInicioIndisponibilidad(null);
            incidencia.setFechaFinIndisponibilidad(null);
            incidencia.setHorasIndisponibilidad(null);
        }

        return incidenciaRepository.save(incidencia);
    }

    @Transactional
    public Optional<Incidencia> cerrar(Long id, String solucion, LocalDateTime fechaFin) {

        return incidenciaRepository.findById(id).map(incidencia -> {

            LocalDateTime cierre = (fechaFin != null) ? fechaFin : LocalDateTime.now();

            incidencia.setEstado("CERRADA");
            incidencia.setFechaCierre(cierre);
            incidencia.setSolucion(solucion);
            // Cada vez que se cierra (incluso si ya se habia rechazado antes),
            // queda pendiente de que el Usuario Final confirme.
            incidencia.setConfirmacionUsuario("PENDIENTE");

            if (Boolean.TRUE.equals(incidencia.getGeneraIndisponibilidad())
                    && incidencia.getFechaInicioIndisponibilidad() != null) {

                incidencia.setFechaFinIndisponibilidad(cierre);
                incidencia.setHorasIndisponibilidad(
                        calcularHoras(incidencia.getFechaInicioIndisponibilidad(), cierre));
            }

            return incidenciaRepository.save(incidencia);
        });
    }

    /**
     * El Usuario Final confirma que la solucion si funciono. Solo aplica si
     * la incidencia le pertenece y esta cerrada, pendiente de confirmacion.
     */
    @Transactional
    public boolean confirmarSolucion(Long id, Long usuarioId) {
        return incidenciaRepository.findById(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuarioId))
                .filter(i -> "CERRADA".equals(i.getEstado()) && "PENDIENTE".equals(i.getConfirmacionUsuario()))
                .map(i -> {
                    i.setConfirmacionUsuario("APROBADA");
                    incidenciaRepository.save(i);
                    return true;
                }).orElse(false);
    }

    /**
     * El Usuario Final indica que la solucion no funciono: la incidencia
     * vuelve a quedar ABIERTA, con su comentario explicando que sigue mal.
     * Si generaba indisponibilidad, se limpia la fecha de fin y las horas
     * calculadas, porque el equipo sigue fuera de servicio.
     */
    @Transactional
    public boolean rechazarSolucion(Long id, Long usuarioId, String comentario) {
        return incidenciaRepository.findById(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuarioId))
                .filter(i -> "CERRADA".equals(i.getEstado()) && "PENDIENTE".equals(i.getConfirmacionUsuario()))
                .map(i -> {
                    i.setConfirmacionUsuario("RECHAZADA");
                    i.setComentarioUsuario(comentario);
                    i.setVecesRechazada(i.getVecesRechazada() + 1);
                    i.setEstado("ABIERTA");
                    i.setFechaCierre(null);
                    if (Boolean.TRUE.equals(i.getGeneraIndisponibilidad())) {
                        i.setFechaFinIndisponibilidad(null);
                        i.setHorasIndisponibilidad(null);
                    }
                    incidenciaRepository.save(i);
                    return true;
                }).orElse(false);
    }

    /**
     * El Usuario Final edita su propio reporte, solo mientras siga ABIERTA
     * (antes de que el tecnico la haya cerrado). Solo toca los campos que
     * el aparecen en su formulario reducido.
     */
    @Transactional
    public boolean actualizarPropia(Long id, Long usuarioId, Equipo equipo, CategoriaFalla categoriaFalla,
                                    String prioridad, String descripcion) {
        return incidenciaRepository.findById(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuarioId))
                .filter(i -> "ABIERTA".equals(i.getEstado()))
                .map(i -> {
                    i.setEquipo(equipo);
                    i.setCategoriaFalla(categoriaFalla);
                    i.setPrioridad(prioridad);
                    i.setDescripcion(descripcion);
                    incidenciaRepository.save(i);
                    return true;
                }).orElse(false);
    }

    /**
     * El Usuario Final elimina su propio reporte, solo mientras siga
     * ABIERTA.
     */
    @Transactional
    public boolean eliminarPropia(Long id, Long usuarioId) {
        return incidenciaRepository.findById(id)
                .filter(i -> i.getReportadoPor().getId().equals(usuarioId))
                .filter(i -> "ABIERTA".equals(i.getEstado()))
                .map(i -> {
                    incidenciaRepository.delete(i);
                    return true;
                }).orElse(false);
    }

    private BigDecimal calcularHoras(LocalDateTime inicio, LocalDateTime fin) {

        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            return BigDecimal.ZERO;
        }

        long minutos = Duration.between(inicio, fin).toMinutes();

        return BigDecimal.valueOf(minutos)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}