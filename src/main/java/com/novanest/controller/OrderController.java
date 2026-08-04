package com.novanest.controller;

import com.novanest.model.*;
import com.novanest.repository.*;
import com.novanest.service.ProductImageHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductImageHelper productImageHelper;

    public OrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                           CartItemRepository cartItemRepository, UserRepository userRepository,
                           ProductImageHelper productImageHelper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productImageHelper = productImageHelper;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(orderRepository.findByUser(user));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(orderId);
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                productImageHelper.populateImageUrl(item.getProduct());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("items", items);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        User user = getAuthenticatedUser();
        
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Create Order
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, user, totalAmount, OrderStatus.SUCCESS);
        Order savedOrder = orderRepository.save(order);

        // Create Order Items
        for (CartItem item : cartItems) {
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            
            OrderItem orderItem = new OrderItem(
                savedOrder,
                item.getProduct(),
                item.getQuantity(),
                price,
                itemTotal
            );
            orderItemRepository.save(orderItem);
        }

        // Clear User Cart
        cartItemRepository.deleteByUser(user);

        return ResponseEntity.ok(savedOrder);
    }
}
