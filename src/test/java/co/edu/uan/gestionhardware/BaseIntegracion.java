package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.model.*;
import co.edu.uan.gestionhardware.repository.*;
import co.edu.uan.gestionhardware.service.NotificacionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base de las pruebas de integración: levanta la aplicación completa (controladores,
 * servicios, repositorios, seguridad) sobre una base H2 en memoria. Cada prueba corre
 * dentro de una transacción que se revierte al terminar, así no se ensucian los datos.
 * El correo (Brevo) está simulado: nunca se envía nada real.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hardtrack_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "hardtrack.brevo.api-key="
})
@Transactional
public abstract class BaseIntegracion {

    @Autowired protected WebApplicationContext contexto;
    @PersistenceContext protected EntityManager em;

    @MockitoBean protected NotificacionService notificacion;

    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected SedeRepository sedeRepository;
    @Autowired protected AreaRepository areaRepository;
    @Autowired protected EquipoRepository equipoRepository;
    @Autowired protected EstadoEquipoRepository estadoEquipoRepository;
    @Autowired protected CategoriaFallaRepository categoriaFallaRepository;
    @Autowired protected IncidenciaRepository incidenciaRepository;
    @Autowired protected PasswordEncoder encoder;

    protected MockMvc mvc;
    protected Usuario gestor;

    @BeforeEach
    void prepararBase() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        // Con un Gestor existente, "Crear cuenta" funciona como en producción.
        gestor = crearUsuario("it.gestor@prueba.test", "GESTOR", "Gestor1234");
    }

    protected Usuario crearUsuario(String email, String rol, String clave) {
        Rol r = rolRepository.findByNombre(rol).orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre(rol);
            nuevo.setDescripcion(rol);
            return rolRepository.save(nuevo);
        });
        Usuario u = new Usuario();
        u.setNombreCompleto("Prueba " + rol);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(clave));
        u.setRol(r);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    protected Equipo crearEquipo(String codigo) {
        Sede sede = new Sede();
        sede.setNombre("Sede " + codigo);
        sede.setCiudad("Bogotá");
        sede = sedeRepository.save(sede);

        Area area = new Area();
        area.setSede(sede);
        area.setNombre("Área " + codigo);
        area = areaRepository.save(area);

        Equipo e = new Equipo();
        e.setCodigoInterno(codigo);
        e.setSerial("SER-" + codigo);
        e.setArea(area);
        e.setEstadoEquipo(estadoEquipoRepository.findByNombre("Estable").orElseThrow());
        e.setMarca("Dell");
        e.setModelo("Optiplex");
        e.setFechaCompra(LocalDate.now().minusYears(1));
        return equipoRepository.save(e);
    }

    protected CategoriaFalla categoria() {
        return categoriaFallaRepository.findAllByOrderByNombreAsc().get(0);
    }

    protected Incidencia crearIncidencia(Equipo equipo, Usuario reporta, LocalDateTime fecha) {
        Incidencia i = new Incidencia();
        i.setEquipo(equipo);
        i.setCategoriaFalla(categoria());
        i.setReportadoPor(reporta);
        i.setFechaReporte(fecha);
        i.setDescripcion("No enciende");
        i.setPrioridad("MEDIA");
        i.setGeneraIndisponibilidad(false);
        i.setEstado("ABIERTA");
        return incidenciaRepository.save(i);
    }

    /** Fuerza a leer de la base de datos y no de la memoria de la sesión. */
    protected Incidencia recargar(Long id) {
        em.flush();
        em.clear();
        return incidenciaRepository.findById(id).orElseThrow();
    }

    protected Object modelo(MvcResult r, String clave) {
        return r.getModelAndView().getModel().get(clave);
    }

    /** Cada prueba usa una IP distinta para no chocar con el límite de 5 cuentas por hora. */
    protected static RequestPostProcessor desdeIp(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }
}