package com.authservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.authservice.dto.MessageResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		log.warn("Validation failed:{}", errors);
		return ResponseEntity.badRequest().body(errors);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex) {

		// Log the real reason internally but send vague message to client
		log.warn("Failed login attempt: {}", ex.getMessage());

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new MessageResponse("Invalid username or password."));
	}
	 @ExceptionHandler(TokenRefreshException.class)
	    public ResponseEntity<MessageResponse> handleTokenRefreshException(
	            TokenRefreshException ex) {

	        log.warn("Refresh token failed for token [{}]: {}",
	                ex.getToken(), ex.getMessage());

	        return ResponseEntity
	                .status(HttpStatus.FORBIDDEN)
	                .body(new MessageResponse(ex.getMessage()));
	    }
	 
//	 @ExceptionHandler(AccessDeniedException.class)
//	    public ResponseEntity<MessageResponse> handleAccessDenied(
//	            AccessDeniedException ex) {
//
//	        log.warn("Access denied: {}", ex.getMessage());
//
//	        return ResponseEntity
//	                .status(HttpStatus.FORBIDDEN)
//	                .body(new MessageResponse(
//	                        "Access denied: You don't have permission to perform this action."));
//	    }
	 
	 @ExceptionHandler(RuntimeException.class)
	    public ResponseEntity<MessageResponse> handleRuntimeException(
	            RuntimeException ex) {

	        // Log full stack trace in Eclipse console for debugging
	        log.error("Unhandled exception: {}", ex.getMessage(), ex);

	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new MessageResponse("An error occurred: " + ex.getMessage()));
	    }
	 
	 @ExceptionHandler(UserAlreadyExistsException.class)
	    public ResponseEntity<MessageResponse> handleUserAlreadyExists(
	            UserAlreadyExistsException ex) {

	        log.warn("Registration conflict: {}", ex.getMessage());

	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)   // 409
	                .body(new MessageResponse(ex.getMessage()));
	    }
	 @ExceptionHandler(TokenExpiredException.class)
	    public ResponseEntity<MessageResponse> handleTokenExpired(
	            TokenExpiredException ex) {

	        log.warn("Access token expired: {}", ex.getMessage());

	        return ResponseEntity
	                .status(HttpStatus.UNAUTHORIZED)   // 401
	                .body(new MessageResponse(ex.getMessage()));
	    }

}
