package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EquipoService {

    private final EquipoRepository equipoRepository;

    public EquipoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public List<Equipo> listarActivos() {
        return equipoRepository.findByActivoTrue();
    }

    public Optional<Equipo> buscarPorId(Long id) {
        return equipoRepository.findById(id);
    }

    @Transactional
    public Equipo guardar(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    @Transactional
    public void desactivar(Long id) {
        equipoRepository.findById(id).ifPresent(equipo -> {
            equipo.setActivo(false);
            equipoRepository.save(equipo);
        });
    }
}