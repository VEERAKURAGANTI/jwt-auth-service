package com.authservice.controller;

import com.authservice.entity.User;
import com.authservice.security.UserDetailsImpl;
import com.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "User profile and management operations")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get current authenticated user profile")
	public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl currentUser) {

		User user = userService.getCurrentUser(currentUser);

		// 200 OK with full User entity as JSON
		return ResponseEntity.ok(user);
	}

	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get all users — Admin only")
	public ResponseEntity<List<User>> getAllUsers() {

		List<User> users = userService.getAllUsers();

		// 200 OK with list of all users
		return ResponseEntity.ok(users);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete a user by ID — Admin only")
	public ResponseEntity<String> deleteUser(@PathVariable Long id) {

		userService.deleteUser(id);

		// 200 OK with confirmation message
		return ResponseEntity.ok("User with id " + id + " deleted successfully.");
	}

}
