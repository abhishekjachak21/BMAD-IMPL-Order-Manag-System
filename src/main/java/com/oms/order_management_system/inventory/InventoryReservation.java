package com.oms.order_management_system.inventory;

import com.oms.order_management_system.catalog.Product;
import com.oms.order_management_system.order.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryReservationStatus status;

    @Column(nullable = false)
    private Instant reservedAt;

    @Column
    private Instant expiresAt;

    @Column
    private Instant releasedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        reservedAt = Instant.now();
    }
}
