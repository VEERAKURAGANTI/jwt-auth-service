package com.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class TokenBlacklistService {

	private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

	public void blacklist(String token) {
		blacklistedTokens.add(token);
		log.debug("Token blacklisted. Total blacklisted: {}", blacklistedTokens.size());
	}

	public boolean isBlacklisted(String token) {
		return blacklistedTokens.contains(token);
	}
}
