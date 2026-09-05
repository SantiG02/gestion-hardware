package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
import co.edu.uan.gestionhardware.repository.ConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administra la configuracion parametrizable del sistema (RF-03, RF-04).
 * Es una tabla de una sola fila (id = 1), sembrada con valores por defecto
 * desde DatosInicialesConfig al arrancar la aplicacion.
 */
@Service
@Transactional(readOnly = true)
public class ConfiguracionService {

    public static final Long ID_UNICO = 1L;

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    public ConfiguracionSistema obtener() {
        return configuracionRepository.findById(ID_UNICO)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro la configuracion inicial del sistema"));
    }

    @Transactional
    public ConfiguracionSistema guardar(ConfiguracionSistema configuracion) {
        configuracion.setId(ID_UNICO);
        return configuracionRepository.save(configuracion);
    }
}