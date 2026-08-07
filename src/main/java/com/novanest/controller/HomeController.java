package com.novanest.controller;

import com.novanest.model.User;
import com.novanest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHomeData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, String> addr = new HashMap<>();
        addr.put("houseNo", user.getHouseNo());
        addr.put("street", user.getStreet());
        addr.put("area", user.getArea());
        addr.put("city", user.getCity());
        addr.put("district", user.getDistrict());
        addr.put("state", user.getState());
        addr.put("country", user.getCountry());
        addr.put("pincode", user.getPincode());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Nova Nest, " + username + "!");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("gender", user.getGender());
        response.put("address", addr);
        response.put("profileImage", user.getProfileImage());

        log.info("[PROFILE GET] Returning address details from DB for user {}", username);

        return ResponseEntity.ok(response);
    }
}
