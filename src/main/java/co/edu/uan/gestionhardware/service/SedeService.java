package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;


@Service
@Transactional(readOnly = true)

public class SedeService {

    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;

    public SedeService(SedeRepository sedeRepository, AreaRepository areaRepository){
        
        this.sedeRepository = sedeRepository;
        this.areaRepository = areaRepository;

    }
    
    @Transactional
    public void desactivar(Long id) {
        sedeRepository.findById(id).ifPresent(sede -> {
            sede.setActivo(false);
            sedeRepository.save(sede);

            areaRepository.findBySedeId(id).forEach(area -> {
                area.setActivo(false);
                areaRepository.save(area);
            });
        });
    }

    public long contarAreasActivas(Long sedeId) {
        return areaRepository.countBySedeIdAndActivoTrue(sedeId);
    }

    @Transactional
    public void activar(Long id) {
        sedeRepository.findById(id).ifPresent(sede -> {
            sede.setActivo(true);
            sedeRepository.save(sede);
        });
    }

    public List<Sede> listarTodos() {
        return sedeRepository.findAll();
    }


    public Optional<Sede> buscarPorId(Long id) {
        return sedeRepository.findById(id);
    }

    @Transactional
    public Sede guardar(Sede sede) {

        if (sede.getActivo() == null) { 
            sede.setActivo(true);
        }
        return sedeRepository.save(sede);
    }
}
