package com.oms.order_management_system.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "ProductResponse", description = "Product details returned after creation or lookup.")
public record ProductResponse(
        @Schema(description = "System-generated product identifier.", example = "0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d")
        UUID id,

        @Schema(description = "Unique product SKU", example = "SKU-1001")
        String sku,

        @Schema(description = "Catalog name for the product", example = "Wireless Mouse")
        String name,

        @Schema(description = "Product description or marketing copy", example = "Ergonomic wireless mouse with a long battery life.")
        String description,

        @Schema(description = "Current price for the product in the catalog", example = "49.99")
        BigDecimal price,

        @Schema(description = "Whether the product is currently active and sellable", example = "true")
        boolean active) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(), product.getPrice(), product.isActive());
    }
}
