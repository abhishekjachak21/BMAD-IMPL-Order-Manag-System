package com.oms.order_management_system.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "OrderResponse", description = "Order summary returned by order-related endpoints.")
public record OrderResponse(
        @Schema(description = "System-generated order identifier.", example = "4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615")
        UUID orderId,

        @Schema(description = "Human-readable order number used for business reference.", example = "OMS-2026-000123")
        String orderNumber,

        @Schema(description = "Customer identifier associated with the order.", example = "CUST-1024")
        String customerId,

        @Schema(description = "Current life-cycle status of the order.", example = "CREATED")
        String status,

        @Schema(description = "Total monetary value of the order.", example = "99.98")
        BigDecimal totalAmount) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus().name(), order.getTotalAmount());
    }
}
