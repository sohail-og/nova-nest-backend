package com.novanest.controller;

import com.novanest.model.CartItem;
import com.novanest.model.Product;
import com.novanest.model.User;
import com.novanest.repository.CartItemRepository;
import com.novanest.repository.ProductRepository;
import com.novanest.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartItemController {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartItemController(CartItemRepository cartItemRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(cartItemRepository.findByUser(user));
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody Map<String, Object> body) {
        User user = getAuthenticatedUser();
        Integer productId = (Integer) body.get("productId");
        Integer quantity = (Integer) body.get("quantity");

        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        final Integer finalQuantity = quantity;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct_Id(user, productId)
                .map(item -> {
                    item.setQuantity(item.getQuantity() + finalQuantity);
                    return cartItemRepository.save(item);
                })
                .orElseGet(() -> {
                    CartItem newItem = new CartItem(user, product, finalQuantity);
                    return cartItemRepository.save(newItem);
                });

        return ResponseEntity.ok(cartItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Integer quantity = body.get("quantity");
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().build();
        }

        cartItem.setQuantity(quantity);
        return ResponseEntity.ok(cartItemRepository.save(cartItem));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Integer id) {
        cartItemRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<Void> clearCart() {
        User user = getAuthenticatedUser();
        cartItemRepository.deleteByUser(user);
        return ResponseEntity.ok().build();
    }
}
