package com.oms.order_management_system.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PaymentResponse", description = "Payment processing result returned by the payment API.")
public record PaymentResponse(
        @Schema(description = "Unique payment identifier.", example = "735b14f0-738d-4d53-b1ff-3154d4dbd5f1")
        UUID paymentId,

        @Schema(description = "Order affected by the payment.", example = "4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615")
        UUID orderId,

        @Schema(description = "Idempotency key used for the request.", example = "pay-9b67f82c-4476-4ad7-b1d9-fb4cfbbc8df7")
        String idempotencyKey,

        @Schema(description = "Amount charged for the payment.", example = "99.98")
        BigDecimal amount,

        @Schema(description = "Payment processing status.", example = "CAPTURED")
        String status,

        @Schema(description = "Provider-level status returned by the simulated gateway.", example = "APPROVED")
        String providerStatus,

        @Schema(description = "Human-readable outcome message from the payment flow.", example = "Payment processed successfully.")
        String message,

        @Schema(description = "Instant at which the payment record was created.", example = "2026-11-01T14:15:22Z")
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
