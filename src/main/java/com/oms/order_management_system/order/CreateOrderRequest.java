package com.oms.order_management_system.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "CreateOrderRequest", description = "Request payload used to place a new order for a product.")
public record CreateOrderRequest(
        @Schema(description = "Identifier of the customer placing the order.", example = "CUST-1024", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String customerId,

        @Schema(description = "Product identifier to include in the order.", example = "0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID productId,

        @Schema(description = "Quantity requested for the selected product.", example = "2", minimum = "1")
        @Min(1) int quantity
) {
}
