package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.repository.TipoEquipoRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TipoEquipoConverter implements Converter<String, TipoEquipo> {

    private final TipoEquipoRepository tipoEquipoRepository;

    public TipoEquipoConverter(TipoEquipoRepository tipoEquipoRepository) {
        this.tipoEquipoRepository = tipoEquipoRepository;
    }

    @Override
    public TipoEquipo convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return tipoEquipoRepository.findById(Long.valueOf(source)).orElse(null);
    }
}