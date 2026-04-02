package com.authservice.service;

import com.authservice.dto.*;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.Role;
import com.authservice.entity.Role.ERole;
import com.authservice.entity.User;
import com.authservice.exception.TokenRefreshException;
import com.authservice.exception.UserAlreadyExistsException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtService;
import com.authservice.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final TokenBlacklistService tokenBlacklistService;

	@Transactional
	public MessageResponse register(RegisterRequest request) {

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
		}

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use.");
		}

		User user = User.builder().username(request.getUsername()).email(request.getEmail())

				.password(passwordEncoder.encode(request.getPassword())).enabled(true).build();

		Set<Role> roles = resolveRoles(request.getRoles());
		user.setRoles(roles);

		userRepository.save(user);
		log.info("New user registered: {}", user.getUsername());

		return new MessageResponse("User registered successfully! Username: " + user.getUsername());
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), // what client sent
						request.getPassword() // plain text (BCrypt compares internally)
				));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

		String accessToken = jwtService.generateAccessToken(userDetails);

		User user = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());

		log.info("User logged in: {}", userDetails.getUsername());

		return new AuthResponse(accessToken, refreshToken.getToken(), userDetails.getId(), userDetails.getUsername(),
				userDetails.getEmail(), roles);
	}

	public AuthResponse refreshToken(RefreshTokenRequest request) {

		String requestRefreshToken = request.getRefreshToken();

		return refreshTokenService.findByToken(requestRefreshToken)
				// Validate: not expired, not revoked
				// Throws TokenRefreshException if invalid
				.map(refreshTokenService::verifyExpiration)
				// Get the User entity associated with this token
				.map(RefreshToken::getUser)
				// Build the AuthResponse with a new access token
				.map(user -> {
					// Build UserDetailsImpl from User entity
					UserDetailsImpl userDetails = UserDetailsImpl.build(user);

					// Generate brand new access token
					String newAccessToken = jwtService.generateAccessToken(userDetails);

					// Extract role names
					List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
							.collect(Collectors.toList());

					log.debug("Access token refreshed for: {}", user.getUsername());

					// Return same refresh token — NOT rotated here
					// For higher security, generate a new refresh
					// token here too (full token rotation)
					return new AuthResponse(newAccessToken, requestRefreshToken, user.getId(), user.getUsername(),
							user.getEmail(), roles);
				})
				// If findByToken() returned empty Optional
				// → token doesn't exist in DB
				.orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
						"Refresh token not found. Please log in again."));
	}

	@Transactional
	public MessageResponse logout(String username, String accessToken) {

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		// 1. Delete refresh token from DB
		// → client cannot get new access tokens
		refreshTokenService.deleteByUser(user);

		// 2. Blacklist the current access token
		// → token immediately rejected even before expiry
		if (accessToken != null && !accessToken.isEmpty()) {
			tokenBlacklistService.blacklist(accessToken);
			log.info("Access token blacklisted for user: {}", username);
		}

		log.info("User logged out: {}", username);

		return new MessageResponse("Logged out successfully. See you next time!");
	}

	private Set<Role> resolveRoles(Set<String> roleNames) {
		Set<Role> roles = new HashSet<>();

		if (roleNames == null || roleNames.isEmpty()) {
			// No roles specified → assign default ROLE_USER
			Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(
					() -> new RuntimeException("ROLE_USER not found. " + "Make sure DataSeeder has run correctly."));
			roles.add(userRole);

		} else {
			// Map each string to the corresponding Role entity
			roleNames.forEach(roleName -> {
				switch (roleName.toLowerCase()) {

				case "admin" -> {
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found."));
					roles.add(adminRole);
				}

				case "mod" -> {
					Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
							.orElseThrow(() -> new RuntimeException("ROLE_MODERATOR not found."));
					roles.add(modRole);
				}

				// Unknown role string → default to ROLE_USER
				default -> {
					Role userRole = roleRepository.findByName(ERole.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("ROLE_USER not found."));
					roles.add(userRole);
				}
				}
			});
		}

		return roles;
	}

}
