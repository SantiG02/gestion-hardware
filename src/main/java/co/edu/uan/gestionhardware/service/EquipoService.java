package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final AreaRepository areaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final EstadoEquipoRepository estadoEquipoRepository;
    private final SedeRepository sedeRepository;

    public EquipoService(EquipoRepository equipoRepository,
                         AreaRepository areaRepository,
                         TipoEquipoRepository tipoEquipoRepository,
                         EstadoEquipoRepository estadoEquipoRepository,
                         SedeRepository sedeRepository) {
        this.equipoRepository = equipoRepository;
        this.areaRepository = areaRepository;
        this.tipoEquipoRepository = tipoEquipoRepository;
        this.estadoEquipoRepository = estadoEquipoRepository;
        this.sedeRepository = sedeRepository;
    }

    public List<Equipo> listarActivos() {
        return equipoRepository.findByActivoTrue();
    }

    /**
     * Filtro de RF-06: busqueda libre (codigo, serial, marca, modelo) mas
     * filtros por sede, area, estado y responsable. Cualquier parametro
     * nulo se ignora, asi que llamar con todos en null equivale a
     * listarActivos().
     */
    public List<Equipo> filtrar(String texto, Long sedeId, Long areaId,
                                Long estadoId, Long responsableId) {
        String textoNormalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
        return equipoRepository.filtrar(textoNormalizado, sedeId, areaId, estadoId, responsableId);
    }

    public Optional<Equipo> buscarPorId(Long id) {
        return equipoRepository.findConRelaciones(id);
    }

    // Listas para los desplegables del formulario y de los filtros
    public List<Sede> listarSedes() {
        return sedeRepository.findAll();
    }

    public List<Area> listarAreas() {
        return areaRepository.findAll();
    }

    public List<TipoEquipo> listarTipos() {
        return tipoEquipoRepository.findAll();
    }

    public List<EstadoEquipo> listarEstados() {
        return estadoEquipoRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional
    public Equipo guardar(Equipo equipo) {
        if (equipo.getActivo() == null) {
            equipo.setActivo(true);
        }
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