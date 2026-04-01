package com.authservice.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 20, message = "Usernmae must be 3 to 20 characters")
	private String username;
	@NotBlank(message = " Email is required")
	@Email(message = "Must be a valid email address")
	private String email;
	@NotBlank(message = "Password is required")
	@Size(min = 6, max = 40, message = "Password must be 6 to 40 characters")
	private String password;

	private Set<String> roles;
}
