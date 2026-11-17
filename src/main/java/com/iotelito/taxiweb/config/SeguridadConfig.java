package com.iotelito.taxiweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Panel web: solo el superadmin, con sesión y contraseña BCrypt. El registro de taxistas
 * es público. Todo lo demás se rechaza hasta que tenga su propia regla (la API REST tendrá
 * su cadena aparte, con el token de Firebase).
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain panelWeb(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(peticion -> peticion
                        .requestMatchers("/", "/error", "/favicon.ico", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/taxistas/registro").permitAll()
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .anyRequest().denyAll())
                .formLogin(login -> login
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/superadmin/dashboard", true)
                        .failureUrl("/?error")
                        .permitAll())
                // Los enlaces "Cerrar sesión" de las vistas son <a>, por eso se acepta GET.
                .logout(salida -> salida
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                        .logoutSuccessUrl("/?salio")
                        .permitAll());
        return http.build();
    }
}
