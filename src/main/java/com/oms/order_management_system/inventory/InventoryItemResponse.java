package com.oms.order_management_system.inventory;

import java.util.UUID;

public record InventoryItemResponse(UUID productId,
                                   int totalQuantity,
                                   int availableQuantity,
                                   int reservedQuantity,
                                   int committedQuantity) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getProduct().getId(),
                item.getTotalQuantity(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getCommittedQuantity()
        );
    }
}
