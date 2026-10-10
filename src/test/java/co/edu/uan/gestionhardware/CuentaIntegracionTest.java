package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CuentaIntegracionTest extends BaseIntegracion {

    private MockHttpServletRequestBuilder solicitud(String email, String clave, String ip) {
        return post("/solicitar-cuenta").with(csrf()).with(desdeIp(ip))
                .param("nombreCompleto", "Persona Prueba")
                .param("email", email)
                .param("password", clave)
                .param("passwordConfirmacion", clave)
                .param("aceptaTerminos", "on");
    }

    private String codigoEnviado(String email) {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(notificacion).enviarCodigoVerificacion(anyString(), eq(email), codigo.capture());
        return codigo.getValue();
    }

    @Test
    void crearCuenta_confirmarCodigo_eIngresar() throws Exception {
        String email = "nueva.persona@prueba.test";
        when(notificacion.correoConfigurado()).thenReturn(true);

        // Paso 1: datos. La cuenta NO se crea todavía, se pide el código.
        mvc.perform(solicitud(email, "Clave12345", "10.1.0.1"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("verificar", true));
        assertThat(usuarioRepository.existsByEmail(email)).isFalse();

        // Paso 2: con el código enviado por correo, la cuenta se crea.
        String codigo = codigoEnviado(email);
        mvc.perform(post("/verificar-cuenta").with(csrf())
                        .param("email", email).param("codigo", codigo))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("exito"));

        Usuario creado = usuarioRepository.findByEmailConRol(email).orElseThrow();
        assertThat(creado.getRol().getNombre()).isEqualTo("USUARIO");
        assertThat(creado.getActivo()).isTrue();
        assertThat(creado.getPasswordHash()).isNotEqualTo("Clave12345");

        // Paso 3: ingresa con esa cuenta y llega a su pantalla.
        mvc.perform(formLogin("/login").user("email", email).password("password", "Clave12345"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/mis-incidencias"));
    }

    @Test
    void codigoIncorrecto_descuentaIntentos_yAlQuintoReinicia() throws Exception {
        String email = "intentos@prueba.test";
        when(notificacion.correoConfigurado()).thenReturn(true);
        mvc.perform(solicitud(email, "Clave12345", "10.1.0.2")).andExpect(status().isOk());

        String real = codigoEnviado(email);
        String malo = real.equals("000000") ? "111111" : "000000";

        MvcResult primero = mvc.perform(post("/verificar-cuenta").with(csrf())
                        .param("email", email).param("codigo", malo))
                .andExpect(status().isOk())
                .andExpect(model().attribute("verificar", true))
                .andReturn();
        assertThat((String) modelo(primero, "errorCrear")).contains("Te quedan 4");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/verificar-cuenta").with(csrf())
                    .param("email", email).param("codigo", malo)).andExpect(status().isOk());
        }

        // Quinto intento fallido: el registro se descarta.
        MvcResult quinto = mvc.perform(post("/verificar-cuenta").with(csrf())
                        .param("email", email).param("codigo", malo))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("verificar"))
                .andReturn();
        assertThat((String) modelo(quinto, "errorCrear")).contains("Demasiados intentos");

        // Aunque ahora mande el código verdadero, ya no sirve.
        MvcResult tarde = mvc.perform(post("/verificar-cuenta").with(csrf())
                        .param("email", email).param("codigo", real))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((String) modelo(tarde, "errorCrear")).contains("venció");
        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void sinCorreoConfigurado_creaLaCuentaDirecto() throws Exception {
        String email = "directa@prueba.test";
        // El mock devuelve false por defecto: es el caso del equipo de un jurado sin clave de Brevo.
        mvc.perform(solicitud(email, "Clave12345", "10.1.0.3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(usuarioRepository.existsByEmail(email)).isTrue();
        verify(notificacion, never()).enviarCodigoVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void correoYaRegistrado_noSeCreaOtraCuenta() throws Exception {
        crearUsuario("existente@prueba.test", "USUARIO", "Clave12345");

        MvcResult r = mvc.perform(solicitud("existente@prueba.test", "Otra12345", "10.1.0.4"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andReturn();
        assertThat((String) modelo(r, "errorCrear")).contains("Ya existe una cuenta");
    }

    @Test
    void validaciones_clavesDistintas_terminos_ybots() throws Exception {
        MvcResult distintas = mvc.perform(post("/solicitar-cuenta").with(csrf()).with(desdeIp("10.1.0.5"))
                        .param("nombreCompleto", "Ana").param("email", "ana@prueba.test")
                        .param("password", "Clave12345").param("passwordConfirmacion", "Otra12345")
                        .param("aceptaTerminos", "on"))
                .andExpect(status().isOk()).andReturn();
        assertThat((String) modelo(distintas, "errorCrear")).contains("no coinciden");

        MvcResult sinTerminos = mvc.perform(post("/solicitar-cuenta").with(csrf()).with(desdeIp("10.1.0.6"))
                        .param("nombreCompleto", "Ana").param("email", "ana@prueba.test")
                        .param("password", "Clave12345").param("passwordConfirmacion", "Clave12345"))
                .andExpect(status().isOk()).andReturn();
        assertThat((String) modelo(sinTerminos, "errorCrear")).contains("Términos");

        // Campo señuelo lleno = bot: responde "bien" pero no guarda nada.
        mvc.perform(solicitud("bot@prueba.test", "Clave12345", "10.1.0.7").param("sitioWeb", "http://spam.com"))
                .andExpect(redirectedUrl("/login"));
        assertThat(usuarioRepository.existsByEmail("bot@prueba.test")).isFalse();
    }
}