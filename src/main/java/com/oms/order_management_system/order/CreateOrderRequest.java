package com.oms.order_management_system.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotNull UUID productId,
        @Min(1) int quantity
) {
}
