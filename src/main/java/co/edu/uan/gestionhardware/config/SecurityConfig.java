package co.edu.uan.gestionhardware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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

                // Exclusivo del Gestor Tecnologico
                .requestMatchers("/usuarios/**").hasRole("GESTOR")
                .requestMatchers("/configuracion/**").hasRole("GESTOR")
                .requestMatchers("/reportes/**").hasRole("GESTOR")

                // Escritura sobre el inventario: solo Gestor
                .requestMatchers("/equipos/nuevo", "/equipos/guardar").hasRole("GESTOR")
                .requestMatchers("/equipos/*/editar", "/equipos/*/desactivar").hasRole("GESTOR")

                // Consulta del inventario: ambos roles
                .requestMatchers("/equipos/**").hasAnyRole("GESTOR", "TECNICO")

                .requestMatchers("/sedes/**").hasRole("GESTOR")

                .requestMatchers("/incidencias/**").hasAnyRole("GESTOR", "TECNICO")
                .requestMatchers("/mantenimientos/**").hasAnyRole("GESTOR", "TECNICO")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/equipos", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acceso-denegado")
            );

        return http.build();
    }
}