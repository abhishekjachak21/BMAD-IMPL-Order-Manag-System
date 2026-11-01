package com.oms.order_management_system.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "CreateProductRequest", description = "Request payload used to create a new catalog product.")
public record CreateProductRequest(
        @Schema(description = "Unique stock-keeping unit for the product.", example = "SKU-1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sku,

        @Schema(description = "Display name of the product shown in the catalog.", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String name,

        @Schema(description = "Optional marketing description or notes for the product.", example = "Ergonomic wireless mouse with a long battery life.")
        String description,

        @Schema(description = "Selling price of the product. Must be greater than zero.", example = "49.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price
) {
}
