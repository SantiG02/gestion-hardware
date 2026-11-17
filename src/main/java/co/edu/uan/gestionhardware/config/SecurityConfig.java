package co.edu.uan.gestionhardware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Content-Security-Policy: los scripts solo se cargan desde el propio sitio
    // (no hay scripts ni manejadores en linea). Los estilos en linea se permiten
    // porque las plantillas usan atributos style.
    private static final String POLITICA_CSP =
            "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; "
            + "font-src 'self'; "
            + "connect-src 'self'; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                                // Publico
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/configuracion-inicial/**", "/solicitar-cuenta", "/verificar-cuenta", "/reenviar-codigo", "/terminos", "/recuperar", "/restablecer").permitAll()

                // Exclusivo del Gestor Tecnologico
                .requestMatchers("/usuarios/**").hasRole("GESTOR")
                .requestMatchers("/configuracion/**").hasRole("GESTOR")
                .requestMatchers("/reportes/**").hasRole("GESTOR")

                // Escritura sobre el inventario: solo Gestor
                .requestMatchers("/equipos/nuevo", "/equipos/guardar").hasRole("GESTOR")
                .requestMatchers("/equipos/*/editar", "/equipos/*/desactivar").hasRole("GESTOR")
                .requestMatchers("/equipos/importar/**").hasRole("GESTOR")

                // Consulta del inventario: ambos roles
                .requestMatchers("/equipos/**").hasAnyRole("GESTOR", "TECNICO")

                .requestMatchers("/sedes/**").hasRole("GESTOR")
                .requestMatchers("/tipos-equipo/**").hasRole("GESTOR")

                .requestMatchers("/incidencias/**").hasAnyRole("GESTOR", "TECNICO")
                .requestMatchers("/mantenimientos/**").hasAnyRole("GESTOR", "TECNICO")

                .requestMatchers("/dashboard/**").hasRole("GESTOR")
                .requestMatchers("/mis-incidencias/**").hasRole("USUARIO")
                .requestMatchers("/panel-tecnico/**").hasRole("TECNICO")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(new RedireccionPorRolSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CSP))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.SAME_ORIGIN))
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acceso-denegado")
            );

        return http.build();
    }
}