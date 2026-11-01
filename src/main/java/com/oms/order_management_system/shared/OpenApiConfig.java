package com.oms.order_management_system.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI omsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management System API")
                        .version("1.0")
                        .description("REST API for catalog, inventory, order, and payment operations across the Order Management System.")
                        .contact(new Contact()
                                .name("OMS Support")
                                .email("support@oms.local")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server")
                ));
    }
}
