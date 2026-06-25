package com.example.basketservice.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI basketServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Basket Service API")
                .version("v1")
                .description("REST APIs for browsing products and managing a shopping basket."));
    }
}
