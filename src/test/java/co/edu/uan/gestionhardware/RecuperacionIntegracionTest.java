package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecuperacionIntegracionTest extends BaseIntegracion {

    @Test
    void flujoCompleto_solicitar_restablecer_eIngresar() throws Exception {
        Usuario u = crearUsuario("it.recupera@prueba.test", "USUARIO", "ClaveVieja1");

        mvc.perform(post("/recuperar").with(csrf()).param("email", u.getEmail()))
                .andExpect(status().isOk())
                .andExpect(view().name("recuperacion/solicitar"))
                .andExpect(model().attribute("enviado", true));

        String token = usuarioRepository.findByEmailConRol(u.getEmail()).orElseThrow().getResetToken();
        assertThat(token).isNotBlank();

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(notificacion).enviarRecuperacionContrasena(any(Usuario.class), enlace.capture());
        assertThat(enlace.getValue()).endsWith("/restablecer?token=" + token);

        MvcResult formulario = mvc.perform(get("/restablecer").param("token", token))
                .andExpect(status().isOk()).andReturn();
        assertThat(modelo(formulario, "token")).isEqualTo(token);

        mvc.perform(post("/restablecer").with(csrf())
                        .param("token", token)
                        .param("password", "ClaveNueva1")
                        .param("passwordConfirmacion", "ClaveNueva1"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("exito"));

        mvc.perform(formLogin("/login").user("email", u.getEmail()).password("password", "ClaveNueva1"))
                .andExpect(authenticated());
        mvc.perform(formLogin("/login").user("email", u.getEmail()).password("password", "ClaveVieja1"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        // El token sirve una sola vez.
        MvcResult reuso = mvc.perform(get("/restablecer").param("token", token))
                .andExpect(status().isOk()).andReturn();
        assertThat(modelo(reuso, "tokenInvalido")).isEqualTo(true);
    }

    @Test
    void correoInexistente_muestraElMismoMensaje_ySinEnviarCorreo() throws Exception {
        mvc.perform(post("/recuperar").with(csrf()).param("email", "nadie@prueba.test"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("enviado", true));

        verify(notificacion, never()).enviarRecuperacionContrasena(any(Usuario.class), anyString());
    }

    @Test
    void tokenVencido_oInventado_esRechazado() throws Exception {
        Usuario u = crearUsuario("it.vencido@prueba.test", "USUARIO", "ClaveVieja1");
        u.setResetToken("token-vencido");
        u.setResetTokenExpira(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.save(u);

        MvcResult vencido = mvc.perform(get("/restablecer").param("token", "token-vencido"))
                .andExpect(status().isOk()).andReturn();
        assertThat(modelo(vencido, "tokenInvalido")).isEqualTo(true);

        MvcResult inventado = mvc.perform(get("/restablecer").param("token", "no-existe"))
                .andExpect(status().isOk()).andReturn();
        assertThat(modelo(inventado, "tokenInvalido")).isEqualTo(true);
    }

    @Test
    void clavesDistintas_noCambianLaContrasena() throws Exception {
        Usuario u = crearUsuario("it.distintas@prueba.test", "USUARIO", "ClaveVieja1");
        u.setResetToken("token-ok");
        u.setResetTokenExpira(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(u);

        MvcResult r = mvc.perform(post("/restablecer").with(csrf())
                        .param("token", "token-ok")
                        .param("password", "ClaveNueva1")
                        .param("passwordConfirmacion", "OtraClave99"))
                .andExpect(status().isOk()).andReturn();
        assertThat((String) modelo(r, "error")).contains("no coinciden");

        mvc.perform(formLogin("/login").user("email", u.getEmail()).password("password", "ClaveVieja1"))
                .andExpect(authenticated());
    }
}