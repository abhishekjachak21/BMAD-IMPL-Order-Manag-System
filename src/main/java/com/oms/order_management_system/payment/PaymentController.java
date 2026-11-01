package com.oms.order_management_system.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Payments", description = "Payment processing and retrieval for orders")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Process an order payment",
            description = "Creates a payment for a specific order using a simulated gateway and an idempotency key to protect duplicate submissions.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Payment processed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaymentResponse.class),
                                    examples = @ExampleObject(name = "Payment created", value = "{\n  \"paymentId\": \"735b14f0-738d-4d53-b1ff-3154d4dbd5f1\",\n  \"orderId\": \"4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615\",\n  \"idempotencyKey\": \"pay-9b67f82c-4476-4ad7-b1d9-fb4cfbbc8df7\",\n  \"amount\": 99.98,\n  \"status\": \"CAPTURED\",\n  \"providerStatus\": \"APPROVED\",\n  \"message\": \"Payment processed successfully.\",\n  \"createdAt\": \"2026-11-01T14:15:22Z\"\n}"))),
                    @ApiResponse(responseCode = "400", description = "Payment request is invalid or unsupported", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Duplicate payment idempotency key or conflicting state", content = @Content(mediaType = "application/json"))
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = PaymentRequest.class),
            examples = @ExampleObject(name = "Create payment payload", value = "{\n  \"idempotencyKey\": \"pay-9b67f82c-4476-4ad7-b1d9-fb4cfbbc8df7\",\n  \"amount\": 99.98,\n  \"providerMode\": \"SUCCESS\"\n}")))
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable UUID orderId,
                                                        @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(orderId, request));
    }

    @Operation(
            summary = "Get latest payment",
            description = "Fetches the most recent payment associated with an order.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Latest payment returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaymentResponse.class),
                                    examples = @ExampleObject(name = "Latest payment", value = "{\n  \"paymentId\": \"735b14f0-738d-4d53-b1ff-3154d4dbd5f1\",\n  \"orderId\": \"4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615\",\n  \"idempotencyKey\": \"pay-9b67f82c-4476-4ad7-b1d9-fb4cfbbc8df7\",\n  \"amount\": 99.98,\n  \"status\": \"CAPTURED\",\n  \"providerStatus\": \"APPROVED\",\n  \"message\": \"Payment processed successfully.\",\n  \"createdAt\": \"2026-11-01T14:15:22Z\"\n}"))),
                    @ApiResponse(responseCode = "404", description = "No payment exists for the order", content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/orders/{orderId}/payments/latest")
    public ResponseEntity<PaymentResponse> getLatestPayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getLatestPayment(orderId));
    }
}
