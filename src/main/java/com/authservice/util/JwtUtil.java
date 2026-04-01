package com.authservice.util;

public class JwtUtil {
	public static final String AUTH_HEADER = "Authorization";
	public static final String TOKEN_PREFIX = "Bearer ";
	public static final String CLAIM_USERNAME = "sub";
	public static final String CLAIM_ROLES = "roles";
	public static final String TOKEN_TYPE = "Bearer";

	private JwtUtil() {
        
	}
}
