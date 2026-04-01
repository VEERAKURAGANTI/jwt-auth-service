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

	        http .csrf(AbstractHttpConfigurer::disable)

	     
	            .authorizeHttpRequests(auth -> auth

	                .requestMatchers(
	                    "/api/auth/**",       // login, register, refresh
	                    "/swagger-ui/**",     // Swagger UI pages
	                    "/swagger-ui.html",   // Swagger UI entry point
	                    "/api-docs/**",       // OpenAPI JSON
	                    "/v3/api-docs/**",    // OpenAPI v3 JSON
	                    "/"                   // serve index.html UI
	                ).permitAll()

	               
	                .requestMatchers(
	                    "/api/admin/**"       // all admin operations
	                ).hasRole("ADMIN")

	              
	                .anyRequest().authenticated()
	            )

	         
	            .sessionManagement(session -> session
	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	            )

	        
	            .authenticationProvider(authenticationProvider)

	           
	            .addFilterBefore(
	                jwtAuthenticationFilter,
	                UsernamePasswordAuthenticationFilter.class
	            );

	        return http.build();
	    }
}
