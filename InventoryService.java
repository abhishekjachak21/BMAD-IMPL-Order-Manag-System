package com.oms.order_management_system.inventory;

import com.oms.order_management_system.catalog.Product;
import com.oms.order_management_system.catalog.ProductRepository;
import com.oms.order_management_system.order.Order;
import com.oms.order_management_system.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryReservationRepository reservationRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryReservation reserveForOrder(Order order, UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> createInitialInventory(product));

        if (item.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Insufficient inventory for product " + productId);
        }

        item.reserve(quantity);
        inventoryRepository.save(item);

        InventoryReservation reservation = InventoryReservation.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .status(InventoryReservationStatus.RESERVED)
                .reservedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getInventory(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        InventoryItem item = inventoryRepository.findByProduct(product)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse adjustInventory(UUID productId, int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> createInitialInventory(product));

        int newTotal = item.getTotalQuantity() + delta;
        if (newTotal < 0) {
            throw new IllegalStateException("Inventory total cannot be negative");
        }

        int newAvailable = item.getAvailableQuantity() + delta;
        if (newAvailable < 0) {
            throw new IllegalStateException("Available inventory cannot be negative");
        }

        item.setTotalQuantity(newTotal);
        item.setAvailableQuantity(newAvailable);
        item.setUpdatedAt(Instant.now());
        item = inventoryRepository.save(item);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public void releaseReservationForOrder(UUID orderId) {
        reservationRepository.findAll().stream()
                .filter(r -> r.getOrder() != null && r.getOrder().getId() != null && r.getOrder().getId().equals(orderId))
                .forEach(r -> {
                    if (r.getStatus() == InventoryReservationStatus.RESERVED) {
                        InventoryItem item = inventoryRepository.findByProductIdForUpdate(r.getProduct().getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + r.getProduct().getId()));
                        item.releaseReservation(r.getQuantity());
                        r.setStatus(InventoryReservationStatus.RELEASED);
                        r.setReleasedAt(Instant.now());
                        inventoryRepository.save(item);
                        reservationRepository.save(r);
                    }
                });
    }

    private InventoryItem createInitialInventory(Product product) {
        InventoryItem item = InventoryItem.builder()
                .product(product)
                .totalQuantity(0)
                .availableQuantity(0)
                .reservedQuantity(0)
                .committedQuantity(0)
                .version(0L)
                .updatedAt(Instant.now())
                .build();
        return inventoryRepository.save(item);
    }
}
