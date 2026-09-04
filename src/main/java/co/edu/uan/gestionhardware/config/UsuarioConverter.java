package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UsuarioConverter implements Converter<String, Usuario> {

    private final UsuarioRepository usuarioRepository;

    public UsuarioConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return usuarioRepository.findById(Long.valueOf(source)).orElse(null);
    }
} 