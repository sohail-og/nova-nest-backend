package com.novanest.controller;

import com.novanest.model.User;
import com.novanest.repository.UserRepository;
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

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getHomeData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to Nova Nest, " + username + "!");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("gender", user.getGender());

        return ResponseEntity.ok(response);
    }
}
