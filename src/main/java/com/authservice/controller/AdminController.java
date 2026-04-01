package com.authservice.controller;

import com.authservice.dto.MessageResponse;
import com.authservice.entity.Role;
import com.authservice.entity.Role.ERole;
import com.authservice.entity.User;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ← applies to ALL methods in this class
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Administrative operations — ROLE_ADMIN required")
public class AdminController {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@GetMapping("/dashboard")
	@Operation(summary = "Admin dashboard — verify admin access")
	public ResponseEntity<MessageResponse> dashboard() {
		return ResponseEntity.ok(new MessageResponse(
				"Welcome Admin! You have ROLE_ADMIN access. " + "Total users: " + userRepository.count()));
	}

	@PostMapping("/assign-role")
	@Operation(summary = "Assign a role to a user")
	public ResponseEntity<MessageResponse> assignRole(@RequestParam Long userId, @RequestParam String roleName) {

		// Find the user — throws RuntimeException if not found
		// GlobalExceptionHandler catches and returns 500
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

		// Convert the role name string to ERole enum
		// ERole.valueOf("ROLE_ADMIN") → ERole.ROLE_ADMIN
		// Throws IllegalArgumentException if invalid role name
		ERole eRole;
		try {
			eRole = ERole.valueOf(roleName.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(
					"Invalid role name: " + roleName + ". Valid values: ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN");
		}
		 Role role = roleRepository.findByName(eRole)
	                .orElseThrow(() -> new RuntimeException(
	                        "Role not found in DB: " + roleName));

	        // Add role to user's role set
	        // HashSet.add() is idempotent — no duplicate if already has role
	        user.getRoles().add(role);

	        // Save updates the user_roles join table
	        userRepository.save(user);

	        return ResponseEntity.ok(new MessageResponse(
	                "Role " + roleName + " assigned to user "
	                        + user.getUsername()));
	}

	@PostMapping("/revoke-role")
	@Operation(summary = "Remove a role from a user")
	public ResponseEntity<MessageResponse> revokeRole(@RequestParam Long userId, @RequestParam String roleName) {

		// Find the user
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

		// Convert string to ERole enum
		ERole eRole;
		try {
			eRole = ERole.valueOf(roleName.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(
					"Invalid role name: " + roleName + ". Valid values: ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN");
		}

		// Find the Role entity from DB
		Role role = roleRepository.findByName(eRole)
				.orElseThrow(() -> new RuntimeException("Role not found in DB: " + roleName));

		// Remove role from user's role set
		// HashSet.remove() is safe even if role not present
		user.getRoles().remove(role);

		// Save updates the user_roles join table
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("Role " + roleName + " revoked from user " + user.getUsername()));
	}

}
