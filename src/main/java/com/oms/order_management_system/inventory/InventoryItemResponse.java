package com.oms.order_management_system.inventory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "InventoryItemResponse", description = "Current inventory snapshot for a product and its reservation state.")
public record InventoryItemResponse(
        @Schema(description = "Product identifier linked to this inventory record.", example = "0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d")
        UUID productId,

        @Schema(description = "Total inventory units tracked for the product.", example = "250")
        int totalQuantity,

        @Schema(description = "Units currently available to sell.", example = "180")
        int availableQuantity,

        @Schema(description = "Units reserved on active orders but not yet committed.", example = "40")
        int reservedQuantity,

        @Schema(description = "Units committed to fulfilled or processing orders.", example = "30")
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
