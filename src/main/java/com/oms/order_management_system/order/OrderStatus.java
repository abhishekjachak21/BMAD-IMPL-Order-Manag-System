package com.oms.order_management_system.order;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    RESERVED,
    PAYMENT_PENDING,
    PAYMENT_VERIFICATION_REQUIRED,
    PAID,
    FULFILLMENT_PENDING,
    DISPATCHED,
    DELIVERED,
    RECONCILIATION_REQUIRED,
    CANCELLED,
    FAILED,
    FULFILLED
}
