package com.oms.order_management_system.payment;

import com.oms.order_management_system.IntegrationTestSupport;
import com.oms.order_management_system.catalog.Product;
import com.oms.order_management_system.catalog.ProductRepository;
import com.oms.order_management_system.inventory.InventoryItem;
import com.oms.order_management_system.inventory.InventoryRepository;
import com.oms.order_management_system.order.CreateOrderRequest;
import com.oms.order_management_system.order.OrderRepository;
import com.oms.order_management_system.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        Product product = Product.builder()
                .sku("SKU-PAY-1")
                .name("Payment Test Product")
                .description("Test product")
                .price(new BigDecimal("99.99"))
                .active(true)
                .build();
        Product saved = productRepository.save(product);

        InventoryItem item = InventoryItem.builder()
                .product(saved)
                .totalQuantity(25)
                .availableQuantity(25)
                .reservedQuantity(0)
                .committedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);
    }

    @Test
    void shouldCreateOrderAndCapturePaymentSuccessfully() throws Exception {
        Product saved = productRepository.findBySku("SKU-PAY-1").orElseThrow();

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest("customer-1", saved.getId(), 2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID orderId = UUID.fromString(objectMapper.readTree(orderJson).get("orderId").asText());

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest("idem-success-1", new BigDecimal("199.98"), PaymentProviderMode.SUCCESS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        var order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findByIdempotencyKey("idem-success-1")).isPresent();
    }

    @Test
    void shouldRejectDuplicatePaymentOperationWithSameIdempotencyKey() throws Exception {
        Product saved = productRepository.findBySku("SKU-PAY-1").orElseThrow();

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest("customer-2", saved.getId(), 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID orderId = UUID.fromString(objectMapper.readTree(orderJson).get("orderId").asText());

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest("idem-repeat", new BigDecimal("99.99"), PaymentProviderMode.SUCCESS))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest("idem-repeat", new BigDecimal("99.99"), PaymentProviderMode.SUCCESS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Returned existing payment operation for same idempotency key"));
    }

    @Test
    void shouldMoveOrderToVerificationRequiredWhenProviderTimesOut() throws Exception {
        Product saved = productRepository.findBySku("SKU-PAY-1").orElseThrow();

        String orderJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest("customer-3", saved.getId(), 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID orderId = UUID.fromString(objectMapper.readTree(orderJson).get("orderId").asText());

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest("idem-timeout", new BigDecimal("99.99"), PaymentProviderMode.TIMEOUT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UNRESOLVED"));

        var order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_VERIFICATION_REQUIRED);
    }
}
