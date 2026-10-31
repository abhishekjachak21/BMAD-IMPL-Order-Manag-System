package com.oms.order_management_system.inventory;

import com.oms.order_management_system.catalog.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private int committedQuantity;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onWrite() {
        updatedAt = Instant.now();
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserved quantity must be greater than zero");
        }
        if (availableQuantity < quantity) {
            throw new IllegalStateException("Insufficient available inventory for product " + product.getId());
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be greater than zero");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more reserved inventory than exists");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
}
