package com.novanest.service;

import com.novanest.model.Product;
import com.novanest.model.ProductImage;
import com.novanest.repository.ProductImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductImageHelper {

    private final ProductImageRepository productImageRepository;

    public ProductImageHelper(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    public void populateImageUrl(Product product) {
        if (product == null) return;
        List<ProductImage> images = productImageRepository.findByProductId(product.getId());
        if (images != null && !images.isEmpty()) {
            product.setImageUrl(images.get(0).getImageUrl());
        } else {
            product.setImageUrl(null);
        }
    }

    public void populateImageUrl(List<Product> products) {
        if (products == null) return;
        for (Product product : products) {
            populateImageUrl(product);
        }
    }
}
