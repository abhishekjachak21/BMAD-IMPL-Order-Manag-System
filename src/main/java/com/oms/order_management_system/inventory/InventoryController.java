package com.oms.order_management_system.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventory(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PostMapping("/products/{productId}/adjust")
    public ResponseEntity<InventoryItemResponse> adjustInventory(@PathVariable UUID productId,
                                                               @Valid @RequestBody AdjustInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.adjustInventory(productId, request.delta()));
    }
}
