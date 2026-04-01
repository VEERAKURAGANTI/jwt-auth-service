package com.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING)
	@Column(length = 20,unique = true,nullable = false)
	private ERole name;

	public enum ERole {
		ROLE_USER, // Default role — every new user gets this
		ROLE_MODERATOR, // Can moderate content
		ROLE_ADMIN // Full access — admin operations
	}
}
