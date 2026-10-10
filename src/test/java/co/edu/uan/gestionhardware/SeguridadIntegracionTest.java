package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de seguridad basadas en OWASP Top 10: control de acceso, inyección, XSS, sesión y abuso. */
class SeguridadIntegracionTest extends BaseIntegracion {

    private static final String SCRIPT = "<script>alert(1)</script>";
    private static final String INYECCION = "' OR '1'='1";

    private Usuario ana;
    private Usuario beto;
    private Usuario tecnico;
    private Equipo equipo;
    private Incidencia deBeto;

    @BeforeEach
    void preparar() {
        ana = crearUsuario("it.ana@prueba.test", "USUARIO", "Clave12345");
        beto = crearUsuario("it.beto@prueba.test", "USUARIO", "Clave12345");
        tecnico = crearUsuario("it.tecnico@prueba.test", "TECNICO", "Tecnico1234");
        equipo = crearEquipo("IT-SEC-1");
        deBeto = crearIncidencia(equipo, beto, LocalDateTime.now());
    }

    private MockHttpServletRequestBuilder registro(String email, String ip) {
        return post("/solicitar-cuenta").with(csrf()).with(desdeIp(ip))
                .param("nombreCompleto", "Persona Prueba")
                .param("email", email)
                .param("password", "Clave12345")
                .param("passwordConfirmacion", "Clave12345")
                .param("aceptaTerminos", "on");
    }

    // ---------- A01: control de acceso ----------

    @Test
    void usuarioNoAbreIncidenciaAjena_alCambiarElNumeroEnLaUrl() throws Exception {
        for (String ruta : new String[]{"/mis-incidencias/{id}/editar", "/mis-incidencias/{id}/rechazar"}) {
            mvc.perform(get(ruta, deBeto.getId()).with(user(ana.getEmail()).roles("USUARIO")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mis-incidencias"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Test
    void usuarioNoModificaNiEliminaIncidenciaAjena() throws Exception {
        mvc.perform(post("/mis-incidencias/{id}/eliminar", deBeto.getId())
                        .with(user(ana.getEmail()).roles("USUARIO")).with(csrf()))
                .andExpect(redirectedUrl("/mis-incidencias"));
        assertThat(incidenciaRepository.existsById(deBeto.getId())).isTrue();

        mvc.perform(post("/mis-incidencias/guardar")
                        .with(user(ana.getEmail()).roles("USUARIO")).with(csrf())
                        .param("id", String.valueOf(deBeto.getId()))
                        .param("equipo", String.valueOf(equipo.getId()))
                        .param("categoriaFalla", String.valueOf(categoria().getId()))
                        .param("prioridad", "ALTA")
                        .param("descripcion", "TEXTO MODIFICADO POR OTRO USUARIO"))
                .andExpect(redirectedUrl("/mis-incidencias"));
        assertThat(recargar(deBeto.getId()).getDescripcion()).isEqualTo("No enciende");
    }

    @Test
    void listaPropia_noIncluyeIncidenciasAjenas() throws Exception {
        MvcResult r = mvc.perform(get("/mis-incidencias").with(user(ana.getEmail()).roles("USUARIO")))
                .andExpect(status().isOk()).andReturn();
        java.util.List<?> lista = (java.util.List<?>) modelo(r, "incidencias");
        assertThat(lista).noneMatch(i -> ((Incidencia) i).getId().equals(deBeto.getId()));
    }

    @ParameterizedTest(name = "{0} no puede hacer POST en {1}")
    @CsvSource({
            "USUARIO,/usuarios/guardar",
            "USUARIO,/usuarios/1/activar",
            "USUARIO,/equipos/guardar",
            "TECNICO,/usuarios/guardar",
            "TECNICO,/equipos/guardar",
            "TECNICO,/configuracion/guardar",
            "TECNICO,/sedes/guardar"
    })
    void rolesNoEscalanPrivilegios(String rol, String ruta) throws Exception {
        mvc.perform(post(ruta).with(user("it.cualquiera@prueba.test").roles(rol)).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void registro_noPermiteElegirRol() throws Exception {
        String email = "intruso@prueba.test";
        mvc.perform(registro(email, "10.30.0.1").param("rol", "GESTOR"))
                .andExpect(redirectedUrl("/login"));
        assertThat(usuarioRepository.findByEmailConRol(email).orElseThrow().getRol().getNombre())
                .isEqualTo("USUARIO");
    }

    // ---------- A03: inyección ----------

    @Test
    void inyeccionSql_enFiltros_noDevuelveTodo() throws Exception {
        MvcResult incidencias = mvc.perform(get("/incidencias").param("texto", INYECCION)
                        .with(user(tecnico.getEmail()).roles("TECNICO")))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) modelo(incidencias, "incidencias")).isEmpty();

        MvcResult porEstado = mvc.perform(get("/incidencias").param("estado", INYECCION)
                        .with(user(tecnico.getEmail()).roles("TECNICO")))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) modelo(porEstado, "incidencias")).isEmpty();

        MvcResult equipos = mvc.perform(get("/equipos").param("texto", INYECCION)
                        .with(user(tecnico.getEmail()).roles("TECNICO")))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) modelo(equipos, "equipos")).isEmpty();
    }

