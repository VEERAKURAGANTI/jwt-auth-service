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

	@Transactional
	public MessageResponse register(RegisterRequest request) {

		// ── Check 1: Username must be unique ───────────────────
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
		}

		// ── Check 2: Email must be unique ──────────────────────
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use.");
		}

		// ── Step 3: Build the User entity ──────────────────────
		User user = User.builder().username(request.getUsername()).email(request.getEmail())
				// CRITICAL: NEVER store plain text password
				// passwordEncoder.encode() runs BCrypt hashing
				// "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
				.password(passwordEncoder.encode(request.getPassword())).enabled(true).build();

		// ── Step 4: Assign roles ────────────────────────────────
		// resolveRoles() maps role name strings to Role entities
		// If no roles specified → defaults to ROLE_USER
		Set<Role> roles = resolveRoles(request.getRoles());
		user.setRoles(roles);

		// ── Step 5: Save to database ────────────────────────────
		// JPA generates: INSERT INTO users (username, email, password, enabled)
		// INSERT INTO user_roles (user_id, role_id)
		userRepository.save(user);
		log.info("New user registered: {}", user.getUsername());

		return new MessageResponse("User registered successfully! Username: " + user.getUsername());
	}

	public AuthResponse login(LoginRequest request) {

		// ── Step 1: Validate credentials ───────────────────────
		// This is the SPRING SECURITY authentication call.
		// It internally calls UserDetailsServiceImpl.loadUserByUsername()
		// then BCrypt-compares the password.
		// Throws BadCredentialsException if invalid.
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), // what client sent
						request.getPassword() // plain text (BCrypt compares internally)
				));

		// ── Step 2: Store in SecurityContext ───────────────────
		// Makes the user "authenticated" for the duration of this request
		// Not strictly necessary here but follows Spring Security best practice
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// ── Step 3: Extract our UserDetailsImpl ────────────────
		// authentication.getPrincipal() returns the object from loadUserByUsername()
		// We cast it to our UserDetailsImpl to access id, email etc.
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

		// ── Step 4: Generate JWT access token ──────────────────
		// JwtService signs the token with our secret key
		// Token contains: username, issued-at, expires-at
		String accessToken = jwtService.generateAccessToken(userDetails);

		// ── Step 5: Get User entity for refresh token ──────────
		// We need the User entity (not UserDetailsImpl) for RefreshToken
		User user = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// ── Step 6: Create refresh token in DB ─────────────────
		// RefreshTokenService deletes old token, creates new UUID token,
		// saves to refresh_tokens table
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

		// ── Step 7: Extract role names for response ─────────────
		// getAuthorities() returns [ROLE_USER, ROLE_ADMIN] as
		// GrantedAuthority objects. We map them to plain strings.
		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());

		log.info("User logged in: {}", userDetails.getUsername());

		// ── Step 8: Return response ─────────────────────────────
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
	public MessageResponse logout(String username) {

		// Find the user in DB
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		// Delete their refresh token from the DB
		// Now they cannot refresh → effectively logged out
		refreshTokenService.deleteByUser(user);

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
