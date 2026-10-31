package com.oms.order_management_system.payment;

import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway {
    public PaymentProviderResult process(Payment payment, PaymentProviderMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("providerMode is required");
        }

        return switch (mode) {
            case SUCCESS -> new PaymentProviderResult(PaymentStatus.CAPTURED, "CAPTURED", "Approved by simulated provider");
            case FAILURE -> new PaymentProviderResult(PaymentStatus.FAILED, "DECLINED", "The simulated payment provider declined the transaction");
            case TIMEOUT -> new PaymentProviderResult(PaymentStatus.UNRESOLVED, "TIMEOUT", "The provider timed out; payment remains unresolved");
            case UNKNOWN -> new PaymentProviderResult(PaymentStatus.UNRESOLVED, "UNKNOWN", "The provider returned an unknown status; manual verification required");
        };
    }

    public record PaymentProviderResult(PaymentStatus status, String providerStatus, String message) {
    }
}
