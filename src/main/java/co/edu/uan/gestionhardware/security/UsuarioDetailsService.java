package co.edu.uan.gestionhardware.security;

import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByEmailConRol(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));

        String authority = "ROLE_" + usuario.getRol().getNombre();

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .disabled(!usuario.getActivo())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .build();
    }
}