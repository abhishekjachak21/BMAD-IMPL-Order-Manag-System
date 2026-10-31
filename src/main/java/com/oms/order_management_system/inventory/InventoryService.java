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
