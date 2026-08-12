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
	private final com.novanest.service.JwtService jwtService;

	public AuthController(UserService userService, com.novanest.service.JwtService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, jakarta.servlet.http.HttpServletResponse response) {
		AuthResponse authResponse = userService.register(request);
		if (authResponse.getToken() != null) {
			org.springframework.http.ResponseCookie cookie = jwtService.createJwtCookie(authResponse.getToken());
			response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
		}
		return ResponseEntity.ok(authResponse);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletResponse response) {
		AuthResponse authResponse = userService.login(request);
		org.springframework.http.ResponseCookie cookie = jwtService.createJwtCookie(authResponse.getToken());
		response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
		return ResponseEntity.ok(authResponse);
	}


	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logout(
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			@CookieValue(value = "jwt", required = false) String jwtCookie,
			jakarta.servlet.http.HttpServletResponse response) {
		
		String token = null;
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		} else if (jwtCookie != null) {
			token = jwtCookie;
		}

		if (token != null) {
			userService.logout(token);
		}

		org.springframework.http.ResponseCookie cookie = jwtService.createCleanJwtCookie();
		response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());

		AuthResponse authResponse = new AuthResponse();
		authResponse.setMessage("Logged out successfully");
		return ResponseEntity.ok(authResponse);
	}

	@PutMapping("/change-password")
	public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			Principal principal) {
		if (principal == null) {
			AuthResponse response = new AuthResponse();
			response.setMessage("Unauthorized: Please login to change password");
			return ResponseEntity.status(401).body(response);
		}
		AuthResponse response = userService.changePassword(request, principal.getName());
		return ResponseEntity.ok(response);
	}

	@PutMapping("/reset-password")
	public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		AuthResponse response = userService.resetPassword(request);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/forgot-password")
	public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		AuthResponse response = userService.sendResetLink(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
		AuthResponse response = userService.verifyOtp(request);
		return ResponseEntity.ok(response);
	}
}
