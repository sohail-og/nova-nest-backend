package com.novanest.controller;

import com.novanest.model.*;
import com.novanest.repository.*;
import com.novanest.service.ProductImageHelper;
import com.novanest.service.InvoicePdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductImageHelper productImageHelper;
    private final InvoicePdfService invoicePdfService;

    public OrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                           CartItemRepository cartItemRepository, UserRepository userRepository,
                           ProductImageHelper productImageHelper, InvoicePdfService invoicePdfService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productImageHelper = productImageHelper;
        this.invoicePdfService = invoicePdfService;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByEmail(username)
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
        
        Map<String, Object> address = (Map<String, Object>) body.get("address");
        if (address != null) {
            order.setShippingFullName((String) address.get("fullName"));
            order.setShippingPhone((String) address.get("phone"));
            order.setShippingHouseNo((String) address.get("houseNo"));
            order.setShippingStreet((String) address.get("street"));
            order.setShippingArea((String) address.get("area"));
            order.setShippingCity((String) address.get("city"));
            order.setShippingDistrict((String) address.get("district"));
            order.setShippingState((String) address.get("state"));
            order.setShippingCountry((String) address.get("country"));
            order.setShippingPincode((String) address.get("pincode"));
        }

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

    @GetMapping("/{id}/invoice")
    public ResponseEntity<?> downloadInvoice(@PathVariable String id) {
        long startTime = System.currentTimeMillis();
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            List<OrderItem> items = orderItemRepository.findByOrder_OrderId(id);

            byte[] pdfBytes = invoicePdfService.generateInvoicePdf(order, items);

            String customerName = "Customer";
            if (order.getShippingFullName() != null && !order.getShippingFullName().trim().isEmpty()) {
                customerName = order.getShippingFullName();
            } else if (order.getUser() != null && order.getUser().getFullName() != null) {
                customerName = order.getUser().getFullName();
            }
            
            // Sanitize filename: replace spaces with '_' and remove invalid characters
            String sanitizedCustomerName = customerName.trim().replaceAll("\\s+", "_").replaceAll("[\\\\/:*?\"<>|]", "");
            String filename = "invoice-order-" + sanitizedCustomerName + ".pdf";

            long duration = System.currentTimeMillis() - startTime;
            log.info("[PDF DOWNLOAD SUCCESS] Order ID: {}, Filename: {}, Size: {} bytes, Response Status: 200, Total Request Time: {} ms", 
                     id, filename, pdfBytes.length, duration);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length))
                    .body(pdfBytes);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[PDF DOWNLOAD FAILED] Order ID: {}, Response Status: 500, Error: {}, Total Request Time: {} ms", 
                      id, e.getMessage(), duration, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage() != null ? e.getMessage() : "Failed to generate invoice");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(response);
        }
    }
}
