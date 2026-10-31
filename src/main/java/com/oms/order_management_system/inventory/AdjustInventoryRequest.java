package com.oms.order_management_system.inventory;

import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(@NotNull Integer delta) {
}
