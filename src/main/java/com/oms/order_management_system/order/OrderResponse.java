package com.oms.order_management_system.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID orderId, String orderNumber, String customerId, String status, BigDecimal totalAmount) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus().name(), order.getTotalAmount());
    }
}
