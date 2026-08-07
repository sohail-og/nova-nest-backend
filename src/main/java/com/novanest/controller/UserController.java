package com.novanest.controller;

import com.novanest.model.User;
import com.novanest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Welcome User";
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String currentUsername = principal.getName();
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("email")) {
            String email = (String) body.get("email");
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body("Email already in use");
            }
            user.setEmail(email);
        }
        if (body.containsKey("phone")) {
            String phone = (String) body.get("phone");
            if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                return ResponseEntity.badRequest().body("Phone number already in use");
            }
            user.setPhone(phone);
        }
        if (body.containsKey("gender")) {
            user.setGender((String) body.get("gender"));
        }
        if (body.containsKey("address")) {
            Map<String, String> address = (Map<String, String>) body.get("address");
            if (address != null) {
                user.setHouseNo(address.get("houseNo"));
                user.setStreet(address.get("street"));
                user.setArea(address.get("area"));
                user.setCity(address.get("city"));
                user.setDistrict(address.get("district"));
                user.setState(address.get("state"));
                user.setCountry(address.get("country"));
                user.setPincode(address.get("pincode"));
            }
        }
        if (body.containsKey("profileImage")) {
            user.setProfileImage((String) body.get("profileImage"));
        }
        if (body.containsKey("password") && body.get("password") != null && !body.get("password").toString().trim().isEmpty()) {
            String rawPassword = body.get("password").toString();
            if (rawPassword.length() < 8) {
                return ResponseEntity.badRequest().body("Password must be at least 8 characters long");
            }
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        User saved = userRepository.save(user);
        log.info("[PROFILE UPDATE] Saved profile successfully for user: {}", saved.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("username", saved.getUsername());
        response.put("email", saved.getEmail());
        response.put("phone", saved.getPhone());
        response.put("gender", saved.getGender());
        
        Map<String, String> addr = new HashMap<>();
        addr.put("houseNo", saved.getHouseNo());
        addr.put("street", saved.getStreet());
        addr.put("area", saved.getArea());
        addr.put("city", saved.getCity());
        addr.put("district", saved.getDistrict());
        addr.put("state", saved.getState());
        addr.put("country", saved.getCountry());
        addr.put("pincode", saved.getPincode());
        response.put("address", addr);

        response.put("profileImage", saved.getProfileImage());
        
        log.info("[PROFILE UPDATE] Returning updated profile response");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/profile/address")
    public ResponseEntity<?> updateAddress(@RequestBody Map<String, String> address, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String currentUsername = principal.getName();
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        user.setHouseNo(address.get("houseNo"));
        user.setStreet(address.get("street"));
        user.setArea(address.get("area"));
        user.setCity(address.get("city"));
        user.setDistrict(address.get("district"));
        user.setState(address.get("state"));
        user.setCountry(address.get("country"));
        user.setPincode(address.get("pincode"));
        
        userRepository.save(user);
        return ResponseEntity.ok("Address updated successfully");
    }
}
