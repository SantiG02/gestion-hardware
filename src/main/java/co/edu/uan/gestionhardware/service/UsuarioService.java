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
            usuario.setPendienteAprobacion(false);
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

        // Al editar, el formulario solo trae algunos campos: lo demas
        // (contrasena, fecha de creacion, evidencia de terminos, token) se
        // conserva del registro original para no perderlo.
        if (usuario.getId() != null) {
            usuarioRepository.findById(usuario.getId()).ifPresent(original -> {
                if (!vieneContrasena) {
                    usuario.setPasswordHash(original.getPasswordHash());
                }
                usuario.setFechaCreacion(original.getFechaCreacion());
                usuario.setTerminosAceptadosEn(original.getTerminosAceptadosEn());
                usuario.setTerminosVersion(original.getTerminosVersion());
                usuario.setTerminosIp(original.getTerminosIp());
                usuario.setResetToken(original.getResetToken());
                usuario.setResetTokenExpira(original.getResetTokenExpira());
                usuario.setPendienteAprobacion(original.getPendienteAprobacion());
            });
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
    public Usuario crearPrimerAdministrador(String nombreCompleto, String email, String passwordPlano, String ip) {

        Rol gestor = rolRepository.findByNombre("GESTOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol GESTOR"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setRol(gestor);
        usuario.setActivo(true);
        registrarAceptacionTerminos(usuario, ip);

        return usuarioRepository.save(usuario);
    }

    // ============================================================
    // Registro de cuenta desde el login: cualquier persona puede crearla,
    // siempre con rol USUARIO (Usuario Final) y activa de inmediato. Solo
    // el Gestor puede cambiarle el rol despues, desde /usuarios.
    // ============================================================

    /** Version vigente de los Terminos y Condiciones (ver templates/terminos.html). */
    public static final String VERSION_TERMINOS = "1.0";

    /** Guarda la evidencia de aceptacion: fecha, version del documento e IP. */
    private void registrarAceptacionTerminos(Usuario usuario, String ip) {
        usuario.setTerminosAceptadosEn(LocalDateTime.now());
        usuario.setTerminosVersion(VERSION_TERMINOS);
        usuario.setTerminosIp(ip);
    }

    public List<Usuario> listarPendientes() {
        return usuarioRepository.findPendientesConRol();
    }

    public long contarPendientes() {
        return usuarioRepository.countByPendienteAprobacionTrue();
    }

    public String codificarPassword(String passwordPlano) {
        return passwordEncoder.encode(passwordPlano);
    }

    /** Crea la cuenta (rol USUARIO, activa) cuando no hace falta verificar el correo. */
    @Transactional
    public Optional<Usuario> registrarSolicitud(String nombreCompleto, String email, String passwordPlano, String ip) {
        return crearCuentaVerificada(nombreCompleto, email,
                passwordEncoder.encode(passwordPlano), ip, LocalDateTime.now());
    }

    /**
     * Crea la cuenta con la contrasena ya cifrada. Se usa cuando el correo ya
     * se confirmo con el codigo. Devuelve vacio si el correo ya existe.
     */
    @Transactional
    public Optional<Usuario> crearCuentaVerificada(String nombreCompleto, String email, String passwordHash,
                                                   String ip, LocalDateTime aceptadoEn) {

        if (usuarioRepository.existsByEmail(email)) {
            return Optional.empty();
        }

        Rol rolUsuario = rolRepository.findByNombre("USUARIO")
                .orElseThrow(() -> new IllegalStateException("No existe el rol USUARIO"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordHash);
        usuario.setRol(rolUsuario);
        usuario.setActivo(true);
        usuario.setPendienteAprobacion(false);
        usuario.setTerminosAceptadosEn(aceptadoEn);
        usuario.setTerminosVersion(VERSION_TERMINOS);
        usuario.setTerminosIp(ip);

        return Optional.of(usuarioRepository.save(usuario));
    }

    @Transactional
    public Optional<Usuario> aprobar(Long id, Long rolId) {
        return usuarioRepository.findConRelaciones(id)
                .filter(Usuario::getPendienteAprobacion)
                .map(usuario -> {
                    if (rolId != null) {
                        rolRepository.findById(rolId).ifPresent(usuario::setRol);
                    }
                    usuario.setActivo(true);
                    usuario.setPendienteAprobacion(false);
                    return usuarioRepository.save(usuario);
                });
    }

    @Transactional
    public Optional<Usuario> rechazar(Long id) {
        return usuarioRepository.findConRelaciones(id)
                .filter(Usuario::getPendienteAprobacion)
                .map(usuario -> {
                    usuarioRepository.delete(usuario);
                    return usuario;
                });
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