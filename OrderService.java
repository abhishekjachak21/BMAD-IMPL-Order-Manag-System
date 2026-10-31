package com.oms.order_management_system.order;

import com.oms.order_management_system.catalog.Product;
import com.oms.order_management_system.catalog.ProductRepository;
import com.oms.order_management_system.inventory.InventoryReservation;
import com.oms.order_management_system.inventory.InventoryService;
import com.oms.order_management_system.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        if (!product.isActive()) {
            throw new IllegalStateException("Product is not active");
        }

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID())
                .customerId(request.customerId())
                .status(OrderStatus.RESERVED)
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(request.quantity())
                .unitPrice(product.getPrice())
                .lineTotal(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .build();

        order.addItem(orderItem);
        order = orderRepository.save(order);

        InventoryReservation reservation = inventoryService.reserveForOrder(order, request.productId(), request.quantity());
        order.addReservation(reservation);

        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.FULFILLED || order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order cannot be cancelled from status " + order.getStatus());
        }

        inventoryService.releaseReservationForOrder(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse markOrderPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setStatus(OrderStatus.PAID);
        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse markOrderPaymentVerificationRequired(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setStatus(OrderStatus.PAYMENT_VERIFICATION_REQUIRED);
        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }
}
