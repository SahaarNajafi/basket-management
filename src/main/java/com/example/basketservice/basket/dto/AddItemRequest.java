package com.example.basketservice.basket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddItemRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @Positive(message = "quantity must be greater than zero")
        @Max(value = 1000, message = "quantity must not exceed 1000")
        int quantity) {
}