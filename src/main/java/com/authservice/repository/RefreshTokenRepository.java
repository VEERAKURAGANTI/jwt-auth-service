package com.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);
	Optional<RefreshToken> findByUser(User user);
	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.user=?1")
	void deleteByUser(User user);
}
