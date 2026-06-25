package com.example.basketservice.basket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * Sets the absolute quantity for a line. To remove a line, use the DELETE
 * endpoint rather than sending quantity 0.
 */
public record UpdateItemRequest(
        @Positive(message = "quantity must be greater than zero")
        @Max(value = 1000, message = "quantity must not exceed 1000")
        int quantity) {
}