package com.authservice.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name="users",uniqueConstraints= {
		@UniqueConstraint(columnNames="username"),
		@UniqueConstraint(columnNames="email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@NotBlank
@Size(min=3,max=20)
@Column(nullable = false,unique = true,length = 20)
private String username;
@NotBlank
@Email
@Column(nullable = false,unique = true)
private String email;

@NotBlank
@Size(max = 120)                     // BCrypt hashes are always 60 chars but we give extra space
@Column(nullable = false)
private String password;
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),         // FK to THIS table
    inverseJoinColumns = @JoinColumn(name = "role_id")   // FK to roles table
)
@Builder.Default
private Set<Role> roles=new HashSet<>();
@Column(nullable = false)
@Builder.Default
private boolean enabled=true;

@CreationTimestamp
@Column(updatable = false)
private LocalDateTime createdAt;


}
