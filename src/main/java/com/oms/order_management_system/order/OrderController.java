package com.oms.order_management_system.order;

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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order lifecycle operations including creation, lookup, status, and cancellation")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create a new order",
            description = "Creates an order for a single product and reserves inventory as part of the order placement workflow.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class),
                                    examples = @ExampleObject(name = "Created order", value = "{\n  \"orderId\": \"4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615\",\n  \"orderNumber\": \"OMS-2026-000123\",\n  \"customerId\": \"CUST-1024\",\n  \"status\": \"CREATED\",\n  \"totalAmount\": 99.98\n}"))),
                    @ApiResponse(responseCode = "400", description = "Request validation or bad input", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Selected product or inventory record was not found", content = @Content(mediaType = "application/json"))
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = CreateOrderRequest.class),
            examples = @ExampleObject(name = "Create order payload", value = "{\n  \"customerId\": \"CUST-1024\",\n  \"productId\": \"0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d\",\n  \"quantity\": 2\n}")))
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(request));
    }

    @Operation(
            summary = "Get an order",
            description = "Returns the current order summary for the supplied order identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class),
                                    examples = @ExampleObject(name = "Order lookup", value = "{\n  \"orderId\": \"4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615\",\n  \"orderNumber\": \"OMS-2026-000123\",\n  \"customerId\": \"CUST-1024\",\n  \"status\": \"CREATED\",\n  \"totalAmount\": 99.98\n}"))),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getOrder(id));
    }

    @Operation(
            summary = "Get order status",
            description = "Returns the current status string for a single order as a lightweight status payload.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(name = "Status payload", value = "{\n  \"status\": \"CREATED\"\n}"))),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("status", service.getOrder(id).status()));
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels an existing order and updates its lifecycle state. This operation may fail if the order is not cancellable in its current state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class),
                                    examples = @ExampleObject(name = "Cancelled order", value = "{\n  \"orderId\": \"4d6eebc7-f8d9-47f6-8b46-0d7ff2ab0615\",\n  \"orderNumber\": \"OMS-2026-000123\",\n  \"customerId\": \"CUST-1024\",\n  \"status\": \"CANCELLED\",\n  \"totalAmount\": 99.98\n}"))),
                    @ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json"))
            }
    )
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelOrder(id));
    }
}
