package com.novanest.controller;

import com.novanest.dto.AuthResponse;
import com.novanest.exception.ValidationException;
import com.novanest.model.*;
import com.novanest.repository.*;
import com.novanest.security.CustomUserDetailsService;
import com.novanest.service.JwtService;
import com.novanest.service.ProductImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.novanest.service.ReportExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private static final Logger log = LoggerFactory.getLogger(AdminController.class);

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final CategoryRepository categoryRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartItemRepository cartItemRepository;
	private final WishlistItemRepository wishlistItemRepository;
	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final ProductImageHelper productImageHelper;
	private final ReportExcelService reportExcelService;

	private final JwtTokenRepository jwtTokenRepository;
	private final OtpTokenRepository otpTokenRepository;

	public AdminController(UserRepository userRepository, OrderRepository orderRepository,
						   ProductRepository productRepository, ProductImageRepository productImageRepository,
						   CategoryRepository categoryRepository, OrderItemRepository orderItemRepository,
						   CartItemRepository cartItemRepository, WishlistItemRepository wishlistItemRepository,
						   JwtService jwtService, CustomUserDetailsService customUserDetailsService,
						   PasswordEncoder passwordEncoder, ProductImageHelper productImageHelper,
						   ReportExcelService reportExcelService,
						   JwtTokenRepository jwtTokenRepository, OtpTokenRepository otpTokenRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.productImageRepository = productImageRepository;
		this.categoryRepository = categoryRepository;
		this.orderItemRepository = orderItemRepository;
		this.cartItemRepository = cartItemRepository;
		this.wishlistItemRepository = wishlistItemRepository;
		this.jwtService = jwtService;
		this.customUserDetailsService = customUserDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.productImageHelper = productImageHelper;
		this.reportExcelService = reportExcelService;
		this.jwtTokenRepository = jwtTokenRepository;
		this.otpTokenRepository = otpTokenRepository;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request, jakarta.servlet.http.HttpServletResponse response) {
		String email = request.get("email");
		String password = request.get("password");

		if (email == null || password == null) {
			log.error("Authentication failure: Email and password are required");
			throw new ValidationException("Email and password are required");
		}

		log.info("Admin login attempt for email: {}", email);

		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty()) {
			log.warn("Authentication failure: Email not found: {}", email);
			throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Admin not found");
		}

		User user = userOpt.get();
		log.info("Email found: {}. Detected role: {}", email, user.getRole());

		if (user.getRole() != Role.ADMIN) {
			log.warn("Authentication failure: Access denied for role: {} (email: {})", user.getRole(), email);
			throw new org.springframework.security.authentication.DisabledException("Access denied: Not an Admin");
		}

		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
		log.info("Password match result: {}", passwordMatches);

		if (!passwordMatches) {
			log.warn("Authentication failure: Password mismatch for admin: {}", email);
			throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
		}

		log.info("Authentication success for admin: {}", email);

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
		String token = jwtService.generateToken(userDetails);
		jwtService.saveUserToken(token, userDetails.getUsername());

		AuthResponse authResponse = new AuthResponse();
		authResponse.setToken(token);
		authResponse.setUsername(user.getUsername());
		authResponse.setMessage("Admin login successful");

		org.springframework.http.ResponseCookie cookie = jwtService.createJwtCookie(token);
		response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());

		return ResponseEntity.ok(authResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logout(
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			@CookieValue(value = "jwt", required = false) String jwtCookie,
			jakarta.servlet.http.HttpServletResponse response) {
		
		String token = null;
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		} else if (jwtCookie != null) {
			token = jwtCookie;
		}

		if (token != null) {
			jwtService.revokeToken(token);
		}

		org.springframework.http.ResponseCookie cookie = jwtService.createCleanJwtCookie();
		response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());

		AuthResponse responseBody = new AuthResponse();
		responseBody.setMessage("Logged out successfully");
		return ResponseEntity.ok(responseBody);
	}

	@GetMapping("/dashboard/stats")
	public ResponseEntity<Map<String, Object>> getDashboardStats() {
		List<Order> allOrders = orderRepository.findAll();
		List<User> allUsers = userRepository.findAll();
		List<Product> allProducts = productRepository.findAll();
		List<Category> allCategories = categoryRepository.findAll();

		// Calculate total revenue (SUCCESS orders)
		BigDecimal totalRevenue = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.SUCCESS)
				.map(Order::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		long pendingOrdersCount = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.PENDING)
				.count();

		long deliveredOrdersCount = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.SUCCESS)
				.count();

		long cancelledOrdersCount = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.FAILED)
				.count();

		long activeUsers = allUsers.stream()
				.filter(u -> u.getRole() == Role.CUSTOMER)
				.count();

		// Dynamic revenue details
		LocalDateTime now = LocalDateTime.now();
		BigDecimal todayRevenue = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(now.toLocalDate()))
				.map(Order::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal monthlyRevenue = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().getYear() == now.getYear() && o.getCreatedAt().getMonth() == now.getMonth())
				.map(Order::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal yearlyRevenue = allOrders.stream()
				.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().getYear() == now.getYear())
				.map(Order::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Growth metrics (mock dynamic growth relative to previous values)
		double revenueGrowth = 14.5; // Default positive trend
		double salesGrowth = 8.2; // Default positive trend

		// Top 5 recent orders
		List<Order> recentOrders = allOrders.stream()
				.sorted((o1, o2) -> {
					if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
						return o2.getCreatedAt().compareTo(o1.getCreatedAt());
					}
					return o2.getOrderId().compareTo(o1.getOrderId());
				})
				.limit(5)
				.collect(Collectors.toList());

		// Top 5 recent users
		List<User> recentUsers = allUsers.stream()
				.filter(u -> u.getRole() == Role.CUSTOMER)
				.sorted((u1, u2) -> u2.getId().compareTo(u1.getId()))
				.limit(5)
				.collect(Collectors.toList());

		// Calculate top selling products and categories
		List<OrderItem> allOrderItems = orderItemRepository.findAll();
		Map<Product, Integer> productSales = new HashMap<>();
		for (OrderItem item : allOrderItems) {
			if (item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.SUCCESS) {
				Product p = item.getProduct();
				if (p != null) {
					productSales.put(p, productSales.getOrDefault(p, 0) + item.getQuantity());
				}
			}
		}

		List<Map<String, Object>> topProducts = productSales.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(5)
				.map(e -> {
					Map<String, Object> map = new HashMap<>();
					map.put("id", e.getKey().getId());
					map.put("name", e.getKey().getName());
					map.put("quantity", e.getValue());
					map.put("price", e.getKey().getPrice());
					map.put("totalSales", e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())));
					return map;
				})
				.collect(Collectors.toList());

		Map<Category, Integer> categorySales = new HashMap<>();
		for (OrderItem item : allOrderItems) {
			if (item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.SUCCESS) {
				Product p = item.getProduct();
				if (p != null && p.getCategory() != null) {
					Category c = p.getCategory();
					categorySales.put(c, categorySales.getOrDefault(c, 0) + item.getQuantity());
				}
			}
		}

		List<Map<String, Object>> topCategories = categorySales.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(5)
				.map(e -> {
					Map<String, Object> map = new HashMap<>();
					map.put("id", e.getKey().getId());
					map.put("name", e.getKey().getCategoryName());
					map.put("quantity", e.getValue());
					return map;
				})
				.collect(Collectors.toList());

		Map<String, Object> stats = new HashMap<>();
		stats.put("totalRevenue", totalRevenue);
		stats.put("todayRevenue", todayRevenue);
		stats.put("monthlyRevenue", monthlyRevenue);
		stats.put("yearlyRevenue", yearlyRevenue);
		stats.put("totalOrders", allOrders.size());
		stats.put("pendingOrders", pendingOrdersCount);
		stats.put("deliveredOrders", deliveredOrdersCount);
		stats.put("cancelledOrders", cancelledOrdersCount);
		stats.put("totalUsers", activeUsers);
		stats.put("totalProducts", allProducts.size());
		stats.put("totalCategories", allCategories.size());
		stats.put("revenueGrowth", revenueGrowth);
		stats.put("salesGrowth", salesGrowth);
		stats.put("recentOrders", recentOrders);
		stats.put("recentUsers", recentUsers);
		stats.put("topSellingProducts", topProducts);
		stats.put("topCategories", topCategories);

		// Monthly sales analytics graph data (dynamic based on actual historical orders)
		List<Map<String, Object>> monthlyAnalytics = new ArrayList<>();
		String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
		for (int i = 0; i < 6; i++) {
			int monthVal = (LocalDateTime.now().getMonthValue() - 5 + i + 12) % 12;
			if (monthVal == 0) monthVal = 12;
			final int m = monthVal;
			BigDecimal monthRevenue = allOrders.stream()
					.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().getMonthValue() == m)
					.map(Order::getTotalAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			long monthOrders = allOrders.stream()
					.filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getMonthValue() == m)
					.count();

			Map<String, Object> point = new HashMap<>();
			point.put("month", months[m - 1]);
			point.put("revenue", monthRevenue);
			point.put("orders", monthOrders);
			monthlyAnalytics.add(point);
		}
		stats.put("monthlyAnalytics", monthlyAnalytics);

		// Chart.js data
		List<Map<String, Object>> dailyAnalytics = new ArrayList<>();
		for (int i = 6; i >= 0; i--) {
			LocalDateTime targetDay = now.minusDays(i);
			BigDecimal dayRevenue = allOrders.stream()
					.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(targetDay.toLocalDate()))
					.map(Order::getTotalAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			Map<String, Object> point = new HashMap<>();
			point.put("date", targetDay.toLocalDate().toString());
			point.put("revenue", dayRevenue);
			dailyAnalytics.add(point);
		}
		stats.put("dailyAnalytics", dailyAnalytics);

		List<Map<String, Object>> yearlyAnalytics = new ArrayList<>();
		for (int i = 4; i >= 0; i--) {
			int targetYear = now.getYear() - i;
			BigDecimal yearRevenue = allOrders.stream()
					.filter(o -> o.getStatus() == OrderStatus.SUCCESS && o.getCreatedAt() != null && o.getCreatedAt().getYear() == targetYear)
					.map(Order::getTotalAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			Map<String, Object> point = new HashMap<>();
			point.put("year", String.valueOf(targetYear));
			point.put("revenue", yearRevenue);
			yearlyAnalytics.add(point);
		}
		stats.put("yearlyAnalytics", yearlyAnalytics);

		return ResponseEntity.ok(stats);
	}

	@GetMapping("/users")
	public ResponseEntity<List<User>> getAllUsers() {
		// Return all users so admin can edit them, regardless of role
		List<User> users = userRepository.findAll();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/orders")
	public ResponseEntity<List<Order>> getAllOrders() {
		return ResponseEntity.ok(orderRepository.findAll());
	}

	@PostMapping("/products")
	public ResponseEntity<Product> createProduct(@RequestBody Map<String, Object> body) {
		String name = (String) body.get("name");
		String description = (String) body.get("description");
		BigDecimal price = new BigDecimal(body.get("price").toString());
		Integer stock = Integer.parseInt(body.get("stock").toString());
		Integer categoryId = Integer.parseInt(body.get("categoryId").toString());
		String imageUrl = (String) body.get("imageUrl");

		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ValidationException("Category not found"));

		Product product = new Product();
		product.setName(name);
		product.setDescription(description);
		product.setPrice(price);
		product.setStock(stock);
		product.setCategory(category);
		Product savedProduct = productRepository.save(product);

		if (imageUrl != null && !imageUrl.trim().isEmpty()) {
			ProductImage pImg = new ProductImage();
			pImg.setProduct(savedProduct);
			pImg.setImageUrl(imageUrl);
			productImageRepository.save(pImg);
			savedProduct.setImageUrl(imageUrl);
		}

		return ResponseEntity.ok(savedProduct);
	}

	@PutMapping("/products/{id}")
	public ResponseEntity<Product> updateProduct(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ValidationException("Product not found"));

		if (body.containsKey("name")) product.setName((String) body.get("name"));
		if (body.containsKey("description")) product.setDescription((String) body.get("description"));
		if (body.containsKey("price")) product.setPrice(new BigDecimal(body.get("price").toString()));
		if (body.containsKey("stock")) product.setStock(Integer.parseInt(body.get("stock").toString()));

		if (body.containsKey("categoryId")) {
			Integer categoryId = Integer.parseInt(body.get("categoryId").toString());
			Category category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new ValidationException("Category not found"));
			product.setCategory(category);
		}

		Product savedProduct = productRepository.save(product);

		if (body.containsKey("imageUrl")) {
			String imageUrl = (String) body.get("imageUrl");
			List<ProductImage> pImgs = productImageRepository.findByProductId(id);
			if (pImgs != null && !pImgs.isEmpty()) {
				ProductImage pImg = pImgs.get(0);
				pImg.setImageUrl(imageUrl);
				productImageRepository.save(pImg);
			} else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
				ProductImage pImg = new ProductImage();
				pImg.setProduct(savedProduct);
				pImg.setImageUrl(imageUrl);
				productImageRepository.save(pImg);
			}
			savedProduct.setImageUrl(imageUrl);
		} else {
			productImageHelper.populateImageUrl(savedProduct);
		}

		return ResponseEntity.ok(savedProduct);
	}

	@DeleteMapping("/products/{id}")
	public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Integer id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ValidationException("Product not found"));

		// Delete product images first
		List<ProductImage> pImgs = productImageRepository.findByProductId(id);
		productImageRepository.deleteAll(pImgs);

		productRepository.delete(product);

		Map<String, String> response = new HashMap<>();
		response.put("message", "Product deleted successfully");
		return ResponseEntity.ok(response);
	}

	@PutMapping("/categories/{id}")
	public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody Category categoryDetails) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ValidationException("Category not found"));

		if (categoryDetails.getCategoryName() != null) {
			category.setCategoryName(categoryDetails.getCategoryName());
		}
		if (categoryDetails.getCategoryImage() != null) {
			category.setCategoryImage(categoryDetails.getCategoryImage());
		}
		if (categoryDetails.getDescription() != null) {
			category.setDescription(categoryDetails.getDescription());
		}
		if (categoryDetails.getBannerImage() != null) {
			category.setBannerImage(categoryDetails.getBannerImage());
		}
		if (categoryDetails.getDisplayOrder() != null) {
			category.setDisplayOrder(categoryDetails.getDisplayOrder());
		}
		if (categoryDetails.getVisibility() != null) {
			category.setVisibility(categoryDetails.getVisibility());
		}

		Category updatedCategory = categoryRepository.save(category);
		return ResponseEntity.ok(updatedCategory);
	}

	@PostMapping("/categories")
	public ResponseEntity<Category> createCategory(@RequestBody Category categoryDetails) {
		if (categoryDetails.getCategoryName() == null || categoryDetails.getCategoryName().trim().isEmpty()) {
			throw new ValidationException("Category name is required");
		}
		Category category = new Category();
		category.setCategoryName(categoryDetails.getCategoryName());
		category.setCategoryImage(categoryDetails.getCategoryImage());
		category.setDescription(categoryDetails.getDescription());
		category.setBannerImage(categoryDetails.getBannerImage());
		category.setDisplayOrder(categoryDetails.getDisplayOrder() != null ? categoryDetails.getDisplayOrder() : 0);
		category.setVisibility(categoryDetails.getVisibility() != null ? categoryDetails.getVisibility() : true);
		
		Category saved = categoryRepository.save(category);
		return ResponseEntity.ok(saved);
	}

	@DeleteMapping("/categories/{id}")
	public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Integer id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ValidationException("Category not found"));
		List<Product> products = productRepository.findAll().stream()
				.filter(p -> p.getCategory() != null && p.getCategory().getId().equals(id))
				.collect(Collectors.toList());
		if (!products.isEmpty()) {
			throw new ValidationException("Cannot delete category. It is currently assigned to " + products.size() + " products.");
		}
		categoryRepository.delete(category);
		Map<String, String> response = new HashMap<>();
		response.put("message", "Category deleted successfully");
		return ResponseEntity.ok(response);
	}

	@PostMapping("/users")
	public ResponseEntity<User> createUser(@RequestBody Map<String, Object> body) {
		String username = (String) body.get("username");
		String email = (String) body.get("email");
		String phone = (String) body.get("phone");
		String gender = (String) body.get("gender");
		String password = (String) body.get("password");
		String roleStr = (String) body.get("role");
		String status = (String) body.get("status");
		String address = (String) body.get("address");
		String profileImage = (String) body.get("profileImage");

		if (username == null || email == null || phone == null || password == null) {
			throw new ValidationException("Username, email, phone, and password are required");
		}
		if (userRepository.existsByUsername(username)) {
			throw new ValidationException("Username already exists");
		}
		if (userRepository.existsByEmail(email)) {
			throw new ValidationException("Email already exists");
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPhone(phone);
		user.setGender(gender != null ? gender : "Not Specified");
		user.setPassword(passwordEncoder.encode(password));
		user.setRole(roleStr != null ? Role.valueOf(roleStr.toUpperCase()) : Role.CUSTOMER);
		user.setStatus(status != null ? status : "ACTIVE");
		user.setProfileImage(profileImage);

		User saved = userRepository.save(user);
		return ResponseEntity.ok(saved);
	}

	@PutMapping("/users/{id}")
	public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ValidationException("User not found"));

		if (body.containsKey("username")) {
			String username = (String) body.get("username");
			if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
				throw new ValidationException("Username already exists");
			}
			user.setUsername(username);
		}
		if (body.containsKey("email")) {
			String email = (String) body.get("email");
			if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
				throw new ValidationException("Email already exists");
			}
			user.setEmail(email);
		}
		if (body.containsKey("phone")) {
			String phone = (String) body.get("phone");
			if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
				throw new ValidationException("Phone number already exists");
			}
			user.setPhone(phone);
		}
		if (body.containsKey("gender")) {
			user.setGender((String) body.get("gender"));
		}
		if (body.containsKey("profileImage")) {
			user.setProfileImage((String) body.get("profileImage"));
		}
		if (body.containsKey("status")) {
			user.setStatus((String) body.get("status"));
		}
		if (body.containsKey("role")) {
			String roleStr = (String) body.get("role");
			user.setRole(Role.valueOf(roleStr.toUpperCase()));
		}
		if (body.containsKey("password") && body.get("password") != null && !body.get("password").toString().trim().isEmpty()) {
			user.setPassword(passwordEncoder.encode(body.get("password").toString()));
		}

		User updatedUser = userRepository.save(user);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/users/{id}")
	@org.springframework.transaction.annotation.Transactional
	public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Integer id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ValidationException("User not found"));

		List<Order> userOrders = orderRepository.findByUser(user);
		Map<String, String> response = new HashMap<>();

		// Clear user's active tokens to prevent foreign key constraint issues
		jwtTokenRepository.deleteByUser(user);
		otpTokenRepository.deleteByUser(user);

		if (!userOrders.isEmpty()) {
			// Soft delete logic: change status and clear personal data but retain ID for order history
			user.setStatus("DELETED");
			user.setEmail("deleted_" + user.getId() + "@novanest.com");
			user.setUsername("deleted_user_" + user.getId());
			user.setFullName("Deleted User");
			user.setPhone("0000000000");
			user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
			userRepository.save(user);
			
			// Still remove their cart and wishlist since they are DELETED
			cartItemRepository.deleteByUser(user);
			wishlistItemRepository.deleteByUser(user);
			
			response.put("message", "User soft-deleted to preserve order history");
		} else {
			// Hard cascade delete
			cartItemRepository.deleteByUser(user);
			wishlistItemRepository.deleteByUser(user);
			userRepository.delete(user);
			response.put("message", "User completely deleted");
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/reports/daily/excel")
	public ResponseEntity<byte[]> getDailyReportExcel() {
		try {
			log.info("Generating daily business report Excel");
			java.time.LocalDate today = java.time.LocalDate.now();
			List<Order> allOrders = orderRepository.findAll();
			List<Order> todayOrders = allOrders.stream()
					.filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(today))
					.collect(Collectors.toList());

			List<OrderItem> todayOrderItems = new ArrayList<>();
			for (Order order : todayOrders) {
				List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());
				todayOrderItems.addAll(items);
			}

			byte[] excelBytes = reportExcelService.generateDailyReportExcel(todayOrders, todayOrderItems);

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-report-" + today.toString() + ".xlsx")
					.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.body(excelBytes);
		} catch (Exception e) {
			log.error("Error generating daily report", e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
