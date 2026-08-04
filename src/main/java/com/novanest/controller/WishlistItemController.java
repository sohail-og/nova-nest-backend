package com.novanest.controller;

import com.novanest.model.Product;
import com.novanest.model.User;
import com.novanest.model.WishlistItem;
import com.novanest.repository.ProductRepository;
import com.novanest.repository.UserRepository;
import com.novanest.repository.WishlistItemRepository;
import com.novanest.service.ProductImageHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistItemController {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageHelper productImageHelper;

    public WishlistItemController(WishlistItemRepository wishlistItemRepository, UserRepository userRepository,
                                  ProductRepository productRepository, ProductImageHelper productImageHelper) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageHelper = productImageHelper;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<WishlistItem>> getWishlist() {
        User user = getAuthenticatedUser();
        List<WishlistItem> items = wishlistItemRepository.findByUser(user);
        for (WishlistItem item : items) {
            if (item.getProduct() != null) {
                productImageHelper.populateImageUrl(item.getProduct());
            }
        }
        return ResponseEntity.ok(items);
    }

    @PostMapping("/toggle")
    @Transactional
    public ResponseEntity<Map<String, String>> toggleWishlist(@RequestBody Map<String, Integer> body) {
        User user = getAuthenticatedUser();
        Integer productId = body.get("productId");

        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<WishlistItem> existing = wishlistItemRepository.findByUserAndProduct_Id(user, productId);
        Map<String, String> response = new HashMap<>();

        if (existing.isPresent()) {
            wishlistItemRepository.delete(existing.get());
            response.put("status", "removed");
            response.put("message", "Product removed from wishlist");
        } else {
            WishlistItem newItem = new WishlistItem(user, product);
            wishlistItemRepository.save(newItem);
            response.put("status", "added");
            response.put("message", "Product added to wishlist");
        }

        return ResponseEntity.ok(response);
    }
}
