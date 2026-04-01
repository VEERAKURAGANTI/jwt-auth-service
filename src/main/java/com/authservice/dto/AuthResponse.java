package com.authservice.dto;

import java.util.List;

import lombok.Data;

@Data
public class AuthResponse {
	private String accessToken;
	private String refreshToken;
	private String tokenType = "Bearer";

	private Long id;
	private String username;
	private String email;
	private List<String> roles;

	public AuthResponse(String accessToken, String refreshToken, Long id, String username, String email,
			List<String> roles) {

		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.id = id;
		this.username = username;
		this.email = email;
		this.roles = roles;
	}

}