    @Test
    void inyeccionSql_enElLogin_noDaAcceso() throws Exception {
        mvc.perform(formLogin("/login").user("email", "' OR '1'='1' --").password("password", INYECCION))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---------- A03: XSS ----------

    @Test
    void xss_seMuestraComoTexto_noSeEjecuta() throws Exception {
        Equipo malicioso = crearEquipo("IT-XSS-1");
        malicioso.setMarca(SCRIPT);
        equipoRepository.save(malicioso);

        String listaEquipos = mvc.perform(get("/equipos").with(user(tecnico.getEmail()).roles("TECNICO")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(listaEquipos).doesNotContain(SCRIPT);
        assertThat(listaEquipos).contains("&lt;script&gt;alert(1)&lt;/script&gt;");

        // También en una descripción de incidencia escrita por un usuario.
        mvc.perform(post("/mis-incidencias/guardar")
                        .with(user(ana.getEmail()).roles("USUARIO")).with(csrf())
                        .param("equipo", String.valueOf(equipo.getId()))
                        .param("categoriaFalla", String.valueOf(categoria().getId()))
                        .param("prioridad", "MEDIA")
                        .param("descripcion", SCRIPT))
                .andExpect(redirectedUrl("/mis-incidencias"));

        String propias = mvc.perform(get("/mis-incidencias").with(user(ana.getEmail()).roles("USUARIO")))
                .andReturn().getResponse().getContentAsString();
        String delTecnico = mvc.perform(get("/incidencias").with(user(tecnico.getEmail()).roles("TECNICO")))
                .andReturn().getResponse().getContentAsString();
        assertThat(propias).doesNotContain(SCRIPT);
        assertThat(delTecnico).doesNotContain(SCRIPT);
    }

    // ---------- A07: contraseñas y sesión ----------

    @Test
    void contrasena_seGuardaConBcrypt() throws Exception {
        String email = "hash@prueba.test";
        mvc.perform(registro(email, "10.30.0.2")).andExpect(redirectedUrl("/login"));
        String hash = usuarioRepository.findByEmailConRol(email).orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").isNotEqualTo("Clave12345");
    }

    @Test
    void logout_invalidaLaSesion() throws Exception {
        MvcResult entrada = mvc.perform(formLogin("/login")
                        .user("email", gestor.getEmail()).password("password", "Gestor1234"))
                .andExpect(authenticated()).andReturn();
        MockHttpSession sesion = (MockHttpSession) entrada.getRequest().getSession(false);
        assertThat(sesion).isNotNull();

        mvc.perform(get("/dashboard").session(sesion)).andExpect(status().isOk());

        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(sesion.isInvalid()).isTrue();

        mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void tokenDeRecuperacion_vencePorlaHora() throws Exception {
        Usuario u = crearUsuario("it.token@prueba.test", "USUARIO", "ClaveVieja1");
        mvc.perform(post("/recuperar").with(csrf()).param("email", u.getEmail()))
                .andExpect(status().isOk());

        LocalDateTime vence = usuarioRepository.findByEmailConRol(u.getEmail()).orElseThrow().getResetTokenExpira();
        assertThat(vence).isAfter(LocalDateTime.now().plusMinutes(59));
        assertThat(vence).isBefore(LocalDateTime.now().plusMinutes(61));
    }

    // ---------- A05: configuración y cabeceras ----------

    @Test
    void cabecerasDeSeguridad_estanPresentes() throws Exception {
        var login = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(login.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(login.getHeader("X-Frame-Options")).isEqualTo("DENY");

        var dashboard = mvc.perform(get("/dashboard").with(user(gestor.getEmail()).roles("GESTOR")))
                .andReturn().getResponse();
        assertThat(dashboard.getHeader("Cache-Control")).contains("no-store");
    }

    // ---------- Abuso ----------

    @Test
    void limiteDeCincoCuentasPorHora_bloqueaLaSexta() throws Exception {
        String ip = "10.30.0.99";
        for (int i = 1; i <= 5; i++) {
            mvc.perform(registro("limite" + i + "@prueba.test", ip)).andExpect(redirectedUrl("/login"));
        }
        MvcResult sexta = mvc.perform(registro("limite6@prueba.test", ip))
                .andExpect(status().isOk()).andReturn();
        assertThat((String) modelo(sexta, "errorCrear")).contains("demasiadas cuentas");
        assertThat(usuarioRepository.existsByEmail("limite6@prueba.test")).isFalse();
    }
}