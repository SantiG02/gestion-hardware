package co.edu.uan.gestionhardware;

import co.edu.uan.gestionhardware.dto.Alerta;
import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Usuario;
import co.edu.uan.gestionhardware.service.ConfiguracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertaSeguimientoIntegracionTest extends BaseIntegracion {

    @Autowired private ConfiguracionService configuracionService;

    private Usuario usuario;
    private int umbral;

    @BeforeEach
    void preparar() {
        usuario = crearUsuario("it.reporta@prueba.test", "USUARIO", "Usuario1234");
        umbral = configuracionService.obtener().getUmbralFallasMes();
    }

    private String estadoDe(Long equipoId) {
        em.flush();
        em.clear();
        return equipoRepository.findById(equipoId).orElseThrow().getEstadoEquipo().getNombre();
    }

    private ResumenDashboard abrirDashboard() throws Exception {
        MvcResult r = mvc.perform(get("/dashboard").with(user(gestor.getEmail()).roles("GESTOR")))
                .andExpect(status().isOk())
                .andReturn();
        return (ResumenDashboard) modelo(r, "resumen");
    }

    @Test
    void fallasRepetidas_pasanElEquipoASeguimiento_yGeneranAlerta() throws Exception {
        Equipo equipo = crearEquipo("IT-SEG-1");
        for (int i = 0; i < umbral; i++) {
            crearIncidencia(equipo, usuario, LocalDateTime.now().minusDays(i));
        }
        assertThat(estadoDe(equipo.getId())).isEqualTo("Estable");

        ResumenDashboard resumen = abrirDashboard();

        assertThat(estadoDe(equipo.getId())).isEqualTo("En seguimiento");
        assertThat(resumen.getAlertas())
                .anyMatch(a -> "Fallas recurrentes".equals(a.getTipo())
                        && "IT-SEG-1".equals(a.getEquipoCodigo()));
    }

    @Test
    void pocasFallas_elEquipoSigueEstable_ySinAlerta() throws Exception {
        Equipo equipo = crearEquipo("IT-SEG-2");
        crearIncidencia(equipo, usuario, LocalDateTime.now());

        ResumenDashboard resumen = abrirDashboard();

        assertThat(estadoDe(equipo.getId())).isEqualTo("Estable");
        assertThat(resumen.getAlertas())
                .noneMatch(a -> "IT-SEG-2".equals(a.getEquipoCodigo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notificar_entregaLasAlertasAlServicioDeCorreo() throws Exception {
        Equipo equipo = crearEquipo("IT-SEG-3");
        for (int i = 0; i < umbral; i++) {
            crearIncidencia(equipo, usuario, LocalDateTime.now().minusDays(i));
        }

        mvc.perform(post("/dashboard/notificar")
                        .with(user(gestor.getEmail()).roles("GESTOR")).with(csrf()))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("exito"));

        ArgumentCaptor<List<Alerta>> enviadas = ArgumentCaptor.forClass(List.class);
        verify(notificacion).enviarAlertas(enviadas.capture());
        assertThat(enviadas.getValue())
                .anyMatch(a -> "IT-SEG-3".equals(a.getEquipoCodigo()));
    }
}