package com.oms.order_management_system.catalog;

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
@RequestMapping("/api/products")
@Tag(name = "Catalog", description = "Catalog management for products and pricing")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create a product",
            description = "Creates a new catalog product with its SKU, price, and optional description. Returns the newly persisted product record.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Product created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductResponse.class),
                                    examples = @ExampleObject(name = "Created product", value = "{\n  \"id\": \"0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d\",\n  \"sku\": \"SKU-1001\",\n  \"name\": \"Wireless Mouse\",\n  \"description\": \"Ergonomic wireless mouse with a long battery life.\",\n  \"price\": 49.99,\n  \"active\": true\n}"))),
                    @ApiResponse(responseCode = "400", description = "Invalid product payload", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "A conflicting product record already exists", content = @Content(mediaType = "application/json"))
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = CreateProductRequest.class),
            examples = @ExampleObject(name = "Create product payload", value = "{\n  \"sku\": \"SKU-1001\",\n  \"name\": \"Wireless Mouse\",\n  \"description\": \"Ergonomic wireless mouse with a long battery life.\",\n  \"price\": 49.99\n}")))
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProduct(request));
    }

    @Operation(
            summary = "Retrieve a product",
            description = "Fetches a single catalog product by its identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product found and returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductResponse.class),
                                    examples = @ExampleObject(name = "Product response", value = "{\n  \"id\": \"0c76b1d9-7e48-4fd3-9f3d-3f0d4e8f780d\",\n  \"sku\": \"SKU-1001\",\n  \"name\": \"Wireless Mouse\",\n  \"description\": \"Ergonomic wireless mouse with a long battery life.\",\n  \"price\": 49.99,\n  \"active\": true\n}"))),
                    @ApiResponse(responseCode = "404", description = "Product does not exist", content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getProduct(id));
    }
}
