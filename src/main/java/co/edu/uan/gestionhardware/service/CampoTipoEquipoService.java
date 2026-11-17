package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.CampoTipoEquipo;
import co.edu.uan.gestionhardware.repository.CampoTipoEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CampoTipoEquipoService {

    private final CampoTipoEquipoRepository campoRepository;

    public CampoTipoEquipoService(CampoTipoEquipoRepository campoRepository) {
        this.campoRepository = campoRepository;
    }

    public List<CampoTipoEquipo> listarPorTipo(Long tipoId) {
        return campoRepository.findByTipoEquipoIdOrderByIdAsc(tipoId);
    }

    /**
     * Campos activos de los tipos activos, agrupados por id de tipo.
     * El formulario de equipos los dibuja todos y muestra solo el grupo
     * del tipo elegido.
     */
    public Map<Long, List<CampoTipoEquipo>> listarActivosAgrupadosPorTipo() {
        Map<Long, List<CampoTipoEquipo>> mapa = new LinkedHashMap<>();
        for (CampoTipoEquipo campo : campoRepository.findActivosDeTiposActivos()) {
            mapa.computeIfAbsent(campo.getTipoEquipo().getId(), k -> new ArrayList<>()).add(campo);
        }
        return mapa;
    }

    public Optional<CampoTipoEquipo> buscarPorId(Long id) {
        return campoRepository.findById(id);
    }

    public boolean existeNombreEnOtro(Long tipoId, String nombre, Long idActual) {
        if (idActual == null) {
            return campoRepository.existsByTipoEquipoIdAndNombreIgnoreCase(tipoId, nombre);
        }
        return campoRepository.existsByTipoEquipoIdAndNombreIgnoreCaseAndIdNot(tipoId, nombre, idActual);
    }

    @Transactional
    public CampoTipoEquipo guardar(CampoTipoEquipo campo) {
        if (campo.getActivo() == null) {
            campo.setActivo(true);
        }
        if (campo.getObligatorio() == null) {
            campo.setObligatorio(false);
        }
        return campoRepository.save(campo);
    }

    @Transactional
    public void desactivar(Long id) {
        campoRepository.findById(id).ifPresent(c -> {
            c.setActivo(false);
            campoRepository.save(c);
        });
    }

    @Transactional
    public void activar(Long id) {
        campoRepository.findById(id).ifPresent(c -> {
            c.setActivo(true);
            campoRepository.save(c);
        });
    }
}
