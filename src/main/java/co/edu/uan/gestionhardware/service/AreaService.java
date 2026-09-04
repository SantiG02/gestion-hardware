package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.repository.AreaRepository;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areaRepository;
    private final EquipoRepository equipoRepository;

    public AreaService(AreaRepository areaRepository, EquipoRepository equipoRepository) {
        this.areaRepository = areaRepository;
        this.equipoRepository = equipoRepository;
    }

    public List<Area> listarPorSede(Long sedeId) {
        return areaRepository.findConSedePorSede(sedeId);
    }

    public List<Area> listarTodas() {
        return areaRepository.findActivasConSede();
    }

    public Optional<Area> buscarPorId(Long id) {
        return areaRepository.findConSede(id);
    }

    public long contarEquiposActivos(Long areaId) {
        return equipoRepository.countByAreaIdAndActivoTrue(areaId);
    }

    public boolean existeNombreEnSede(Long sedeId, String nombre) {
        return areaRepository.existsBySedeIdAndNombreIgnoreCase(sedeId, nombre);
    }

    @Transactional
    public Area guardar(Area area) {
        if (area.getActivo() == null) {
            area.setActivo(true);
        }
        return areaRepository.save(area);
    }

    @Transactional
    public void desactivar(Long id) {
        areaRepository.findById(id).ifPresent(area -> {
            area.setActivo(false);
            areaRepository.save(area);
        });
    }

    @Transactional
    public void activar(Long id) {
        areaRepository.findById(id).ifPresent(area -> {
            area.setActivo(true);
            areaRepository.save(area);
        });
    }
}