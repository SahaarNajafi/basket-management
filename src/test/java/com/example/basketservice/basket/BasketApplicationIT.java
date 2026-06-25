package com.example.basketservice.basket;

import com.example.basketservice.basket.dto.AddItemRequest;
import com.example.basketservice.basket.dto.CreateBasketRequest;
import com.example.basketservice.basket.dto.UpdateItemRequest;
import com.example.basketservice.basket.repository.BasketRepository;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BasketApplicationIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BasketRepository basketRepository;

    private UUID productId;

    @BeforeEach
    void setUp() {
        basketRepository.deleteAll();
        productRepository.deleteAll();
        Product saved = productRepository.save(
                new Product("Test Widget", new BigDecimal("10.00"), 5));
        productId = saved.getId();
    }

    @Test
    void happyPath_create_add_update_checkout() throws Exception {
        UUID basketId = createBasket();

        // Add 2 units -> total 20.00
        mockMvc.perform(post("/api/v1/baskets/{id}/items", basketId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(productId, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalAmount").value(20.00));

        // Update absolute quantity to 3 -> total 30.00
        mockMvc.perform(put("/api/v1/baskets/{id}/items/{productId}", basketId, productId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItemRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(30.00));

        // Checkout
        mockMvc.perform(post("/api/v1/baskets/{id}/checkout", basketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalAmount").value(30.00));

        // Modifying after checkout is rejected
        mockMvc.perform(post("/api/v1/baskets/{id}/items", basketId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(productId, 1))))
                .andExpect(status().isConflict());
    }

    @Test
    void addingMoreThanStock_returns409() throws Exception {
        UUID basketId = createBasket();

        mockMvc.perform(post("/api/v1/baskets/{id}/items", basketId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(productId, 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidQuantity_returns400() throws Exception {
        UUID basketId = createBasket();

        mockMvc.perform(post("/api/v1/baskets/{id}/items", basketId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(productId, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void unknownBasket_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/baskets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_emptiesBasket() throws Exception {
        UUID basketId = createBasket();

        mockMvc.perform(post("/api/v1/baskets/{id}/items", basketId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(productId, 1))))
                .andExpect(status().isOk());

        // DELETE returns 204 No Content with an empty body.
        mockMvc.perform(delete("/api/v1/baskets/{id}/items/{productId}", basketId, productId))
                .andExpect(status().isNoContent());

        // The basket is now empty.
        mockMvc.perform(get("/api/v1/baskets/{id}", basketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0.00));
    }

    @Test
    void create_echoesCustomerId_andStartsActive() throws Exception {
        mockMvc.perform(post("/api/v1/baskets")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBasketRequest("customer-123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private UUID createBasket() throws Exception {
        String body = mockMvc.perform(post("/api/v1/baskets"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}