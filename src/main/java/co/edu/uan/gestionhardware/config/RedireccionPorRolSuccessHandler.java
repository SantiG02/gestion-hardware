package co.edu.uan.gestionhardware.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;

/**
 * Redirige segun el rol justo despues de iniciar sesion: el Gestor
 * Tecnologico entra directo al dashboard (RF-18), el Tecnico de Soporte
 * entra al inventario, y el Usuario Final entra a sus propias incidencias,
 * ya que no tiene acceso a ninguna otra parte del sistema.
 */
public class RedireccionPorRolSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        String destino;
        if (roles.contains("ROLE_GESTOR")) {
            destino = "/dashboard";
        } else if (roles.contains("ROLE_TECNICO")) {
            destino = "/equipos";
        } else {
            destino = "/mis-incidencias";
        }

        setDefaultTargetUrl(destino);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}