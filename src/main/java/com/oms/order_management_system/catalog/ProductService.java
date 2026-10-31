package com.oms.order_management_system.catalog;

import com.oms.order_management_system.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (request.sku() == null || request.sku().isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be greater than zero");
        }

        if (repository.findBySku(request.sku()).isPresent()) {
            throw new IllegalArgumentException("Product with sku " + request.sku() + " already exists");
        }

        Product product = Product.builder()
                .sku(request.sku().trim())
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .active(true)
                .build();

        return ProductResponse.from(repository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ProductResponse.from(product);
    }
}
