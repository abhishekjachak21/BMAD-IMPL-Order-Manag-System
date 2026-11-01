package com.oms.order_management_system.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory visibility and stock adjustments for products")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(
            summary = "Get product inventory",
            description = "Returns the current total, available, reserved, and committed inventory values for a specific product.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Inventory found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = InventoryItemResponse.class),
                                    examples = @ExampleObject(name = "Inventory payload", value = "{\n  \"productId\": \"0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d\",\n  \"totalQuantity\": 250,\n  \"availableQuantity\": 180,\n  \"reservedQuantity\": 40,\n  \"committedQuantity\": 30\n}"))),
                    @ApiResponse(responseCode = "404", description = "Product inventory does not exist", content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/products/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventory(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @Operation(
            summary = "Adjust inventory",
            description = "Applies a positive or negative stock delta to a product's inventory and returns the updated snapshot.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Inventory adjusted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = InventoryItemResponse.class),
                                    examples = @ExampleObject(name = "Adjusted inventory", value = "{\n  \"productId\": \"0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d\",\n  \"totalQuantity\": 250,\n  \"availableQuantity\": 175,\n  \"reservedQuantity\": 40,\n  \"committedQuantity\": 35\n}"))),
                    @ApiResponse(responseCode = "400", description = "Inventory adjustment is invalid", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Inventory record not found", content = @Content(mediaType = "application/json"))
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = AdjustInventoryRequest.class),
            examples = @ExampleObject(name = "Adjust inventory payload", value = "{\n  \"delta\": -5\n}")))
    @PostMapping("/products/{productId}/adjust")
    public ResponseEntity<InventoryItemResponse> adjustInventory(@PathVariable UUID productId,
                                                               @Valid @RequestBody AdjustInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.adjustInventory(productId, request.delta()));
    }
}
