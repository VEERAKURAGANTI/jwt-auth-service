package com.authservice.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import com.authservice.security.JwtService;
import com.authservice.security.UserDetailsServiceImpl;
import com.authservice.service.TokenBlacklistService;
import com.authservice.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsServiceImpl userDetailsService;
	private final TokenBlacklistService tokenBlacklistService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		final String authHeader = request.getHeader(JwtUtil.AUTH_HEADER);
		if (authHeader == null || !authHeader.startsWith(JwtUtil.TOKEN_PREFIX)) {
			// No JWT found — pass request to next filter unchanged
			filterChain.doFilter(request, response);
			return;
		}
		final String jwt = authHeader.substring(JwtUtil.TOKEN_PREFIX.length());
		 if (tokenBlacklistService.isBlacklisted(jwt)) {
	            log.warn("Blacklisted token used — rejecting request");
	            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	            response.setContentType("application/json");
	            response.getWriter().write(
	                "{\"message\": \"Token has been revoked. Please log in again.\"}");
	            return;
	        }
		
		final String username;
		try {
			username = jwtService.extractUsername(jwt);
		} catch (Exception e) {
			// Token is malformed or signature is invalid
			// Log it and skip to next filter → Spring returns 401
			log.error("Cannot extract username from JWT: {}", e.getMessage());
			filterChain.doFilter(request, response);
			return;
		}
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails;
			try {
				userDetails = userDetailsService.loadUserByUsername(username);
			} catch (Exception e) {
				// User not found in DB (deleted after token was issued?)
				log.error("User not found during JWT validation: {}", e.getMessage());
				filterChain.doFilter(request, response);
				return;
			}
			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, // principal
						null, // credentials (not needed)
						userDetails.getAuthorities() // roles
				);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);

				log.debug("Authenticated user: {} for request: {}", username, request.getRequestURI());
			} else {
				// Token validation failed (expired or wrong user)
				log.warn("Invalid JWT token for user: {}", username);
			}
		}
		filterChain.doFilter(request, response);
	}

	@Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip JWT filter for ALL these paths
        return path.equals("/")
                || path.equals("/index.html")
                || path.endsWith(".html")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".ico")
                || path.startsWith("/static/")
                || path.startsWith("/assets/")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars");
    }

}
