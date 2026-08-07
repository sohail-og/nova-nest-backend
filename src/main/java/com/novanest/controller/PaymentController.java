package com.novanest.controller;

import com.novanest.model.*;
import com.novanest.repository.*;
import com.novanest.dto.*;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public PaymentController(UserRepository userRepository, CartItemRepository cartItemRepository,
                             OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateAddress(AddressDto address) {
        if (address == null) throw new IllegalArgumentException("Address cannot be null");
        if (address.getFullName() == null || address.getFullName().trim().isEmpty()) throw new IllegalArgumentException("fullName is missing");
        if (address.getPhone() == null || address.getPhone().trim().isEmpty()) throw new IllegalArgumentException("phone is missing");
        if (address.getEmail() == null || address.getEmail().trim().isEmpty()) throw new IllegalArgumentException("email is missing");
        if (address.getHouseNo() == null || address.getHouseNo().trim().isEmpty()) throw new IllegalArgumentException("houseNo is missing");
        if (address.getStreet() == null || address.getStreet().trim().isEmpty()) throw new IllegalArgumentException("street is missing");
        if (address.getArea() == null || address.getArea().trim().isEmpty()) throw new IllegalArgumentException("area is missing");
        if (address.getCity() == null || address.getCity().trim().isEmpty()) throw new IllegalArgumentException("city is missing");
        if (address.getDistrict() == null || address.getDistrict().trim().isEmpty()) throw new IllegalArgumentException("district is missing");
        if (address.getState() == null || address.getState().trim().isEmpty()) throw new IllegalArgumentException("state is missing");
        if (address.getCountry() == null || address.getCountry().trim().isEmpty()) throw new IllegalArgumentException("country is missing");
        if (address.getPincode() == null || address.getPincode().trim().isEmpty()) throw new IllegalArgumentException("pincode is missing");
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder() {
        try {
            User user = getAuthenticatedUser();
            List<CartItem> cartItems = cartItemRepository.findByUser(user);
            if (cartItems.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Cart is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate total amount
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(itemTotal);
            }
            BigDecimal grandTotal = subtotal.add(BigDecimal.valueOf(250)); // subtotal + ₹250 delivery charge

            // Amount in paise
            long amountInPaise = grandTotal.multiply(BigDecimal.valueOf(100)).longValue();

            // Call Razorpay to create order
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            com.razorpay.Order order = razorpay.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", razorpayOrderId);
            response.put("amount", String.valueOf(amountInPaise));
            response.put("currency", "INR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to create Razorpay order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyPaymentRequest payload) {
        try {
            String razorpayOrderId = payload.getRazorpayOrderId();
            String razorpayPaymentId = payload.getRazorpayPaymentId();
            String razorpaySignature = payload.getRazorpaySignature();

            if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Missing Razorpay payment parameters");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify signature using HmacSHA256
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String generatedSignature = hexString.toString();

            if (!generatedSignature.equals(razorpaySignature)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Payment verification failed: invalid signature");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate the address BEFORE creating the order
            try {
                validateAddress(payload.getAddress());
            } catch (IllegalArgumentException ex) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Please provide a valid delivery address: " + ex.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // Signature verified successfully
            User user = getAuthenticatedUser();
            
            List<CartItem> cartItems = cartItemRepository.findByUser(user);
            if (cartItems.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Cart is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate total amount
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(itemTotal);
            }
            BigDecimal grandTotal = subtotal.add(BigDecimal.valueOf(250)); // subtotal + ₹250 delivery charge

            // Save Order
            Order order = new Order(razorpayOrderId, user, grandTotal, OrderStatus.SUCCESS);
            order.setPaymentMethod("RAZORPAY");
            order.setPaymentStatus("PAID");

            // Save and associate shipping address
            if (payload.getAddress() != null) {
                AddressDto addr = payload.getAddress();
                order.setShippingFullName(addr.getFullName());
                order.setShippingPhone(addr.getPhone());
                order.setShippingHouseNo(addr.getHouseNo());
                order.setShippingStreet(addr.getStreet());
                order.setShippingArea(addr.getArea());
                order.setShippingCity(addr.getCity());
                order.setShippingDistrict(addr.getDistrict());
                order.setShippingState(addr.getState());
                order.setShippingCountry(addr.getCountry());
                order.setShippingPincode(addr.getPincode());
                logger.info("Selected Address: {} {}, {}, {}, {}, {}, {}, {}, {}", 
                    addr.getFullName(), addr.getPhone(), addr.getHouseNo(), addr.getStreet(), 
                    addr.getArea(), addr.getCity(), addr.getState(), addr.getCountry(), addr.getPincode());
                logger.info("Address validation result: SUCCESS");
            } else {
                logger.warn("Address validation result: FAILED - Missing address");
            }

            Order savedOrder = orderRepository.save(order);
            logger.info("Order ID: {}", savedOrder.getOrderId());

            // Save Order Items
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

            // Save Payment Details
            Payment payment = new Payment(
                    razorpayPaymentId,
                    savedOrder,
                    razorpayOrderId,
                    razorpaySignature,
                    grandTotal,
                    "INR",
                    "SUCCESS"
            );
            paymentRepository.save(payment);

            // Clear Cart
            cartItemRepository.deleteByUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified and order saved successfully");
            response.put("orderId", savedOrder.getOrderId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Internal server error during verification: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/cod")
    @Transactional
    public ResponseEntity<?> createCodOrder(@RequestBody CodPaymentRequest payload) {
        try {
            AddressDto shippingAddress = payload.getAddress();
            try {
                validateAddress(shippingAddress);
            } catch (IllegalArgumentException ex) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Please provide a valid delivery address: " + ex.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            User user = getAuthenticatedUser();
            List<CartItem> cartItems = cartItemRepository.findByUser(user);
            if (cartItems.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Cart is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate total amount
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(itemTotal);
            }
            BigDecimal grandTotal = subtotal.add(BigDecimal.valueOf(250)); // subtotal + ₹250 delivery charge

            // Save Order
            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Order order = new Order(orderId, user, grandTotal, OrderStatus.PENDING);
            order.setPaymentMethod("COD");
            order.setPaymentStatus("PENDING");

            // Save and associate shipping address
            if (shippingAddress != null) {
                order.setShippingFullName(shippingAddress.getFullName());
                order.setShippingPhone(shippingAddress.getPhone());
                order.setShippingHouseNo(shippingAddress.getHouseNo());
                order.setShippingStreet(shippingAddress.getStreet());
                order.setShippingArea(shippingAddress.getArea());
                order.setShippingCity(shippingAddress.getCity());
                order.setShippingDistrict(shippingAddress.getDistrict());
                order.setShippingState(shippingAddress.getState());
                order.setShippingCountry(shippingAddress.getCountry());
                order.setShippingPincode(shippingAddress.getPincode());
                logger.info("Selected Address: {} {}, {}, {}, {}, {}, {}, {}, {}", 
                    shippingAddress.getFullName(), shippingAddress.getPhone(), shippingAddress.getHouseNo(), shippingAddress.getStreet(), 
                    shippingAddress.getArea(), shippingAddress.getCity(), shippingAddress.getState(), shippingAddress.getCountry(), shippingAddress.getPincode());
                logger.info("Address validation result: SUCCESS");
            } else {
                logger.warn("Address validation result: FAILED - Missing address");
            }

            Order savedOrder = orderRepository.save(order);
            logger.info("Order ID: {}", savedOrder.getOrderId());

            // Save Order Items
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

            // Save Payment Details (with PENDING status for COD)
            Payment payment = new Payment(
                    "COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    savedOrder,
                    "COD",
                    "COD",
                    grandTotal,
                    "INR",
                    "PENDING"
            );
            paymentRepository.save(payment);

            // Clear Cart
            cartItemRepository.deleteByUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "COD order placed successfully");
            response.put("orderId", savedOrder.getOrderId());
            response.put("paymentId", payment.getPaymentId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to place COD order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
