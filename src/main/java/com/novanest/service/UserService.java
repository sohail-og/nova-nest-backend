package com.novanest.service;

import com.novanest.dto.AuthResponse;
import com.novanest.dto.LoginRequest;
import com.novanest.dto.RegisterRequest;
import com.novanest.dto.ChangePasswordRequest;

import com.novanest.exception.UserAlreadyExistsException;
import com.novanest.exception.ValidationException;
import com.novanest.model.User;
import com.novanest.repository.UserRepository;
import com.novanest.model.OtpToken;
import com.novanest.security.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.novanest.model.Role;

import com.novanest.dto.VerifyOtpRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.LocalDateTime;

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

	private void checkUserExistsByEmail(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new UserAlreadyExistsException("Email already exists");
		}
	}

	private void checkUserExistsByUsername(String username) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new UserAlreadyExistsException("Username already exists");
		}
	}

	private void checkUserExistsByPhone(String phone) {
		if (userRepository.findByPhone(phone).isPresent()) {
			throw new UserAlreadyExistsException("Phone number already exists");
		}
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);
		log.info("Incoming RegisterRequest for email: {}", request.getEmail());

		log.info("Validation started for email: {}", request.getEmail());
		if (!request.getPassword().equals(request.getConfirmPassword())) {
			throw new ValidationException("Passwords do not match");
		}

		validatePasswordStrength(request.getPassword());
		log.info("Validation completed for email: {}", request.getEmail());

		checkUserExistsByEmail(request.getEmail());
		log.info("Verified no duplicate verified users exist for email: {}", request.getEmail());

		log.info("Creating new User for email: {}", request.getEmail());
		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setGender(request.getGender());
		user.setEmail(request.getEmail());
		user.setPhone(request.getPhone());
		user.setFullName(request.getFullName());
		user.setRole(Role.CUSTOMER);
		user.setStatus("ACTIVE");

		log.info("User save started for email: {}", request.getEmail());
		user = userRepository.save(user);
		userRepository.flush();
		log.info("User saved with ID: {}", user.getId());

		// Authenticate and generate token immediately
		UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
		String token = jwtService.generateToken(userDetails);
		jwtService.saveUserToken(token, userDetails.getUsername());

		AuthResponse response = new AuthResponse();
		response.setMessage("Registration successful!");
		response.setUsername(user.getUsername());
		response.setToken(token);
		
		log.info("Registration completed successfully for email: {}", request.getEmail());
		return response;
	}


	public AuthResponse login(LoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());
		
		User user = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User account not found"));

		if (user.getRole() == Role.ADMIN) {
			throw new org.springframework.security.authentication.DisabledException("Access denied: Administrators cannot log in through the customer portal.");
		}

		String token = jwtService.generateToken(userDetails);

		jwtService.saveUserToken(token, userDetails.getUsername());

		AuthResponse response = new AuthResponse();
		response.setToken(token);
		response.setUsername(user.getUsername());
		response.setMessage("Login Successful");

		return response;
	}

	public AuthResponse changePassword(ChangePasswordRequest request, String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

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

	public void logout(String token) {
		jwtService.revokeToken(token);
	}

}
