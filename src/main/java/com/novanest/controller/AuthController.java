package com.novanest.controller;

import com.novanest.dto.*;

import com.novanest.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.novanest.dto.ForgotPasswordRequest;
import com.novanest.dto.VerifyOtpRequest;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = userService.register(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = userService.login(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/send-otp")
	public ResponseEntity<AuthResponse> sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {

		AuthResponse response = userService.sendOtp(request);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {

		AuthResponse response = userService.verifyOtp(request);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			userService.logout(token);
		}
		AuthResponse response = new AuthResponse();
		response.setMessage("Logged out successfully");
		return ResponseEntity.ok(response);
	}

	@PutMapping("/change-password")
	public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			Principal principal) {
		AuthResponse response = userService.changePassword(request, principal.getName());
		return ResponseEntity.ok(response);
	}

	@PutMapping("/reset-password")
	public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		AuthResponse response = userService.resetPassword(request);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		AuthResponse response = userService.sendOtp(request);
		return ResponseEntity.ok(response.getMessage());
	}
}
