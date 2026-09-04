package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.CategoriaFalla;
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

    public List<Incidencia> listarAbiertas() {
        return incidenciaRepository.findAbiertasConRelaciones();
    }

    public List<Incidencia> listarPorEquipo(Long equipoId) {
        return incidenciaRepository.findPorEquipo(equipoId);
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

            if (Boolean.TRUE.equals(incidencia.getGeneraIndisponibilidad())
                    && incidencia.getFechaInicioIndisponibilidad() != null) {

                incidencia.setFechaFinIndisponibilidad(cierre);
                incidencia.setHorasIndisponibilidad(
                        calcularHoras(incidencia.getFechaInicioIndisponibilidad(), cierre));
            }

            return incidenciaRepository.save(incidencia);
        });
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