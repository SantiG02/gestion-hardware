package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidenciaIntegracionTest extends BaseIntegracion {

    private Usuario usuario;
    private Usuario tecnico;
    private Equipo equipo;

    @BeforeEach
    void preparar() {
        usuario = crearUsuario("it.usuario@prueba.test", "USUARIO", "Usuario1234");
        tecnico = crearUsuario("it.tecnico@prueba.test", "TECNICO", "Tecnico1234");
        equipo = crearEquipo("IT-INC-1");
    }

    private void tecnicoCierra(Long id, String solucion) throws Exception {
        mvc.perform(post("/incidencias/{id}/cerrar", id)
                        .with(user(tecnico.getEmail()).roles("TECNICO")).with(csrf())
                        .param("solucion", solucion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidencias"));
    }

    @Test
    void ciclo_reporte_cierre_yAprobacion() throws Exception {
        // 1. El usuario reporta desde su pantalla.
        mvc.perform(post("/mis-incidencias/guardar")
                        .with(user(usuario.getEmail()).roles("USUARIO")).with(csrf())
                        .param("equipo", String.valueOf(equipo.getId()))
                        .param("categoriaFalla", String.valueOf(categoria().getId()))
                        .param("prioridad", "MEDIA")
                        .param("descripcion", "El equipo no enciende"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-incidencias"));

        List<Incidencia> propias = incidenciaRepository.findPorUsuarioReportador(usuario.getId());
        assertThat(propias).hasSize(1);
        Long id = propias.get(0).getId();
        Incidencia abierta = recargar(id);
        assertThat(abierta.getEstado()).isEqualTo("ABIERTA");
        assertThat(abierta.getConfirmacionUsuario()).isNull();

        // 2. El técnico la cierra con la solución aplicada.
        tecnicoCierra(id, "Se cambió la fuente de poder");
        Incidencia cerrada = recargar(id);
        assertThat(cerrada.getEstado()).isEqualTo("CERRADA");
        assertThat(cerrada.getConfirmacionUsuario()).isEqualTo("PENDIENTE");
        assertThat(cerrada.getSolucion()).isEqualTo("Se cambió la fuente de poder");
        assertThat(cerrada.getFechaCierre()).isNotNull();

        // 3. El usuario confirma que funcionó.
        mvc.perform(post("/mis-incidencias/{id}/confirmar", id)
                        .with(user(usuario.getEmail()).roles("USUARIO")).with(csrf()))
                .andExpect(redirectedUrl("/mis-incidencias"));
        assertThat(recargar(id).getConfirmacionUsuario()).isEqualTo("APROBADA");
    }

    @Test
    void rechazo_reabreLaIncidencia_yElTecnicoPuedeCerrarla2daVez() throws Exception {
        Incidencia inc = crearIncidencia(equipo, usuario, LocalDateTime.now());
        tecnicoCierra(inc.getId(), "Reinicio de equipo");

        mvc.perform(post("/mis-incidencias/{id}/rechazar", inc.getId())
                        .with(user(usuario.getEmail()).roles("USUARIO")).with(csrf())
                        .param("comentario", "Sigue sin encender"))
                .andExpect(redirectedUrl("/mis-incidencias"));

        Incidencia reabierta = recargar(inc.getId());
        assertThat(reabierta.getEstado()).isEqualTo("ABIERTA");
        assertThat(reabierta.getConfirmacionUsuario()).isEqualTo("RECHAZADA");
        assertThat(reabierta.getComentarioUsuario()).isEqualTo("Sigue sin encender");
        assertThat(reabierta.getVecesRechazada()).isEqualTo(1);
        assertThat(reabierta.getFechaCierre()).isNull();

        tecnicoCierra(inc.getId(), "Cambio de placa");
        Incidencia nuevamente = recargar(inc.getId());
        assertThat(nuevamente.getEstado()).isEqualTo("CERRADA");
        assertThat(nuevamente.getConfirmacionUsuario()).isEqualTo("PENDIENTE");
        assertThat(nuevamente.getVecesRechazada()).isEqualTo(1);
    }

    @Test
    void otroUsuario_noPuedeConfirmarUnaIncidenciaAjena() throws Exception {
        Usuario otro = crearUsuario("it.otro@prueba.test", "USUARIO", "Usuario1234");
        Incidencia inc = crearIncidencia(equipo, usuario, LocalDateTime.now());
        tecnicoCierra(inc.getId(), "Listo");

        mvc.perform(post("/mis-incidencias/{id}/confirmar", inc.getId())
                        .with(user(otro.getEmail()).roles("USUARIO")).with(csrf()))
                .andExpect(redirectedUrl("/mis-incidencias"));

        assertThat(recargar(inc.getId()).getConfirmacionUsuario()).isEqualTo("PENDIENTE");
    }

    @Test
    void cerrarSinSolucion_noCambiaNada() throws Exception {
        Incidencia inc = crearIncidencia(equipo, usuario, LocalDateTime.now());

        mvc.perform(post("/incidencias/{id}/cerrar", inc.getId())
                        .with(user(tecnico.getEmail()).roles("TECNICO")).with(csrf())
                        .param("solucion", "   "))
                .andExpect(redirectedUrl("/incidencias/" + inc.getId() + "/cerrar"));

        assertThat(recargar(inc.getId()).getEstado()).isEqualTo("ABIERTA");
    }
}