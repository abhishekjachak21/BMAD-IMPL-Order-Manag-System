package com.oms.order_management_system.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AdjustInventoryRequest", description = "Inventory delta used to add or remove available stock for a product.")
public record AdjustInventoryRequest(
        @Schema(description = "Positive number to increase inventory or negative number to decrease it.", example = "-5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer delta) {
}
