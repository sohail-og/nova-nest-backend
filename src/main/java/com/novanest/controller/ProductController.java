package com.novanest.controller;

import com.novanest.model.Product;
import com.novanest.repository.ProductRepository;
import com.novanest.service.ProductImageHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductImageHelper productImageHelper;

    public ProductController(ProductRepository productRepository, ProductImageHelper productImageHelper) {
        this.productRepository = productRepository;
        this.productImageHelper = productImageHelper;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        productImageHelper.populateImageUrl(products);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return productRepository.findById(id)
                .map(product -> {
                    productImageHelper.populateImageUrl(product);
                    return ResponseEntity.ok(product);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
