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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.authservice.security.UserDetailsImpl;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin operations — ROLE_ADMIN required")
public class AdminController {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	private void requireAdmin(UserDetailsImpl currentUser) {
		boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (!isAdmin) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied: ROLE_ADMIN required.");
		}
	}

	@GetMapping("/dashboard")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Admin dashboard")
	public ResponseEntity<MessageResponse> dashboard(@AuthenticationPrincipal UserDetailsImpl currentUser) {

		requireAdmin(currentUser);

		return ResponseEntity.ok(new MessageResponse("Welcome Admin! Total users: " + userRepository.count()));
	}
	

	@PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<MessageResponse> assignRole(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam Long userId,
            @RequestParam String roleName) {

        requireAdmin(currentUser);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + userId));

        ERole eRole;
        try {
            eRole = ERole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid role: " + roleName
                    + ". Use: ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN");
        }

        Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException(
                        "Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse(
                "Role " + roleName + " assigned to "
                        + user.getUsername()));
    }


	@PostMapping("/revoke-role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revoke role from user")
    public ResponseEntity<MessageResponse> revokeRole(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam Long userId,
            @RequestParam String roleName) {

        requireAdmin(currentUser);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + userId));

        ERole eRole;
        try {
            eRole = ERole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid role: " + roleName);
        }

        Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException(
                        "Role not found: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse(
                "Role " + roleName + " revoked from "
                        + user.getUsername()));
    }

}
