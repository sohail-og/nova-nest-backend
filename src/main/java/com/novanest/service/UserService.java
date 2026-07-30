package com.novanest.service;

import com.novanest.dto.AuthResponse;

import com.novanest.dto.LoginRequest;
import com.novanest.dto.RegisterRequest;
import com.novanest.dto.ChangePasswordRequest;
import com.novanest.dto.ResetPasswordRequest;
import com.novanest.exception.UserAlreadyExistsException;
import com.novanest.exception.ValidationException;
import com.novanest.model.User;
import com.novanest.repository.UserRepository;
import com.novanest.security.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.novanest.model.Role;
import com.novanest.service.MailService;
import com.novanest.service.OtpService;
import com.novanest.dto.ForgotPasswordRequest;
import com.novanest.dto.VerifyOtpRequest;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final CustomUserDetailsService customUserDetailsService;
	private final MailService mailService;
	private final OtpService otpService;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService,
			MailService mailService, OtpService otpService) {

		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.customUserDetailsService = customUserDetailsService;
		this.mailService = mailService;
		this.otpService = otpService;
	}

	private void validatePasswordStrength(String password) {
		if (password.length() < 8) {
			throw new ValidationException("Password must be at least 8 characters long");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new ValidationException("Password must contain at least one uppercase letter");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new ValidationException("Password must contain at least one lowercase letter");
		}
		if (!password.matches(".*[0-9].*")) {
			throw new ValidationException("Password must contain at least one number");
		}
		if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
			throw new ValidationException("Password must contain at least one special character");
		}
	}

	/**
	 * Register new user
	 */
	public AuthResponse register(RegisterRequest request) {

		if (!request.getPassword().equals(request.getConfirmPassword())) {
			throw new ValidationException("Passwords do not match");
		}

		validatePasswordStrength(request.getPassword());

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException("Username already exists");
		}

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException("Email already exists");
		}

		if (userRepository.existsByPhone(request.getPhone())) {
			throw new UserAlreadyExistsException("Phone number already exists");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setGender(request.getGender());
		user.setEmail(request.getEmail());
		user.setPhone(request.getPhone());
		user.setRole(Role.CUSTOMER);

		userRepository.save(user);

		AuthResponse response = new AuthResponse();
		response.setMessage("Registration Successful");
		response.setUsername(user.getUsername());

		return response;
	}

	/**
	 * Login user
	 */
	public AuthResponse login(LoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());
		String token = jwtService.generateToken(userDetails);

		jwtService.saveUserToken(token, userDetails.getUsername());

		AuthResponse response = new AuthResponse();
		response.setToken(token);
		response.setUsername(userDetails.getUsername());
		response.setMessage("Login Successful");

		return response;
	}

	/**
	 * Change password for authenticated user
	 */
	public AuthResponse changePassword(ChangePasswordRequest request, String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ValidationException("User not found"));

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new ValidationException("Invalid current password");
		}

		if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
			throw new ValidationException("Confirm new password does not match new password");
		}

		validatePasswordStrength(request.getNewPassword());

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		AuthResponse response = new AuthResponse();
		response.setMessage("Password updated successfully");
		response.setUsername(user.getUsername());
		return response;
	}

	/**
	 * Reset password via email verification
	 */
	public AuthResponse resetPassword(ResetPasswordRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ValidationException("Email address does not exist"));

		if (!otpService.isEmailOtpVerified(request.getEmail())) {
			throw new ValidationException("OTP not verified or expired for this email. Please verify OTP first.");
		}

		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new ValidationException("Passwords do not match");
		}

		validatePasswordStrength(request.getNewPassword());

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		otpService.clearOtp(request.getEmail());

		AuthResponse response = new AuthResponse();
		response.setMessage("Password reset successful");
		response.setUsername(user.getUsername());
		return response;
	}

	/**
	 * Send OTP to registered email
	 */
	public AuthResponse sendOtp(ForgotPasswordRequest request) {

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ValidationException("Email does not exist"));

		String otp = otpService.generateOtp(request.getEmail());

		mailService.sendOtp(request.getEmail(), otp);

		AuthResponse response = new AuthResponse();
		response.setMessage("OTP sent successfully to your email");
		response.setUsername(user.getUsername());

		return response;
	}

	/**
	 * Verify OTP
	 */
	public AuthResponse verifyOtp(VerifyOtpRequest request) {

		boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());

		if (!valid) {
			throw new ValidationException("Invalid or Expired OTP");
		}

		AuthResponse response = new AuthResponse();
		response.setMessage("OTP Verified Successfully");

		return response;
	}

	/**
	 * Logout user
	 */
	public void logout(String token) {
		jwtService.revokeToken(token);
	}
}