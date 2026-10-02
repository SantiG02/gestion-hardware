package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


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

    public List<Usuario> listarActivos() {
        return usuarioRepository.findActivosConRol();
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

        public boolean existeDocumento(String documento) {
        return usuarioRepository.existsByDocumento(documento);
    }
    
    public List<Rol> listaRols() {
        return rolRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findConRelaciones(id);
    }

    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmailConRol(email);
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

    // ============================================================
    // Cuenta de administrador inicial (requisito del tutor)
    // ============================================================

    /**
     * Indica si ya existe al menos un usuario con rol GESTOR. Mientras sea
     * false, la pantalla de /configuracion-inicial queda disponible; una
     * vez haya uno, esa puerta se cierra.
     */
    public boolean existeAlgunGestor() {
        return usuarioRepository.existsByRolNombre("GESTOR");
    }

    @Transactional
    public Usuario crearPrimerAdministrador(String nombreCompleto, String email, String passwordPlano) {

        Rol gestor = rolRepository.findByNombre("GESTOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol GESTOR"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setRol(gestor);
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    // ============================================================
    // Recuperacion de cuenta por correo (requisito del tutor)
    // ============================================================

    /**
     * Genera un token de recuperacion valido por 1 hora si el correo existe.
     * Si no existe, no hace nada (el controlador igual muestra el mismo
     * mensaje generico, para no revelar que correos estan registrados).
     */
    @Transactional
    public Optional<Usuario> solicitarRecuperacion(String email) {
        return usuarioRepository.findByEmailConRol(email).map(usuario -> {
            usuario.setResetToken(UUID.randomUUID().toString());
            usuario.setResetTokenExpira(LocalDateTime.now().plusHours(1));
            return usuarioRepository.save(usuario);
        });
    }

    public Optional<Usuario> tokenValido(String token) {
        return usuarioRepository.findByResetToken(token)
                .filter(u -> u.getResetTokenExpira() != null
                        && u.getResetTokenExpira().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public boolean restablecerPassword(String token, String passwordPlano) {
        return tokenValido(token).map(usuario -> {
            usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
            usuario.setResetToken(null);
            usuario.setResetTokenExpira(null);
            usuarioRepository.save(usuario);
            return true;
        }).orElse(false);
    }
}