package com.oms.order_management_system.inventory;

import com.oms.order_management_system.catalog.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByProduct(Product product);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.product.id = :productId")
    Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") UUID productId);
}
