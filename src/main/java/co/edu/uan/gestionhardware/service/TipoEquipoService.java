package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import co.edu.uan.gestionhardware.repository.TipoEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TipoEquipoService {

    private final TipoEquipoRepository tipoRepository;
    private final EquipoRepository equipoRepository;

    public TipoEquipoService(TipoEquipoRepository tipoRepository,
                             EquipoRepository equipoRepository) {
        this.tipoRepository = tipoRepository;
        this.equipoRepository = equipoRepository;
    }

    public List<TipoEquipo> listarTodos() {
        return tipoRepository.findAllByOrderByNombreAsc();
    }

    public List<TipoEquipo> listarActivos() {
        return tipoRepository.findByActivoTrueOrderByNombreAsc();
    }

    public Optional<TipoEquipo> buscarPorId(Long id) {
        return tipoRepository.findById(id);
    }

    public boolean existeNombreEnOtro(String nombre, Long idActual) {
        if (idActual == null) {
            return tipoRepository.existsByNombreIgnoreCase(nombre);
        }
        return tipoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idActual);
    }

    public long contarEquiposActivos(Long tipoId) {
        return equipoRepository.countByTipoEquipoIdAndActivoTrue(tipoId);
    }

    @Transactional
    public TipoEquipo guardar(TipoEquipo tipo) {
        if (tipo.getActivo() == null) {
            tipo.setActivo(true);
        }
        return tipoRepository.save(tipo);
    }

    @Transactional
    public void desactivar(Long id) {
        tipoRepository.findById(id).ifPresent(t -> {
            t.setActivo(false);
            tipoRepository.save(t);
        });
    }

    @Transactional
    public void activar(Long id) {
        tipoRepository.findById(id).ifPresent(t -> {
            t.setActivo(true);
            tipoRepository.save(t);
        });
    }
}