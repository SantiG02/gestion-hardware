package co.edu.uan.gestionhardware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccesoPorRolIntegracionTest extends BaseIntegracion {

    private static final String[][] REGLAS = {
            {"/usuarios", "GESTOR"},
            {"/configuracion", "GESTOR"},
            {"/reportes", "GESTOR"},
            {"/sedes", "GESTOR"},
            {"/dashboard", "GESTOR"},
            {"/equipos/nuevo", "GESTOR"},
            {"/equipos", "GESTOR,TECNICO"},
            {"/incidencias", "GESTOR,TECNICO"},
            {"/mantenimientos", "GESTOR,TECNICO"},
            {"/mis-incidencias", "USUARIO"},
            {"/panel-tecnico", "TECNICO"},
    };

    @BeforeEach
    void preparar() {
        crearUsuario("it.tecnico@prueba.test", "TECNICO", "Tecnico1234");
        crearUsuario("it.usuario@prueba.test", "USUARIO", "Usuario1234");
    }

    static Stream<Arguments> matriz() {
        List<Arguments> filas = new ArrayList<>();
        for (String[] regla : REGLAS) {
            List<String> permitidos = List.of(regla[1].split(","));
            for (String rol : List.of("GESTOR", "TECNICO", "USUARIO")) {
                filas.add(Arguments.of(rol, regla[0], permitidos.contains(rol)));
            }
        }
        return filas.stream();
    }

    private String correoDe(String rol) {
        return "GESTOR".equals(rol) ? gestor.getEmail() : "it." + rol.toLowerCase() + "@prueba.test";
    }

    @ParameterizedTest(name = "{0} en {1} → permitido={2}")
    @MethodSource("matriz")
    void accesoSegunRol(String rol, String ruta, boolean permitido) throws Exception {
        var peticion = get(ruta).with(user(correoDe(rol)).roles(rol));

        if (permitido) {
            mvc.perform(peticion).andExpect(status().is2xxSuccessful());
        } else {
            mvc.perform(peticion)
                    .andExpect(status().isForbidden())
                    .andExpect(forwardedUrl("/acceso-denegado"));
        }
    }

    @ParameterizedTest(name = "sin sesión en {0} → va al login")
    @ValueSource(strings = {"/usuarios", "/configuracion", "/reportes", "/sedes", "/dashboard",
            "/equipos", "/incidencias", "/mantenimientos", "/mis-incidencias", "/panel-tecnico"})
    void sinSesion_redirigeAlLogin(String ruta) throws Exception {
        mvc.perform(get(ruta))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @org.junit.jupiter.api.Test
    void paginasPublicas_respondenSinSesion() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/terminos")).andExpect(status().isOk());
        mvc.perform(get("/recuperar")).andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void postSinTokenCsrf_esRechazado() throws Exception {
        mvc.perform(post("/recuperar").param("email", "a@b.co"))
                .andExpect(status().isForbidden());
    }
}