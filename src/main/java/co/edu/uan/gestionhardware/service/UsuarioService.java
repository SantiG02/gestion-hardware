package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;


@Service
@Transactional(readOnly = true)

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder){
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void desactivar(Long id) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public void activar(Long id) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        });
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllConRol();
    }

    public List<Rol> listaRols() {
        return rolRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findConRelaciones(id);
    }

    @Transactional
    public Usuario guardar(Usuario usuario, String passwordPlano) {

        boolean vieneContrasena = passwordPlano != null && !passwordPlano.isBlank();

        if (vieneContrasena) {
            usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        }

        if (!vieneContrasena && usuario.getId() != null) {
            usuarioRepository.findById(usuario.getId())
                    .ifPresent(original -> usuario.setPasswordHash(original.getPasswordHash()));
        }

        return usuarioRepository.save(usuario);
    }
}
