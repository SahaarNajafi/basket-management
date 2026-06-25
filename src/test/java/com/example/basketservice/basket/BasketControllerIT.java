package com.example.basketservice.basket;

import com.example.basketservice.basket.dto.AddItemRequest;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BasketControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldCreateBasket() throws Exception {

        String response =
                mockMvc.perform(post("/api/v1/baskets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "customerId":"customer-123"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        assertThat(json.get("id")).isNotNull();
        assertThat(json.get("customerId").asText())
                .isEqualTo("customer-123");
        assertThat(json.get("status").asText())
                .isEqualTo("ACTIVE");
    }

    @Test
    void shouldAddItemToBasket() throws Exception {

        Product product = productRepository.save(
                new Product(
                        "MacBook",
                        BigDecimal.valueOf(1000),
                        10));

        String basketResponse =
                mockMvc.perform(post("/api/v1/baskets"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID basketId =
                UUID.fromString(
                        objectMapper.readTree(basketResponse)
                                .get("id")
                                .asText());

        AddItemRequest request =
                new AddItemRequest(product.getId(), 2);

        String response =
                mockMvc.perform(post("/api/v1/baskets/{basketId}/items", basketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        assertThat(json.get("items").size()).isEqualTo(1);
        assertThat(json.get("totalAmount").asDouble())
                .isEqualTo(2000.0);
    }

    @Test
    void shouldUpdateBasketItemQuantity() throws Exception {

        Product product = productRepository.save(
                new Product(
                        "Keyboard",
                        BigDecimal.valueOf(100),
                        20));

        String basketResponse =
                mockMvc.perform(post("/api/v1/baskets"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID basketId =
                UUID.fromString(
                        objectMapper.readTree(basketResponse)
                                .get("id")
                                .asText());

        mockMvc.perform(post("/api/v1/baskets/{basketId}/items", basketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId":"%s",
                                    "quantity":1
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk());

        String response =
                mockMvc.perform(
                                put("/api/v1/baskets/{basketId}/items/{productId}",
                                        basketId,
                                        product.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                    "quantity":5
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        assertThat(
                json.get("items")
                        .get(0)
                        .get("quantity")
                        .asInt())
                .isEqualTo(5);
    }

    @Test
    void shouldRemoveBasketItem() throws Exception {

        Product product = productRepository.save(
                new Product(
                        "Mouse",
                        BigDecimal.valueOf(50),
                        10));

        String basketResponse =
                mockMvc.perform(post("/api/v1/baskets"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID basketId =
                UUID.fromString(
                        objectMapper.readTree(basketResponse)
                                .get("id")
                                .asText());

        mockMvc.perform(post("/api/v1/baskets/{basketId}/items", basketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId":"%s",
                                    "quantity":1
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(delete(
                        "/api/v1/baskets/{basketId}/items/{productId}",
                        basketId,
                        product.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCheckoutBasket() throws Exception {

        Product product = productRepository.save(
                new Product(
                        "Monitor",
                        BigDecimal.valueOf(500),
                        5));

        String basketResponse =
                mockMvc.perform(post("/api/v1/baskets"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID basketId =
                UUID.fromString(
                        objectMapper.readTree(basketResponse)
                                .get("id")
                                .asText());

        mockMvc.perform(post("/api/v1/baskets/{basketId}/items", basketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId":"%s",
                                    "quantity":2
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk());

        String response =
                mockMvc.perform(post(
                                "/api/v1/baskets/{basketId}/checkout",
                                basketId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        assertThat(json.get("status").asText())
                .isEqualTo("CHECKED_OUT");

        assertThat(json.get("totalAmount").asDouble())
                .isEqualTo(1000.0);
    }
}