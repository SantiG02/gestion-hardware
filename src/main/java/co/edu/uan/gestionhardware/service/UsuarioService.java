package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;


@Service
@Transactional(readOnly = true)

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository){
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listarActivos() {
        return usuarioRepository.findActivosConRol();
    }

    public List<Rol> listaRols() {
        return rolRepository.findAll();
    }
}
