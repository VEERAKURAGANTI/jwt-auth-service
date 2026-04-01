package com.authservice.controller;

import com.authservice.dto.AuthResponse;
import com.authservice.dto.LoginRequest;
import com.authservice.dto.MessageResponse;
import com.authservice.dto.RefreshTokenRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.security.UserDetailsImpl;
import com.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh tokens, and logout")
public class AuthController {
	private final AuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Register a new user account")
	public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {

		MessageResponse response = authService.register(request);

		// 200 OK — user created successfully
		return ResponseEntity.ok(response);
	}

	@PostMapping("/login")
	@Operation(summary = "Login and receive JWT tokens")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

		AuthResponse response = authService.login(request);

		// 200 OK — authentication successful
		return ResponseEntity.ok(response);
	}
	 @PostMapping("/refresh")
	    @Operation(summary = "Get new access token using refresh token")
	    public ResponseEntity<AuthResponse> refreshToken(
	            @Valid @RequestBody RefreshTokenRequest request) {

	        AuthResponse response = authService.refreshToken(request);

	        // 200 OK — new access token issued
	        return ResponseEntity.ok(response);
	    }
	  @PostMapping("/logout")
	    @PreAuthorize("isAuthenticated()")
	    @SecurityRequirement(name = "Bearer Authentication")
	    @Operation(summary = "Logout and revoke refresh token")
	    public ResponseEntity<MessageResponse> logout(
	            @AuthenticationPrincipal UserDetailsImpl currentUser) {

	        MessageResponse response = authService.logout(
	                currentUser.getUsername());

	        // 200 OK — logged out successfully
	        return ResponseEntity.ok(response);
	    }

}
