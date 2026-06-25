package com.example.basketservice.product;

import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldGetProduct() throws Exception {

        Product product =
                productRepository.save(
                        new Product(
                                "Laptop",
                                BigDecimal.valueOf(1500),
                                5));

        mockMvc.perform(
                        get("/api/v1/products/{id}",
                                product.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldListProducts() throws Exception {

        productRepository.save(
                new Product(
                        "Phone",
                        BigDecimal.valueOf(1000),
                        10));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }
}