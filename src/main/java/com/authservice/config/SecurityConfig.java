package com.authservice.config;

import com.authservice.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig — Fixed for Spring Boot 3.5.x
 *
 * FIXES APPLIED:
 *   1. CORS configured properly — allows frontend to call API
 *   2. Static resources permitted — index.html loads correctly
 *   3. All public endpoints whitelisted — no more 403 on login/register
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // ── Disable CSRF (not needed for JWT REST APIs) ──────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Enable CORS with our configuration ───────────────
            // Without this, browser blocks ALL cross-origin requests
            // even if CORS headers are set elsewhere
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── URL Authorization Rules ──────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Static files — serve the frontend UI
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.html",
                    "/*.css",
                    "/*.js",
                    "/favicon.ico",
                    "/static/**",
                    "/assets/**"
                ).permitAll()

                // Public auth endpoints — no token needed
                .requestMatchers(
                    "/api/auth/**"
                ).permitAll()

                // Swagger UI — allow without auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()

                // Admin endpoints — ROLE_ADMIN only
                .requestMatchers(
                    "/api/admin/**"
                ).hasRole("ADMIN")

                // Everything else needs a valid JWT
                .anyRequest().authenticated()
            )

            // ── Stateless — no server-side sessions ─────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Our custom authentication provider ───────────────
            .authenticationProvider(authenticationProvider)

            // ── JWT filter runs before Spring's default filter ───
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * CORS Configuration — allows the browser to call your API.
     *
     * WHY IS CORS NEEDED?
     *   When your frontend (localhost:8080) calls your API (localhost:8080)
     *   it works fine. But if you ever test from a different port or
     *   deploy the frontend separately, the browser will block requests
     *   with "CORS policy" error unless the server explicitly allows it.
     *
     *   This configuration allows:
     *     → Any origin (for development — restrict in production)
     *     → GET, POST, PUT, DELETE, OPTIONS methods
     *     → Authorization header (needed to send JWT)
     *     → Content-Type header (needed to send JSON)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow these origins — add your frontend URL here
        // In production replace "*" with your actual domain
        config.setAllowedOriginPatterns(List.of("*"));

        // Allow these HTTP methods
        config.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE",
                          "PATCH", "OPTIONS")
        );

        // Allow these headers in requests
        config.setAllowedHeaders(
            Arrays.asList("Authorization", "Content-Type",
                          "X-Requested-With", "Accept",
                          "Origin", "Access-Control-Request-Method",
                          "Access-Control-Request-Headers")
        );

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // How long browsers cache the preflight response (1 hour)
        config.setMaxAge(3600L);

        // Apply this CORS config to ALL paths
        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}