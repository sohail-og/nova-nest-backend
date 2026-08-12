package com.novanest.config;

import com.novanest.model.Role;
import com.novanest.model.User;
import com.novanest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@novanest.com";
        Optional<User> adminOpt = userRepository.findByEmail(adminEmail);

        if (adminOpt.isEmpty()) {
            log.info("Admin account not found. Seeding default admin account: {}", adminEmail);
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("System Administrator");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setStatus("ACTIVE");
            admin.setPhone("0000000000");
            admin.setGender("Not Specified");
            userRepository.save(admin);
            log.info("Default admin account created successfully.");
        } else {
            log.info("Admin account {} already exists.", adminEmail);
        }
    }
}
