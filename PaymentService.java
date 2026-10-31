package com.oms.order_management_system.payment;

import com.oms.order_management_system.order.Order;
import com.oms.order_management_system.order.OrderRepository;
import com.oms.order_management_system.order.OrderStatus;
import com.oms.order_management_system.shared.ConflictException;
import com.oms.order_management_system.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SimulatedPaymentGateway simulatedPaymentGateway;

    public PaymentService(PaymentRepository paymentRepository,
                         OrderRepository orderRepository,
                         SimulatedPaymentGateway simulatedPaymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.simulatedPaymentGateway = simulatedPaymentGateway;
    }

    @Transactional
    public PaymentResponse processPayment(UUID orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("amount must match the order total: " + order.getTotalAmount());
        }

        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }

        Payment existingByKey = paymentRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existingByKey != null) {
            return PaymentResponse.from(existingByKey, "Returned existing payment operation for same idempotency key");
        }

        Payment activePayment = paymentRepository.findLatestActiveForOrder(orderId,
                List.of(PaymentStatus.INITIATED, PaymentStatus.UNRESOLVED)).orElse(null);

        if (activePayment != null) {
            throw new ConflictException("Payment already in progress for order " + orderId + ". Existing operation status: " + activePayment.getStatus());
        }

        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey(request.idempotencyKey())
                .amount(request.amount())
                .status(PaymentStatus.INITIATED)
                .build();
        payment = paymentRepository.save(payment);

        SimulatedPaymentGateway.PaymentProviderResult providerResult = simulatedPaymentGateway.process(payment, request.providerMode());
        payment.setProviderStatus(providerResult.providerStatus());
        payment.setStatus(providerResult.status());
        payment.setFailureReason(providerResult.message());

        if (providerResult.status() == PaymentStatus.CAPTURED) {
            order.setStatus(OrderStatus.PAID);
            payment.setProviderReference("simulated-provider-" + UUID.randomUUID());
        } else if (providerResult.status() == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.RECONCILIATION_REQUIRED);
        } else {
            order.setStatus(OrderStatus.PAYMENT_VERIFICATION_REQUIRED);
        }

        orderRepository.save(order);
        payment = paymentRepository.save(payment);
        return PaymentResponse.from(payment, providerResult.message());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getLatestPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Payment payment = paymentRepository.findLatestActiveForOrder(orderId,
                List.of(PaymentStatus.INITIATED, PaymentStatus.UNRESOLVED, PaymentStatus.CAPTURED, PaymentStatus.FAILED)).orElseGet(() ->
                paymentRepository.findAll().stream()
                        .filter(p -> p.getOrder().getId().equals(orderId))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No payment found for order: " + orderId))
        );

        return PaymentResponse.from(payment, "Latest payment state for order " + orderId);
    }
}
