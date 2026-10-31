package com.oms.order_management_system.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price
) {
}
