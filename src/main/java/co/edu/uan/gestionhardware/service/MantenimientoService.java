package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.Mantenimiento;
import co.edu.uan.gestionhardware.repository.MantenimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
    }

    public List<Mantenimiento> listarTodos() {
        return mantenimientoRepository.findTodosConRelaciones();
    }

    public List<Mantenimiento> listarPendientes() {
        return mantenimientoRepository.findPendientesConRelaciones();
    }

    public List<Mantenimiento> listarPorEquipo(Long equipoId) {
        return mantenimientoRepository.findPorEquipo(equipoId);
    }

    public Optional<Mantenimiento> buscarPorId(Long id) {
        return mantenimientoRepository.findConRelaciones(id);
    }

    public List<Mantenimiento> listarProximosAVencer(int dias) {
        return mantenimientoRepository.findProximosAVencer(LocalDate.now().plusDays(dias));
    }

    public long contarPorTipoEnPeriodo(String tipo, LocalDate desde, LocalDate hasta) {
        return mantenimientoRepository.countByTipoAndFechaEjecucionBetween(tipo, desde, hasta);
    }

    @Transactional
    public Mantenimiento guardar(Mantenimiento mantenimiento) {

        if (mantenimiento.getEstado() == null || mantenimiento.getEstado().isBlank()) {
            mantenimiento.setEstado("PROGRAMADO");
        }

        // Un preventivo nunca lleva incidencia asociada
        if ("PREVENTIVO".equals(mantenimiento.getTipo())) {
            mantenimiento.setIncidencia(null);
        }

        // Si se registra una ejecucion, el mantenimiento pasa a finalizado
        if (mantenimiento.getFechaEjecucion() != null
                && "PROGRAMADO".equals(mantenimiento.getEstado())) {
            mantenimiento.setEstado("FINALIZADO");
        }

        return mantenimientoRepository.save(mantenimiento);
    }

    @Transactional
    public Optional<Mantenimiento> finalizar(Long id,
                                             LocalDate fechaEjecucion,
                                             String actividades,
                                             String resultado,
                                             String observaciones) {

        return mantenimientoRepository.findById(id).map(mantenimiento -> {
            mantenimiento.setFechaEjecucion(
                    fechaEjecucion != null ? fechaEjecucion : LocalDate.now());
            mantenimiento.setActividadesRealizadas(actividades);
            mantenimiento.setResultado(resultado);
            mantenimiento.setObservaciones(observaciones);
            mantenimiento.setEstado("FINALIZADO");
            return mantenimientoRepository.save(mantenimiento);
        });
    }
}