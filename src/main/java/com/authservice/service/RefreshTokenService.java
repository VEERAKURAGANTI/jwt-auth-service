package com.authservice.service;

import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.exception.TokenRefreshException;
import com.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
	@Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    @Transactional
    public RefreshToken createRefreshToken(User user) {

        // Delete existing token for this user (token rotation)
        // This ensures only ONE active session per user
        refreshTokenRepository.deleteByUser(user);

        // Build the new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                // Random UUID - not a JWT, just a lookup key
                .token(UUID.randomUUID().toString())
                // Expiry = right now + 7 days (in milliseconds)
                .expiryDate(Instant.now()
                        .plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        // Save to refresh_tokens table and return
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user: {}", user.getUsername());

        return saved;
    }
    public RefreshToken verifyExpiration(RefreshToken token) {

        // Check 1: Has this token been manually revoked?
        if (token.isRevoked()) {
            refreshTokenRepository.delete(token);
            log.warn("Revoked refresh token used: {}", token.getToken());
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has been revoked. Please log in again.");
        }

        // Check 2: Has this token passed its 7-day expiry?
        // token.isExpired() checks: expiryDate.isBefore(Instant.now())
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("Expired refresh token used: {}", token.getToken());
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has expired. Please log in again.");
        }

        // Token passed both checks — it is valid
        return token;
    }
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.debug("Deleted refresh token for user: {}", user.getUsername());
    }
}
