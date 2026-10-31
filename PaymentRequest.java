package com.oms.order_management_system.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String idempotencyKey,
        @NotNull BigDecimal amount,
        @NotNull PaymentProviderMode providerMode
) {
}
