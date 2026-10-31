package com.oms.order_management_system.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.order_management_system.IntegrationTestSupport;
import com.oms.order_management_system.catalog.CreateProductRequest;
import com.oms.order_management_system.catalog.ProductRepository;
import com.oms.order_management_system.inventory.InventoryItem;
import com.oms.order_management_system.inventory.InventoryRepository;
import com.oms.order_management_system.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCreationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        Product product = Product.builder()
                .sku("SKU-200")
                .name("Monitor")
                .description("24 inch monitor")
                .price(new BigDecimal("199.00"))
                .active(true)
                .build();
        Product saved = productRepository.save(product);

        InventoryItem item = InventoryItem.builder()
                .product(saved)
                .totalQuantity(10)
                .availableQuantity(10)
                .reservedQuantity(0)
                .committedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);
    }

    @Test
    void createOrder_shouldReserveInventoryAndPersistOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("customer-1", productRepository.findBySku("SKU-200").orElseThrow().getId(), 2);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        InventoryItem updated = inventoryRepository.findByProduct(productRepository.findBySku("SKU-200").orElseThrow()).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualTo(8);
        assertThat(updated.getReservedQuantity()).isEqualTo(2);
    }
}
