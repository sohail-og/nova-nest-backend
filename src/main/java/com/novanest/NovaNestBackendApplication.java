package com.novanest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.novanest.model.User;
import com.novanest.model.Role;
import com.novanest.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class NovaNestBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NovaNestBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (!userRepository.existsByEmail("11") && !userRepository.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@novanest.com");
				admin.setPassword(passwordEncoder.encode("AdminPassword123!"));
				admin.setRole(Role.ADMIN);
				admin.setGender("Male");
				admin.setPhone("9999999999");
				userRepository.save(admin);
				System.out.println("Default Admin user seeded successfully!");
			}
		};
	}

	@Bean
	public CommandLineRunner cleanupDb(JdbcTemplate jdbcTemplate) {
		return args -> {
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN email_verified"); } catch(Exception e) {}
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN otp"); } catch(Exception e) {}
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN otp_expiry"); } catch(Exception e) {}
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN is_verified"); } catch(Exception e) {}
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN verification_code"); } catch(Exception e) {}
			try { jdbcTemplate.execute("ALTER TABLE users DROP COLUMN pending"); } catch(Exception e) {}
		};
	}
}