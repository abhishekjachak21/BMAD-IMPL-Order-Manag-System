package com.oms.order_management_system.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "PaymentRequest", description = "Payment request submitted for a specific order.")
public record PaymentRequest(
        @Schema(description = "Unique idempotency key to safely retry the same payment request.", example = "pay-9b67f82c-4476-4ad7-b1d9-fb4cfbbc8df7", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String idempotencyKey,

        @Schema(description = "Amount to charge against the order total.", example = "99.98", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull BigDecimal amount,

        @Schema(description = "Provider mode used to simulate the payment gateway outcome.", example = "SUCCESS", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull PaymentProviderMode providerMode
) {
}
