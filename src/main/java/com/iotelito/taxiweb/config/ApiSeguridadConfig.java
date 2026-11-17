package com.iotelito.taxiweb.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API REST /api/v1 para la app. Sin sesión ni CSRF: cada llamada trae el token de Firebase
 * (Authorization: Bearer). Se verifica la firma con las claves públicas de Google, el emisor
 * y la audiencia del proyecto. El rol se decide después, leyendo users/{uid} en Firestore.
 */
@Configuration
public class ApiSeguridadConfig {

    static final String CLAVES_GOOGLE =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    JwtDecoder firebaseJwtDecoder(@Value("${iotelito.firebase.project-id}") String proyecto) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CLAVES_GOOGLE).build();
        OAuth2TokenValidator<Jwt> validador = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + proyecto),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(proyecto)),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank()));
        decoder.setJwtValidator(validador);
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http, JwtDecoder decoder) throws Exception {
        AuthenticationEntryPoint sinSesion = (request, response, error) ->
                escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO",
                        "Inicia sesión para usar el servicio de taxis.");
        AccessDeniedHandler prohibido = (request, response, error) ->
                escribir(response, HttpServletResponse.SC_FORBIDDEN, "PROHIBIDO",
                        "Tu cuenta no puede hacer esto.");

        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(peticion -> peticion
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/aeropuertos").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.decoder(decoder))
                        .authenticationEntryPoint(sinSesion)
                        .accessDeniedHandler(prohibido))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinSesion)
                        .accessDeniedHandler(prohibido));
        return http.build();
    }

    private static void escribir(HttpServletResponse response, int status, String codigo, String mensaje)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
