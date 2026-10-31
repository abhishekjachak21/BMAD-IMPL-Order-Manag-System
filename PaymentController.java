package com.oms.order_management_system.payment;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable UUID orderId,
                                                        @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(orderId, request));
    }

    @GetMapping("/orders/{orderId}/payments/latest")
    public ResponseEntity<PaymentResponse> getLatestPayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getLatestPayment(orderId));
    }
}
