package com.authservice.service;

import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	private final UserRepository userRepository;

	public User getCurrentUser(UserDetailsImpl userDetails) {
		return userRepository.findById(userDetails.getId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userDetails.getId()));
	}
	
	public List<User> getAllUsers() {
        return userRepository.findAll();
    }
	
	public void deleteUser(Long userId) {

        // Check user exists before trying to delete
        // Gives a clear error instead of silent no-op
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException(
                    "Cannot delete — user not found with id: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("User deleted with id: {}", userId);
    }
}
