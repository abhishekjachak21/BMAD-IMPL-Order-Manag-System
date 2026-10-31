package com.oms.order_management_system.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID paymentId,
                             UUID orderId,
                             String idempotencyKey,
                             BigDecimal amount,
                             String status,
                             String providerStatus,
                             String message,
                             Instant createdAt) {
    public static PaymentResponse from(Payment payment, String message) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getProviderStatus(),
                message,
                payment.getCreatedAt()
        );
    }
}
