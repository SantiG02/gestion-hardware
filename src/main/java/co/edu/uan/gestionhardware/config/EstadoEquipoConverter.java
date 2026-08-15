package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.EstadoEquipo;
import co.edu.uan.gestionhardware.repository.EstadoEquipoRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EstadoEquipoConverter implements Converter<String, EstadoEquipo> {

    private final EstadoEquipoRepository estadoEquipoRepository;

    public EstadoEquipoConverter(EstadoEquipoRepository estadoEquipoRepository) {
        this.estadoEquipoRepository = estadoEquipoRepository;
    }

    @Override
    public EstadoEquipo convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return estadoEquipoRepository.findById(Long.valueOf(source)).orElse(null);
    }
}