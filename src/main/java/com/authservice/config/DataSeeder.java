package com.authservice.config;

import com.authservice.entity.Role;
import com.authservice.entity.Role.ERole;
import com.authservice.entity.User;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {

		log.info("─────────────────────────────────────");
		log.info("  DataSeeder starting...");
		log.info("─────────────────────────────────────");

		// ── Step 1: Seed Roles ──────────────────────────────────
		seedRole(ERole.ROLE_USER);
		seedRole(ERole.ROLE_MODERATOR);
		seedRole(ERole.ROLE_ADMIN);

		log.info("  Roles in DB: {}", roleRepository.count());

		// ── Step 2: Seed Default Admin User ────────────────────
		seedAdminUser();

		log.info("─────────────────────────────────────");
		log.info("  DataSeeder complete. App is ready!");
		log.info("─────────────────────────────────────");
	}

	private void seedRole(ERole eRole) {
		if (roleRepository.findByName(eRole).isEmpty()) {
			// Role does not exist → create and save it
			Role role = new Role();
			role.setName(eRole);
			roleRepository.save(role);
			log.info("  ✓ Created role: {}", eRole.name());
		} else {
			// Role already exists → skip silently
			log.debug("  → Role already exists: {}", eRole.name());
		}
	}

	private void seedAdminUser() {
		if (userRepository.existsByUsername("admin")) {
			log.debug("  → Admin user already exists, skipping.");
			return;
		}

		// Fetch both roles to assign to admin
		Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
				.orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found — roles not seeded yet!"));

		Role userRole = roleRepository.findByName(ERole.ROLE_USER)
				.orElseThrow(() -> new RuntimeException("ROLE_USER not found — roles not seeded yet!"));

		// Build the admin user
		User admin = User.builder().username("admin").email("admin@authservice.com")
				// BCrypt hash of "admin123"
				// CHANGE THIS IN PRODUCTION!
				.password(passwordEncoder.encode("admin123")).enabled(true).build();

		// Assign both ROLE_ADMIN and ROLE_USER
		admin.getRoles().add(adminRole);
		admin.getRoles().add(userRole);

		userRepository.save(admin);

		log.info("  ✓ Created default admin user");
		log.info("    Username : admin");
		log.info("    Password : admin123");
		log.info("    ⚠ CHANGE PASSWORD IN PRODUCTION!");
	}

}
