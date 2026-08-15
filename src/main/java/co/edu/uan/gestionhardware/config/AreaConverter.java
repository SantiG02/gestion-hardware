package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.repository.AreaRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AreaConverter implements Converter<String, Area> {

    private final AreaRepository areaRepository;

    public AreaConverter(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public Area convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return areaRepository.findById(Long.valueOf(source)).orElse(null);
    }
}