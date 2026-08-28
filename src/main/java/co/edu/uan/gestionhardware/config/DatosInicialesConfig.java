package co.edu.uan.gestionhardware.config;

import co.edu.uan.gestionhardware.model.Rol;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.repository.RolRepository;
import co.edu.uan.gestionhardware.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatosInicialesConfig {

    @Bean
    public CommandLineRunner cargarDatosIniciales(RolRepository rolRepository,
                                                  UsuarioRepository usuarioRepository,
                                                  PasswordEncoder passwordEncoder) {
        return args -> {

            Rol gestor = rolRepository.findByNombre("GESTOR").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombre("GESTOR");
                r.setDescripcion("Gestor Tecnologico. Acceso integral al sistema.");
                return rolRepository.save(r);
            });

            Rol tecnico = rolRepository.findByNombre("TECNICO").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombre("TECNICO");
                r.setDescripcion("Tecnico de Soporte. Registra mantenimientos e incidencias.");
                return rolRepository.save(r);
            });

            crearSiNoExiste(usuarioRepository, passwordEncoder,
                    "admin@gestionhardware.co", "Gestor Tecnologico", gestor, "Admin1234");

            crearSiNoExiste(usuarioRepository, passwordEncoder,
                    "tecnico@gestionhardware.co", "Tecnico de Soporte", tecnico, "Tecnico1234");
        };
    }

    private void crearSiNoExiste(UsuarioRepository repo, PasswordEncoder encoder,
                                 String email, String nombre, Rol rol, String password) {
        if (!repo.existsByEmail(email)) {
            Usuario u = new Usuario();
            u.setNombreCompleto(nombre);
            u.setEmail(email);
            u.setPasswordHash(encoder.encode(password));
            u.setRol(rol);
            u.setActivo(true);
            repo.save(u);
        }
    }
}