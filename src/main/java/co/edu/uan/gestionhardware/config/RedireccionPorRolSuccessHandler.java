package co.edu.uan.gestionhardware.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Redirige segun el rol justo despues de iniciar sesion: el Gestor
 * Tecnologico entra directo al dashboard (RF-18), el Tecnico de Soporte
 * entra al inventario, ya que /dashboard es exclusivo de GESTOR.
 */
public class RedireccionPorRolSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        boolean esGestor = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol -> rol.equals("ROLE_GESTOR"));

        setDefaultTargetUrl(esGestor ? "/dashboard" : "/equipos");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}