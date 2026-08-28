package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.Rol;
import co.edu.uan.gestionhardware.repository.RolRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RolConverter implements Converter<String, Rol> {

    private final RolRepository rolRepository;

    public RolConverter(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public Rol convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return rolRepository.findById(Long.valueOf(source)).orElse(null);
    }
}