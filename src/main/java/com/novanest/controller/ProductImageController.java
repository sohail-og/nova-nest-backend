package com.novanest.controller;

import com.novanest.model.ProductImage;
import com.novanest.repository.ProductImageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productimages")
public class ProductImageController {

    private final ProductImageRepository productImageRepository;

    public ProductImageController(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductImage>> getAllProductImages() {
        return ResponseEntity.ok(productImageRepository.findAll());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImage>> getImagesByProductId(@PathVariable Integer productId) {
        return ResponseEntity.ok(productImageRepository.findByProductId(productId));
    }
}
